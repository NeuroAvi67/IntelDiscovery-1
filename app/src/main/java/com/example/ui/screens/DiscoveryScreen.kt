package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryDraft
import com.example.data.DiscoveryRecord
import com.example.data.SearchHistoryItem
import com.example.data.SortOrder
import com.example.data.ThemePreference
import com.example.data.UiState
import com.example.ui.components.DiscoverySkeletonCard
import com.example.ui.components.EpistemicSectionCard
import com.example.ui.components.EpistemicTone
import com.example.ui.components.IntelCard
import com.example.ui.components.SavedDiscoveryCard
import com.example.ui.components.SearchInputBar
import com.example.ui.viewmodel.DiscoveryInvestigationState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryScreen(
    searchQuery: String,
    selectedCategory: DiscoveryCategory,
    sortOrder: SortOrder,
    investigationState: DiscoveryInvestigationState,
    isSaving: Boolean,
    discoveriesState: UiState<List<DiscoveryRecord>>,
    searchesState: UiState<List<SearchHistoryItem>>,
    themePreference: ThemePreference,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (DiscoveryCategory) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onInvestigate: (String, DiscoveryCategory?) -> Unit,
    onSaveDraft: (Boolean) -> Unit,
    onAskAssistant: (DiscoveryDraft) -> Unit,
    onOpenRecord: (DiscoveryRecord) -> Unit,
    onTogglePin: (DiscoveryRecord) -> Unit,
    onDeleteRecord: (DiscoveryRecord) -> Unit,
    onShowToast: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showSortMenu by remember { mutableStateOf(false) }

    val searches = (searchesState as? UiState.Success)?.data.orEmpty()
    val savedList = (discoveriesState as? UiState.Success)?.data.orEmpty()

    val sortedSaved = remember(savedList, selectedCategory, sortOrder) {
        val filtered = if (selectedCategory == DiscoveryCategory.ALL) {
            savedList
        } else {
            savedList.filter { it.category.equals(selectedCategory.label, ignoreCase = true) }
        }
        when (sortOrder) {
            SortOrder.NEWEST -> filtered
            SortOrder.PINNED_FIRST -> filtered.sortedByDescending { it.pinned }
            SortOrder.ALPHABETICAL -> filtered.sortedBy { it.query.lowercase() }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "DISCOVERY ENGINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Structured Topic Investigation",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Search bar + category filters + sort
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchInputBar(
                        query = searchQuery,
                        onQueryChange = onQueryChange,
                        onSubmit = { onInvestigate(searchQuery, null) },
                        placeholder = "Search a concept, technology, or question...",
                        testTag = "discovery_search_input",
                        submitTestTag = "discovery_search_submit",
                        enabled = investigationState !is DiscoveryInvestigationState.Investigating
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Domain Filter",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(sortOrder.label, style = MaterialTheme.typography.labelSmall)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            onSortSelected(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DiscoveryCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { onCategorySelected(category) },
                                label = { Text(category.label) }
                            )
                        }
                    }
                }
            }

            // Search History Chips
            if (searches.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Recent Queries",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            searches.take(6).forEach { historyItem ->
                                AssistChip(
                                    onClick = {
                                        onInvestigate(
                                            historyItem.query,
                                            DiscoveryCategory.fromRaw(historyItem.category)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = historyItem.query,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Investigation Results / Loading / Error
            when (investigationState) {
                is DiscoveryInvestigationState.Idle -> {
                    item {
                        IntelCard(accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Ready to synthesize intelligence",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter any topic above to generate a structured brief with separate Verified Facts, Analytical Interpretations, AI Suggestions, and transparent attribution notes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is DiscoveryInvestigationState.Investigating -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Synthesizing intelligence for \"${investigationState.query}\"...",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            DiscoverySkeletonCard()
                            DiscoverySkeletonCard()
                        }
                    }
                }

                is DiscoveryInvestigationState.Error -> {
                    item {
                        IntelCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Investigation Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = investigationState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onInvestigate(investigationState.query, null) },
                                modifier = Modifier.testTag("discovery_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Investigation")
                            }
                        }
                    }
                }

                is DiscoveryInvestigationState.Ready -> {
                    val draft = investigationState.draft
                    item {
                        IntelCard(accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = draft.category.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = draft.modelUsed,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val formatted = buildBriefClipboardText(draft)
                                        clipboard.setText(AnnotatedString(formatted))
                                        onShowToast("Discovery brief copied to clipboard")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy discovery brief"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = draft.query,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = draft.summary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val alreadySaved = draft.savedRecordId != null
                                Button(
                                    onClick = { onSaveDraft(false) },
                                    enabled = !alreadySaved && !isSaving,
                                    modifier = Modifier.testTag("save_discovery_button")
                                ) {
                                    Icon(
                                        imageVector = if (alreadySaved) Icons.Filled.CheckCircle else Icons.Filled.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (alreadySaved) "Saved" else if (isSaving) "Saving..." else "Save Brief")
                                }

                                if (!alreadySaved) {
                                    FilledTonalButton(
                                        onClick = { onSaveDraft(true) },
                                        enabled = !isSaving,
                                        modifier = Modifier.testTag("save_pin_discovery_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PushPin,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Pin")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onAskAssistant(draft) },
                                    modifier = Modifier.testTag("ask_assistant_from_draft_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Discuss in Assistant")
                                }
                            }
                        }
                    }

                    item {
                        EpistemicSectionCard(
                            title = "Verified Facts",
                            badgeLabel = "ESTABLISHED",
                            items = draft.verifiedFacts,
                            tone = EpistemicTone.VERIFIED_FACT,
                            themePreference = themePreference
                        )
                    }

                    item {
                        EpistemicSectionCard(
                            title = "Analytical Interpretations",
                            badgeLabel = "ANALYSIS",
                            items = draft.interpretations,
                            tone = EpistemicTone.INTERPRETATION,
                            themePreference = themePreference
                        )
                    }

                    item {
                        EpistemicSectionCard(
                            title = "AI-Generated Suggestions",
                            badgeLabel = "EXPLORE",
                            items = draft.aiSuggestions,
                            tone = EpistemicTone.AI_SUGGESTION,
                            themePreference = themePreference
                        )
                    }

                    item {
                        EpistemicSectionCard(
                            title = "Knowledge Basis & Caveats",
                            badgeLabel = "ATTRIBUTION",
                            items = draft.sourceNotes,
                            tone = EpistemicTone.SOURCE_NOTE,
                            themePreference = themePreference
                        )
                    }
                }
            }

            // Saved Library Below
            if (sortedSaved.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Saved Library (${sortedSaved.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(sortedSaved, key = { it.id }) { record ->
                    SavedDiscoveryCard(
                        record = record,
                        onOpen = { onOpenRecord(record) },
                        onTogglePin = { onTogglePin(record) },
                        onDelete = { onDeleteRecord(record) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

private fun buildBriefClipboardText(draft: DiscoveryDraft): String = buildString {
    appendLine("INTELDISCOVERY BRIEF: ${draft.query}")
    appendLine("Category: ${draft.category.label} | Model: ${draft.modelUsed}")
    appendLine()
    appendLine("SUMMARY:")
    appendLine(draft.summary)
    appendLine()
    if (draft.verifiedFacts.isNotEmpty()) {
        appendLine("VERIFIED FACTS:")
        draft.verifiedFacts.forEach { appendLine("- $it") }
        appendLine()
    }
    if (draft.interpretations.isNotEmpty()) {
        appendLine("ANALYTICAL INTERPRETATIONS:")
        draft.interpretations.forEach { appendLine("- $it") }
        appendLine()
    }
    if (draft.aiSuggestions.isNotEmpty()) {
        appendLine("AI-GENERATED SUGGESTIONS:")
        draft.aiSuggestions.forEach { appendLine("- $it") }
        appendLine()
    }
    if (draft.sourceNotes.isNotEmpty()) {
        appendLine("KNOWLEDGE BASIS & CAVEATS:")
        draft.sourceNotes.forEach { appendLine("- $it") }
    }
}
