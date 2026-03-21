package com.scrolllight.bible.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.ui.components.DailyVerseCard
import com.scrolllight.bible.ui.components.FeatureButton
import com.scrolllight.bible.ui.components.SectionHeader

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

    val features = listOf(
        FeatureItem(Icons.Outlined.MenuBook,    "圣经目录") { onNavigateToBookContents() },
        FeatureItem(Icons.Outlined.Search,      "搜索经文") { onNavigateToSearch() },
        FeatureItem(Icons.Outlined.AutoStories, "阅读圣经") {
            onNavigateToReading(state.recentBook?.id ?: "mat", state.recentChapter)
        },
        FeatureItem(Icons.Outlined.Headphones,  "有声圣经") { },
        FeatureItem(Icons.Outlined.WbSunny,     "每日灵粮") { },
        FeatureItem(Icons.Outlined.Article,     "圣经注释") { },
        FeatureItem(Icons.Outlined.LibraryBooks,"圣经辞典") { },
        FeatureItem(Icons.Outlined.Translate,   "圣经原文") { },
        FeatureItem(Icons.Outlined.Download,    "下载内容") { },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("光言圣经", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Daily verse
            item {
                state.dailyVerse?.let { verse ->
                    DailyVerseCard(
                        theme    = verse.theme,
                        subTheme = verse.subTheme,
                        text     = verse.text,
                        reference = verse.reference,
                        date     = verse.date,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Continue reading
            state.recentBook?.let { book ->
                item {
                    SectionHeader("继续阅读")
                    Spacer(Modifier.height(8.dp))
                    ElevatedCard(
                        onClick = { onNavigateToReading(book.id, state.recentChapter) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(book.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("第${state.recentChapter}章 · 和合本",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ArrowForwardIos, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Feature grid
            item {
                SectionHeader("功能")
                Spacer(Modifier.height(8.dp))
                // 3-column non-scrollable grid inside LazyColumn
                val rows = features.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { item ->
                                FeatureButton(
                                    icon    = item.icon,
                                    label   = item.label,
                                    onClick = item.action,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty cells in last row
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
