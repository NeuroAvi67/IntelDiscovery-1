package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AssistantMessage
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryRecord
import com.example.data.SearchHistoryItem
import com.example.data.UiState
import com.example.ui.components.DiscoverySkeletonCard
import com.example.ui.components.IntelCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SavedDiscoveryCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.SuggestionViolet
import com.example.ui.theme.VerifiedEmerald

private val TopicExplorationPrompts = listOf(
    "Quantum Error Correction" to DiscoveryCategory.SCIENCE,
    "Autonomous Supply Chains" to DiscoveryCategory.ECONOMICS,
    "Zero-Trust Architecture" to DiscoveryCategory.SECURITY,
    "Spatial UI Design Systems" to DiscoveryCategory.DESIGN,
    "Local LLM Quantization" to DiscoveryCategory.TECHNOLOGY
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    userDisplayName: String?,
    discoveriesState: UiState<List<DiscoveryRecord>>,
    searchesState: UiState<List<SearchHistoryItem>>,
    messagesState: UiState<List<AssistantMessage>>,
    onInvestigateTopic: (String, DiscoveryCategory) -> Unit,
    onOpenDiscoveriesTab: () -> Unit,
    onOpenRecord: (DiscoveryRecord) -> Unit,
    onTogglePin: (DiscoveryRecord) -> Unit,
    onDeleteRecord: (DiscoveryRecord) -> Unit
) {
    var pinnedOnly by remember { mutableStateOf(false) }
    var categoryFilter by remember { mutableStateOf(DiscoveryCategory.ALL) }

    val discoveries = (discoveriesState as? UiState.Success)?.data.orEmpty()
    val searches = (searchesState as? UiState.Success)?.data.orEmpty()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    val filteredDiscoveries = remember(discoveries, pinnedOnly, categoryFilter) {
        discoveries.filter { record ->
            val matchesPin = !pinnedOnly || record.pinned
            val matchesCategory = categoryFilter == DiscoveryCategory.ALL ||
                record.category.equals(categoryFilter.label, ignoreCase = true)
            matchesPin && matchesCategory
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
                        text = "INTELLIGENCE WORKSPACE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (!userDisplayName.isNullOrBlank()) {
                            "${userDisplayName.substringBefore(" ")}'s Dashboard"
                        } else {
                            "Intelligence Dashboard"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Live telemetry of your saved intelligence briefs, pinned insights, and research trails.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Metrics grid
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "SAVED BRIEFS",
                        value = discoveries.size.toString(),
                        icon = Icons.Filled.Bookmark,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "PINNED",
                        value = discoveries.count { it.pinned }.toString(),
                        icon = Icons.Filled.PushPin,
                        tint = VerifiedEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "SEARCHES",
                        value = searches.size.toString(),
                        icon = Icons.Filled.Search,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "AI TURNS",
                        value = messages.size.toString(),
                        icon = Icons.Filled.AutoAwesome,
                        tint = SuggestionViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Topic Exploration
            item {
                IntelCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Topic Exploration",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Launch a structured intelligence investigation with one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TopicExplorationPrompts.forEach { (topic, category) ->
                            AssistChip(
                                onClick = { onInvestigateTopic(topic, category) },
                                label = { Text(topic) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Explore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Saved Discoveries header & filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saved Discoveries",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        FilterChip(
                            selected = pinnedOnly,
                            onClick = { pinnedOnly = !pinnedOnly },
                            label = { Text("Pinned") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("dashboard_pinned_filter")
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DiscoveryCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = categoryFilter == cat,
                                onClick = { categoryFilter = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }

            when (discoveriesState) {
                is UiState.Loading -> {
                    item { DiscoverySkeletonCard() }
                }
                is UiState.Error -> {
                    item {
                        IntelCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Unable to sync saved discoveries",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = discoveriesState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredDiscoveries.isEmpty()) {
                        item {
                            IntelCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (discoveries.isEmpty()) {
                                        "Your saved library is empty"
                                    } else {
                                        "No discoveries match these filters"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (discoveries.isEmpty()) {
                                        "Run an investigation in the Discovery Engine and save structured briefs here."
                                    } else {
                                        "Clear your category or pinned filter to view all ${discoveries.size} saved briefs."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (discoveries.isEmpty()) {
                                            onOpenDiscoveriesTab()
                                        } else {
                                            pinnedOnly = false
                                            categoryFilter = DiscoveryCategory.ALL
                                        }
                                    },
                                    modifier = Modifier.testTag("dashboard_empty_action")
                                ) {
                                    Text(if (discoveries.isEmpty()) "Open Discovery Engine" else "Reset Filters")
                                }
                            }
                        }
                    } else {
                        items(filteredDiscoveries, key = { it.id }) { record ->
                            SavedDiscoveryCard(
                                record = record,
                                onOpen = { onOpenRecord(record) },
                                onTogglePin = { onTogglePin(record) },
                                onDelete = { onDeleteRecord(record) }
                            )
                        }
                    }
                }
            }

            // Recent Searches
            item {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (searches.isEmpty()) {
                item {
                    IntelCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No search history recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(searches.take(8), key = { it.id }) { item ->
                    IntelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onInvestigateTopic(
                                    item.query,
                                    DiscoveryCategory.fromRaw(item.category)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = item.query,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.category} • ${item.resultCount} insights",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatTimestamp(item.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
