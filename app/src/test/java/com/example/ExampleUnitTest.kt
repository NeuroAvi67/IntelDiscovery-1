package com.example

import com.example.data.AiModelOption
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryDraft
import com.example.data.ResponseDepth
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun discoveryCategory_parsesCaseInsensitivelyWithSafeFallback() {
        assertEquals(DiscoveryCategory.SECURITY, DiscoveryCategory.fromRaw("security"))
        assertEquals(DiscoveryCategory.TECHNOLOGY, DiscoveryCategory.fromRaw("Technology"))
        assertEquals(DiscoveryCategory.GENERAL, DiscoveryCategory.fromRaw("unknown_domain"))
    }

    @Test
    fun aiModelAndDepth_fallbackSafely() {
        assertEquals(AiModelOption.GEMINI_3_8_FLASH, AiModelOption.fromRaw("gemini-3.8-flash"))
        assertEquals(AiModelOption.GEMINI_3_5_FLASH_LITE, AiModelOption.fromRaw("gemini-3.5-flash-lite"))
        assertEquals(AiModelOption.GEMINI_3_8_FLASH, AiModelOption.fromRaw("deprecated-model"))
        assertEquals(ResponseDepth.CONCISE, ResponseDepth.fromRaw("INVALID"))
    }

    @Test
    fun discoveryDraft_computesTotalInsights() {
        val draft = DiscoveryDraft(
            query = "Quantum Error Correction",
            category = DiscoveryCategory.SCIENCE,
            summary = "Surface codes reduce logical error rates.",
            verifiedFacts = listOf("Fact 1", "Fact 2"),
            interpretations = listOf("Interp 1"),
            aiSuggestions = listOf("Sug 1", "Sug 2", "Sug 3"),
            sourceNotes = listOf("Note 1"),
            modelUsed = "gemini-3.8-flash"
        )
        assertEquals(6, draft.totalInsights)
    }
}
