package com.scrolllight.bible.ui.reading

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.ai.AiChatPanel
import com.scrolllight.bible.ai.AiChatViewModel
import com.scrolllight.bible.data.model.HighlightColor
import com.scrolllight.bible.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    bookId: String,
    chapter: Int,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToContents: () -> Unit,
    vm: ReadingViewModel = hiltViewModel()
) {
    val state     by vm.state.collectAsState()
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    var showAi     by remember { mutableStateOf(false) }
    var aiInput    by remember { mutableStateOf("") }
    val colors     = MaterialTheme.colorScheme
    val aiChatVm: AiChatViewModel = hiltViewModel()
    val aiState    by aiChatVm.state.collectAsState()

    LaunchedEffect(bookId, chapter) {
        vm.loadChapter(bookId, chapter)
        vm.observeAiCommands()
    }
    LaunchedEffect(state.scrollToVerse) {
        state.scrollToVerse?.let { v ->
            val idx = state.verses.indexOfFirst { it.verse == v }
            if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
            vm.clearScrollTarget()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") } },
                title = {
                    Column {
                        Text("${state.book?.name ?: ""} ${state.chapter}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("和合本", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToContents) { Icon(Icons.Outlined.FormatListBulleted, "目录") }
                    IconButton(onClick = { vm.toggleParallel() }) {
                        Icon(Icons.Outlined.CompareArrows, "对照",
                            tint = if (state.showParallel) colors.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = onNavigateToSearch) { Icon(Icons.Outlined.Search, "搜索") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            AuroraAiButton(onClick = { showAi = true })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(state.verses) { _, verse ->
                    val hlColor = state.highlights[verse.verse]
                    val aiHl    = state.aiHighlights[verse.verse]
                    val annotation = state.aiAnnotations[verse.verse]
                    val isSelected = state.selectedVerse == verse.verse
                    val highlightColor = when {
                        aiHl != null -> Color(android.graphics.Color.parseColor(HighlightColor.valueOf(aiHl).hex))
                        hlColor != null -> Color(android.graphics.Color.parseColor(hlColor.hex))
                        else -> Color.Transparent
                    }
                    Column {
                        AuroraVerseRow(
                            verseNumber    = verse.verse,
                            text           = verse.text,
                            isHighlighted  = hlColor != null || aiHl != null,
                            highlightColor = highlightColor,
                            isSelected     = isSelected,
                            onClick        = { vm.selectVerse(if (isSelected) null else verse.verse) }
                        )
                        if (state.showParallel && verse.textEn != null) {
                            Text(verse.textEn, modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                                fontStyle = FontStyle.Italic)
                        }
                        annotation?.let {
                            AiAnnotationBubble(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                    AnimatedVisibility(visible = isSelected) {
                        VerseActionBar(
                            onHighlight = { vm.toggleHighlight(verse.verse) },
                            onBookmark  = {}, onNote = {}, onShare = {}
                        )
                    }
                }
                // Chapter navigation
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { vm.navigateChapter(-1) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, null)
                            Spacer(Modifier.width(4.dp))
                            Text("上一章")
                        }
                        OutlinedButton(
                            onClick = { vm.navigateChapter(1) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("下一章")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.ChevronRight, null)
                        }
                    }
                }
            }
        }
    }

    if (showAi) {
        ModalBottomSheet(
            onDismissRequest = { showAi = false },
            containerColor = Color.Transparent,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val glass = LocalGlassParams.current
            val isDark = colors.background.luminance() < 0.15f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .glassBackground(
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        if (isDark) colors.surfaceVariant else Color.White, glass.cardAlpha + 0.1f
                    )
            ) {
                AiChatPanel(
                    messages     = aiState.bubbles.filterIsInstance<com.scrolllight.bible.ai.ChatBubble.Assistant>()
                        .map { com.scrolllight.bible.ai.AiMessage(com.scrolllight.bible.ai.AiMessage.Role.ASSISTANT, it.text) },
                    input        = aiInput,
                    onInputChange = { aiInput = it },
                    onSend       = { aiChatVm.send(aiInput); aiInput = "" },
                    context      = "${state.book?.name} ${state.chapter}",
                    modifier     = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VerseActionBar(onHighlight: () -> Unit, onBookmark: () -> Unit, onNote: () -> Unit, onShare: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 4.dp)
            .glassBackground(RoundedCornerShape(14.dp), if (isDark) colors.surfaceVariant else Color.White, 0.85f, elevation = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(Icons.Outlined.Highlight to "高亮" to onHighlight,
               Icons.Outlined.Bookmark  to "收藏" to onBookmark,
               Icons.Outlined.Edit      to "笔记" to onNote,
               Icons.Outlined.Share     to "分享" to onShare).forEach { (iconLabel, action) ->
            val (icon, label) = iconLabel
            IconButton(onClick = action) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, label, modifier = Modifier.size(19.dp), tint = colors.primary)
                    Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun AiAnnotationBubble(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    Row(
        modifier = modifier.fillMaxWidth()
            .glassBackground(RoundedCornerShape(12.dp), if (isDark) colors.surfaceVariant else Color.White,
                0.75f, elevation = 3.dp)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("✦", color = colors.primary, fontSize = 12.sp)
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
    }
}
