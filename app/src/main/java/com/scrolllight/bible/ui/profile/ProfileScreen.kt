package com.scrolllight.bible.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToAiSettings: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    vm: ProfileViewModel = hiltViewModel(),
    themeVm: ThemeViewModel = hiltViewModel()
) {
    val state     by vm.state.collectAsState()
    val themeMode by themeVm.themeMode.collectAsState()
    val colors     = MaterialTheme.colorScheme
    val isDark     = colors.background.luminance() < 0.15f

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("我的", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile header
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("L", style = MaterialTheme.typography.headlineMedium,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Lisk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("编辑资料", style = MaterialTheme.typography.bodySmall, color = colors.primary)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Stats
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("笔记",  "${state.noteCount}",      Modifier.weight(1f))
                    StatCard("高亮",  "${state.highlightCount}", Modifier.weight(1f))
                    StatCard("收藏",  "0",                       Modifier.weight(1f))
                }
            }

            // Theme switcher
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("主题风格", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.values().forEach { mode ->
                                val selected = themeMode == mode
                                Column(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) colors.primary.copy(0.15f)
                                            else colors.surfaceVariant.copy(0.5f)
                                        )
                                        .clickable { themeVm.setThemeMode(mode) }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(mode.icon, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) colors.primary else colors.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Menu
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MenuItem(Icons.Outlined.Edit,          "我的笔记",     badge = "${state.noteCount}")
                        MenuItem(Icons.Outlined.Highlight,     "我的高亮",     badge = "${state.highlightCount}")
                        MenuItem(Icons.Outlined.Bookmark,      "我的收藏")
                        MenuItem(Icons.Outlined.ShoppingCart,  "已购资源")
                        MenuItem(Icons.Outlined.Download,      "我的下载",     badge = "1")
                    }
                }
            }
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MenuItem(Icons.Outlined.Group, "小组")
                    }
                }
            }
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MenuItem(Icons.Outlined.Favorite, "奉献支持")
                    }
                }
            }
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MenuItem(Icons.Outlined.Notifications, "通知")
                        MenuItem(Icons.Outlined.Feedback,      "反馈")
                        MenuItem(Icons.Outlined.ThumbUp,       "给好评")
                        MenuItem(Icons.Outlined.Share,         "推荐给朋友")
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outline.copy(0.2f))
                        MenuItem(Icons.Outlined.LibraryBooks, "书库管理",
                        onClick = onNavigateToLibrary)
                    MenuItem(Icons.Outlined.AutoAwesome,   "AI 助读设置",  onClick = onNavigateToAiSettings)
                        MenuItem(Icons.Outlined.Settings,      "设置")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AuroraCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector, label: String,
    value: String? = null, badge: String? = null,
    onClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                .background(colors.primary.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, label, tint = colors.primary, modifier = Modifier.size(18.dp)) }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (badge != null) Text(badge, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        if (value != null) Text(value, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Icon(Icons.Outlined.ChevronRight, null, tint = colors.outline, modifier = Modifier.size(16.dp))
    }
}
