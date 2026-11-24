package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DiscoveryCategory(val label: String) {
    ALL("All"),
    TECHNOLOGY("Technology"),
    SCIENCE("Science"),
    ECONOMICS("Economics"),
    SECURITY("Security"),
    DESIGN("Design"),
    GENERAL("General");

    companion object {
        val selectableWriteCategories: List<DiscoveryCategory> = listOf(
            TECHNOLOGY,
            SCIENCE,
            ECONOMICS,
            SECURITY,
            DESIGN,
            GENERAL
        )

        fun fromRaw(raw: String?): DiscoveryCategory {
            return entries.firstOrNull {
                it.label.equals(raw?.trim(), ignoreCase = true) ||
                    it.name.equals(raw?.trim(), ignoreCase = true)
            } ?: GENERAL
        }
    }
}

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    PINNED_FIRST("Pinned First"),
    ALPHABETICAL("A–Z")
}

enum class ThemePreference(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class AiModelOption(val modelId: String, val displayName: String, val description: String) {
    GEMINI_3_8_FLASH(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Balanced reasoning & structured discovery"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "Ultra-low latency rapid responses"
    );

    companion object {
        fun fromRaw(raw: String?): AiModelOption =
            entries.firstOrNull { it.name == raw || it.modelId == raw } ?: GEMINI_3_8_FLASH
    }
}

enum class ResponseDepth(val label: String, val instructionHint: String) {
    CONCISE("Concise", "Keep sections crisp, executive-ready, and high-signal."),
    COMPREHENSIVE("Comprehensive", "Provide thorough, nuanced technical and strategic context.");

    companion object {
        fun fromRaw(raw: String?): ResponseDepth =
            entries.firstOrNull { it.name == raw } ?: CONCISE
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class DiscoveryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val query: String = "",
    val category: String = "General",
    val summary: String = "",
    val verifiedFacts: List<String> = emptyList(),
    val interpretations: List<String> = emptyList(),
    val aiSuggestions: List<String> = emptyList(),
    val sourceNotes: List<String> = emptyList(),
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SearchHistoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val query: String = "",
    val category: String = "All",
    val resultCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AssistantMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val modelName: String = "gemini-3.8-flash",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DiscoveryDraft(
    val query: String,
    val category: DiscoveryCategory,
    val summary: String,
    val verifiedFacts: List<String>,
    val interpretations: List<String>,
    val aiSuggestions: List<String>,
    val sourceNotes: List<String>,
    val modelUsed: String,
    val savedRecordId: String? = null
) {
    val totalInsights: Int
        get() = verifiedFacts.size + interpretations.size + aiSuggestions.size
}
