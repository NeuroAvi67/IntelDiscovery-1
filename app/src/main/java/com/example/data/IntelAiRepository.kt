package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class IntelAiRepository {
    private val discoverySchema = Schema.obj(
        properties = mapOf(
            "category" to Schema.enumeration(
                values = listOf("Technology", "Science", "Economics", "Security", "Design", "General"),
                description = "Primary domain classification of the topic"
            ),
            "summary" to Schema.string(
                description = "Clear executive synthesis of the topic"
            ),
            "verifiedFacts" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 well-established, consensus factual statements"
            ),
            "interpretations" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 reasoned analytical interpretations, trade-offs, or implications"
            ),
            "aiSuggestions" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 actionable next investigation angles or hypotheses"
            ),
            "sourceNotes" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 transparent attribution notes stating knowledge domains and what requires live external verification"
            )
        )
    )

    private val discoveryConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = discoverySchema
    }

    private val flashDiscoveryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelOption.GEMINI_3_8_FLASH.modelId,
            generationConfig = discoveryConfig,
            systemInstruction = content {
                text(
                    "You are IntelDiscovery, a rigorous intelligence and research synthesis engine. " +
                        "Never fabricate live web URLs, citations, or real-time telemetry. " +
                        "Strictly separate established consensus facts (verifiedFacts), reasoned analysis (interpretations), " +
                        "and exploratory research suggestions (aiSuggestions). " +
                        "In sourceNotes, clearly state the relevant knowledge domains and note where live external verification is advised."
                )
            }
        )
    }

    private val liteDiscoveryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelOption.GEMINI_3_5_FLASH_LITE.modelId,
            generationConfig = discoveryConfig,
            systemInstruction = content {
                text(
                    "You are IntelDiscovery, a fast intelligence synthesis engine. " +
                        "Never fabricate live web URLs or pretend to access real-time feeds. " +
                        "Separate verifiedFacts, interpretations, aiSuggestions, and transparent sourceNotes."
                )
            }
        )
    }

    private val flashChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelOption.GEMINI_3_8_FLASH.modelId,
            systemInstruction = content {
                text(
                    "You are the IntelDiscovery AI Assistant. Deliver clear, structured, analytical intelligence. " +
                        "Explicitly distinguish established facts from hypotheses or suggestions, and never invent live URLs or unverified real-time data."
                )
            }
        )
    }

    private val liteChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelOption.GEMINI_3_5_FLASH_LITE.modelId,
            systemInstruction = content {
                text(
                    "You are the IntelDiscovery AI Assistant. Give crisp, accurate, well-structured analysis " +
                        "while separating verified knowledge from suggestions."
                )
            }
        )
    }

    suspend fun investigateTopic(
        query: String,
        categoryHint: DiscoveryCategory,
        modelOption: AiModelOption,
        depth: ResponseDepth
    ): Result<DiscoveryDraft> {
        return try {
            val model = when (modelOption) {
                AiModelOption.GEMINI_3_8_FLASH -> flashDiscoveryModel
                AiModelOption.GEMINI_3_5_FLASH_LITE -> liteDiscoveryModel
            }

            val categoryClause = if (categoryHint == DiscoveryCategory.ALL) {
                "Select the most fitting category."
            } else {
                "Focus through the lens of ${categoryHint.label}."
            }

            val prompt = buildString {
                append("Investigate the topic: \"")
                append(query.trim())
                append("\". ")
                append(categoryClause)
                append(" ")
                append(depth.instructionHint)
            }

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }

            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("AI model returned an empty intelligence brief."))

            val json = JSONObject(rawJson)
            val resolvedCategory = DiscoveryCategory.fromRaw(json.optString("category", categoryHint.label))
            val summary = json.optString("summary").trim()
                .ifEmpty { "Structured brief generated for \"$query\"." }

            val facts = json.optStringList("verifiedFacts")
            val interpretations = json.optStringList("interpretations")
            val suggestions = json.optStringList("aiSuggestions")
            val sources = json.optStringList("sourceNotes").ifEmpty {
                listOf("Synthesized from Gemini AI knowledge base; verify time-sensitive figures against primary publications.")
            }

            Result.success(
                DiscoveryDraft(
                    query = query.trim(),
                    category = if (resolvedCategory == DiscoveryCategory.ALL) DiscoveryCategory.GENERAL else resolvedCategory,
                    summary = summary,
                    verifiedFacts = facts,
                    interpretations = interpretations,
                    aiSuggestions = suggestions,
                    sourceNotes = sources,
                    modelUsed = modelOption.modelId
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Topic investigation timed out", e)
            Result.failure(IllegalStateException("Intelligence synthesis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Topic investigation failed", e)
            Result.failure(e)
        }
    }

    fun streamAssistantReply(
        prompt: String,
        history: List<AssistantMessage>,
        modelOption: AiModelOption,
        depth: ResponseDepth
    ): Flow<String> = flow {
        val model = when (modelOption) {
            AiModelOption.GEMINI_3_8_FLASH -> flashChatModel
            AiModelOption.GEMINI_3_5_FLASH_LITE -> liteChatModel
        }

        val chatHistory: List<Content> = history
            .takeLast(12)
            .map { msg ->
                val role = if (msg.role == "assistant") "model" else "user"
                content(role) { text(msg.content) }
            }

        val enrichedPrompt = "${depth.instructionHint}\n\nUser request: ${prompt.trim()}"
        val chat = model.startChat(history = chatHistory)
        val buffer = StringBuilder()

        withTimeout(60_000L) {
            chat.sendMessageStream(enrichedPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    buffer.append(text)
                    emit(buffer.toString())
                }
            }
        }

        if (buffer.isBlank()) {
            throw IllegalStateException("AI Assistant returned an empty response.")
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotEmpty()) add(item)
            }
        }
    }

    private companion object {
        const val TAG = "IntelAiRepository"
    }
}
