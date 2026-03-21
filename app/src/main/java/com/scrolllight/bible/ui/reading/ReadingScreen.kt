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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.model.BibleVerse
import com.scrolllight.bible.data.model.HighlightColor
import com.scrolllight.bible.ui.components.AiFloatingButton
import com.scrolllight.bible.ui.components.VerseRow
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
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showAiSheet by remember { mutableStateOf(false) }
    var aiInput by remember { mutableStateOf("") }

    LaunchedEffect(bookId, chapter) {
        vm.loadChapter(bookId, chapter)
        vm.observeAiCommands()
    }

    // Respond to AI scroll command
    LaunchedEffect(state.scrollToVerse) {
        state.scrollToVerse?.let { v ->
            val idx = state.verses.indexOfFirst { it.verse == v }
            if (idx >= 0) listState.animateScrollToItem(idx)
            vm.clearScrollTarget()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") }
                },
                title = {
                    Column {
                        Text(
                            "${state.book?.name ?: ""} ${state.chapter}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("和合本", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToContents) { Icon(Icons.Outlined.FormatListBulleted, "目录") }
                    IconButton(onClick = { vm.toggleParallel() }) {
                        Icon(Icons.Outlined.CompareArrows, "对照",
                            tint = if (state.showParallel) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = onNavigateToSearch) { Icon(Icons.Outlined.Search, "搜索") }
                    IconButton(onClick = {}) { Icon(Icons.Outlined.MoreVert, "更多") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            AiFloatingButton(onClick = { showAiSheet = true })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        VerseRow(
                            verseNumber   = verse.verse,
                            text          = verse.text,
                            isHighlighted = hlColor != null || aiHl != null,
                            highlightColor = highlightColor,
                            isSelected    = isSelected,
                            onClick       = { vm.selectVerse(if (isSelected) null else verse.verse) }
                        )

                        // Parallel NIV
                        if (state.showParallel && verse.textEn != null) {
                            Text(
                                text = verse.textEn,
                                modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        // AI annotation bubble
                        annotation?.let {
                            AiAnnotationBubble(text = it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }

                    // Verse action bar (selected)
                    AnimatedVisibility(visible = isSelected) {
                        VerseActionBar(
                            onHighlight = { vm.toggleHighlight(verse.verse) },
                            onBookmark  = {},
                            onNote      = {},
                            onShare     = {}
                        )
                    }
                }

                // Chapter navigation
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { vm.navigateChapter(-1) }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.ChevronLeft, null)
                            Spacer(Modifier.width(4.dp))
                            Text("上一章")
                        }
                        OutlinedButton(onClick = { vm.navigateChapter(1) }, shape = RoundedCornerShape(12.dp)) {
                            Text("下一章")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.ChevronRight, null)
                        }
                    }
                }
            }
        }
    }

    // AI Panel bottom sheet
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AiChatPanel(
                messages   = state.aiMessages,
                input      = aiInput,
                onInputChange = { aiInput = it },
                onSend     = { vm.sendAiMessage(aiInput); aiInput = "" },
                context    = "${state.book?.name} ${state.chapter}",
                modifier   = Modifier.height(480.dp)
            )
        }
    }
}

@Composable
private fun VerseActionBar(
    onHighlight: () -> Unit,
    onBookmark: () -> Unit,
    onNote: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            Icons.Outlined.Highlight to "高亮" to onHighlight,
            Icons.Outlined.Bookmark  to "收藏" to onBookmark,
            Icons.Outlined.Edit      to "笔记" to onNote,
            Icons.Outlined.Share     to "分享" to onShare
        ).forEach { (iconLabel, action) ->
            val (icon, label) = iconLabel
            IconButton(onClick = action) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun AiAnnotationBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AiChatPanel(
    messages: List<com.scrolllight.bible.ai.AiMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    context: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("✦", modifier = Modifier.padding(6.dp), color = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text("AI 助读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(context, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Divider()

        // Message list
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp), reverseLayout = true) {
            items(messages.reversed().size) { idx ->
                val msg = messages.reversed()[idx]
                val isUser = msg.role == com.scrolllight.bible.ai.AiMessage.Role.USER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            msg.content,
                            modifier = Modifier.padding(10.dp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("询问关于经文的问题…") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank(),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (input.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Outlined.Send, "发送",
                    tint = if (input.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun items(count: Int, itemContent: @Composable (Int) -> Unit): androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
    repeat(count) { idx -> item { itemContent(idx) } }
}
private fun androidx.compose.foundation.lazy.LazyListScope.items(count: Int, itemContent: @Composable (Int) -> Unit) {
    repeat(count) { idx -> item { itemContent(idx) } }
}
