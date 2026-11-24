package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryRecord
import com.example.data.SearchHistoryItem
import com.example.data.UiState
import com.example.ui.components.IntelCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.VerifiedEmerald

private data class QuickToolSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val query: String?,
    val category: DiscoveryCategory
)

private val QuickTools = listOf(
    QuickToolSpec(
        title = "AI Hardware Radar",
        subtitle = "Next-gen silicon & edge inference",
        icon = Icons.Filled.Memory,
        query = "Emerging edge AI inference hardware architectures",
        category = DiscoveryCategory.TECHNOLOGY
    ),
    QuickToolSpec(
        title = "Fact Audit",
        subtitle = "Commercial battery milestones",
        icon = Icons.Filled.Verified,
        query = "Solid-state battery commercialization status",
        category = DiscoveryCategory.SCIENCE
    ),
    QuickToolSpec(
        title = "Security Brief",
        subtitle = "Post-quantum readiness",
        icon = Icons.Filled.Security,
        query = "Post-quantum cryptography migration roadmap",
        category = DiscoveryCategory.SECURITY
    ),
    QuickToolSpec(
        title = "AI Assistant",
        subtitle = "Open streaming analyst",
        icon = Icons.Filled.AutoAwesome,
        query = null,
        category = DiscoveryCategory.GENERAL
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    userDisplayName: String?,
    discoveriesState: UiState<List<DiscoveryRecord>>,
    searchesState: UiState<List<SearchHistoryItem>>,
    onInvestigate: (String, DiscoveryCategory) -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenDiscoveryRecord: (DiscoveryRecord) -> Unit
) {
    var heroQuery by remember { mutableStateOf("") }
    val discoveries = (discoveriesState as? UiState.Success)?.data.orEmpty()
    val searches = (searchesState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Banner Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_intel_1795509376947),
                            contentDescription = "IntelDiscovery network atmosphere",
                            contentScale = ContentScale.Crop,
                            alpha = 0.55f,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.intel_discovery_icon_1795509377870),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = "INTELDISCOVERY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = if (!userDisplayName.isNullOrBlank()) {
                                    "Welcome back, ${userDisplayName.substringBefore(" ")}"
                                } else {
                                    "Intelligence & Discovery Platform"
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Investigate topics with clear epistemic separation between verified facts, analytical interpretations, and AI next steps.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            SearchInputBar(
                                query = heroQuery,
                                onQueryChange = { heroQuery = it },
                                onSubmit = {
                                    if (heroQuery.trim().length >= 2) {
                                        val q = heroQuery.trim()
                                        heroQuery = ""
                                        onInvestigate(q, DiscoveryCategory.ALL)
                                    }
                                },
                                placeholder = "Investigate any topic, system, or trend...",
                                testTag = "home_search_input",
                                submitTestTag = "home_search_submit"
                            )
                        }
                    }
                }
            }

            // Quick-Access Tools
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick-Access Tools",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickTools.forEach { tool ->
                            IntelCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_tool_${tool.title.lowercase().replace(" ", "_")}"),
                                onClick = {
                                    if (tool.query != null) {
                                        onInvestigate(tool.query, tool.category)
                                    } else {
                                        onOpenAssistant()
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = tool.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tool.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Platform Capabilities Overview
            item {
                IntelCard(accentBorder = true) {
                    Text(
                        text = "Platform Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CapabilityRow(
                        icon = Icons.Filled.Explore,
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Epistemic Discovery Engine",
                        description = "Structured briefs split established facts from interpretations and speculative ideas."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CapabilityRow(
                        icon = Icons.Filled.AutoAwesome,
                        tint = MaterialTheme.colorScheme.secondary,
                        title = "Streaming Gemini Assistant",
                        description = "Real-time token streaming via Firebase AI Logic with persistent thread history."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CapabilityRow(
                        icon = Icons.Filled.CloudDone,
                        tint = VerifiedEmerald,
                        title = "Isolated Cloud Workspace",
                        description = "Saved discoveries and search history sync securely to your Google account."
                    )
                }
            }

            // Recent Activity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onOpenDashboard) {
                        Text("Workspace")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (discoveries.isEmpty() && searches.isEmpty()) {
                item {
                    IntelCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "No activity yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Run an investigation above or tap a quick-access tool to start building your intelligence workspace.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                if (searches.isNotEmpty()) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searches.take(5).forEach { search ->
                                AssistChip(
                                    onClick = {
                                        onInvestigate(
                                            search.query,
                                            DiscoveryCategory.fromRaw(search.category)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = search.query,
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

                items(discoveries.take(3), key = { it.id }) { record ->
                    IntelCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenDiscoveryRecord(record) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatTimestamp(record.updatedAt ?: record.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.query,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CapabilityRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
