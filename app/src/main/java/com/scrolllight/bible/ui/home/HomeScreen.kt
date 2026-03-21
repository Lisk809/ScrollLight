package com.scrolllight.bible.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.ui.theme.*

data class FeatureItem(val icon: ImageVector, val label: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBookContents: () -> Unit,
    onNavigateToReading: (String, Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.15f

    val features = listOf(
        FeatureItem(Icons.Outlined.MenuBook,     "圣经目录") { onNavigateToBookContents() },
        FeatureItem(Icons.Outlined.Search,       "搜索经文") { onNavigateToSearch() },
        FeatureItem(Icons.Outlined.AutoStories,  "阅读圣经") {
            onNavigateToReading(state.recentBook?.id ?: "mat", state.recentChapter)
        },
        FeatureItem(Icons.Outlined.Headphones,   "有声圣经") { },
        FeatureItem(Icons.Outlined.WbSunny,      "每日灵粮") { },
        FeatureItem(Icons.Outlined.Article,      "圣经注释") { },
        FeatureItem(Icons.Outlined.LibraryBooks, "圣经辞典") { },
        FeatureItem(Icons.Outlined.Translate,    "圣经原文") { },
        FeatureItem(Icons.Outlined.Download,     "下载内容") { },
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("光言圣经", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = colors.primary)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Daily verse
            item {
                state.dailyVerse?.let { verse ->
                    AuroraDailyVerseCard(
                        theme     = verse.theme,
                        subTheme  = verse.subTheme,
                        text      = verse.text,
                        reference = verse.reference,
                        date      = verse.date,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }

            // Continue reading
            state.recentBook?.let { book ->
                item {
                    AuroraSectionHeader("继续阅读")
                    Spacer(Modifier.height(10.dp))
                    AuroraCard(
                        onClick = { onNavigateToReading(book.id, state.recentChapter) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(42.dp)
                                        .run {
                                            val shape = RoundedCornerShape(12.dp)
                                            this
                                                .clip(shape)
                                                .then(
                                                    Modifier.background(
                                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                                            listOf(colors.primary.copy(0.18f), colors.primary.copy(0.06f))
                                                        )
                                                    )
                                                )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.AutoStories, null, tint = colors.primary,
                                        modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(book.name, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("第${state.recentChapter}章 · 和合本",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Outlined.ArrowForwardIos, null, tint = colors.primary,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Feature grid
            item {
                AuroraSectionHeader("功能")
                Spacer(Modifier.height(10.dp))
                val rows = features.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { item ->
                                AuroraFeatureButton(
                                    icon     = item.icon,
                                    label    = item.label,
                                    onClick  = item.action,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
