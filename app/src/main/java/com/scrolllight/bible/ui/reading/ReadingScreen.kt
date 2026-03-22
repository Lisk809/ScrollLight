package com.scrolllight.bible.ui.reading

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.ai.AiChatViewModel
import com.scrolllight.bible.data.library.*
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
    val state      by vm.state.collectAsState()
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()
    val colors      = MaterialTheme.colorScheme
    val isDark      = colors.background.luminance() < 0.15f
    val aiChatVm: AiChatViewModel = hiltViewModel()
    val aiState    by aiChatVm.state.collectAsState()

    var showVersionSheet by remember { mutableStateOf(false) }
    var aiInput          by remember { mutableStateOf("") }
    var showAiSheet      by remember { mutableStateOf(false) }

    val linkedConfig by vm.linkedConfig.collectAsState(
        initial = com.scrolllight.bible.data.library.LinkedReadingConfig()
    )

    LaunchedEffect(Unit) {
        vm.loadLinkedConfig()   // restore saved three-book link on each open
    }
    LaunchedEffect(bookId, chapter) {
        vm.loadChapter(bookId, chapter)
        vm.observeAiCommands()
    }
    LaunchedEffect(state.scrollToVerse) {
        state.scrollToVerse?.let { v ->
            val verses = if (state.versesLibrary.isNotEmpty()) state.versesLibrary else state.verses
            val idx = verses.indexOfFirst { it is com.scrolllight.bible.data.model.BibleVerse && (it as com.scrolllight.bible.data.model.BibleVerse).verse == v }
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
                        // Show active layer indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LayerChip("正文", colors.primary)
                            if (state.showParallel)
                                LayerChip(state.secondaryVersionId?.uppercase() ?: "平行", Color(0xFF8FA3B1))
                            if (state.showOriginal)
                                LayerChip(state.originalVersionId?.uppercase() ?: "原文", Color(0xFF9B8EC4))
                            if (state.showStudyPanel)
                                LayerChip("注释", Color(0xFFB08C88))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToContents) { Icon(Icons.Outlined.FormatListBulleted, "目录") }
                    // Layer switcher button
                    IconButton(onClick = { showVersionSheet = true }) {
                        Icon(Icons.Outlined.Layers, "切换版本/视图",
                            tint = if (state.showParallel || state.showOriginal || state.showStudyPanel)
                                colors.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = onNavigateToSearch) { Icon(Icons.Outlined.Search, "搜索") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = { AuroraAiButton(onClick = { showAiSheet = true }) }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            when (state.layout) {
                ReadingLayout.CARD -> CardReadingView(state, vm, Modifier.padding(padding))
                else -> ScrollReadingView(state, vm, listState, Modifier.padding(padding))
            }
        }
    }

    // Three-book link panel
    if (showVersionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVersionSheet = false },
            containerColor   = if (isDark) colors.surfaceVariant else Color.White.copy(0.96f),
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            LinkedBooksPanel(
                installedBooks = state.availableBibleVersions,
                config         = linkedConfig,
                onSave         = { vm.applyLinkedConfig(it) },
                onGoToLibrary  = { showVersionSheet = false },
                onDismiss      = { showVersionSheet = false }
            )
        }
    }

    // AI sheet
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            containerColor   = Color.Transparent,
            dragHandle       = null,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().height(480.dp)
                    .glassBackground(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        if (isDark) colors.surfaceVariant else Color.White, 0.94f)
            ) {
                com.scrolllight.bible.ai.AiChatPanel(
                    messages = aiState.bubbles
                        .filterIsInstance<com.scrolllight.bible.ai.ChatBubble.Assistant>()
                        .map { com.scrolllight.bible.ai.AiMessage(com.scrolllight.bible.ai.AiMessage.Role.ASSISTANT, it.text) },
                    input = aiInput,
                    onInputChange = { aiInput = it },
                    onSend = { aiChatVm.send(aiInput); aiInput = "" },
                    context = "${state.book?.name} ${state.chapter}",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Strong's word popup
    state.selectedWord?.let { word ->
        AlertDialog(
            onDismissRequest = { vm.dismissWordPopup() },
            title = {
                Column {
                    Text(word.surface, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    if (word.transliteration.isNotBlank())
                        Text(word.transliteration, style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (word.strongs.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = colors.primaryContainer) {
                            Text(word.strongs, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium, color = colors.primary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    if (word.lemma.isNotBlank())
                        Text("词根：${word.lemma}", style = MaterialTheme.typography.bodyMedium)
                    if (word.morph.isNotBlank())
                        Text("形态：${word.morph}", style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    if (state.strongsEntry != null) {
                        val strongs = state.strongsEntry
                        HorizontalDivider()
                        Text(strongs.definition, style = MaterialTheme.typography.bodyMedium)
                        if (strongs.usage.isNotBlank())
                            Text(strongs.usage, style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant)
                    } else if (word.gloss.isNotBlank()) {
                        Text(word.gloss, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissWordPopup() }) { Text("关闭") }
            }
        )
    }
}

// ── Scroll reading view ───────────────────────────────────────────────────────

@Composable
private fun ScrollReadingView(
    state: ReadingUiState,
    vm: ReadingViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f

    // Merge verse sources: prefer library, fall back to built-in
    val chapterNum = state.chapter
    val verses = state.versesLibrary.ifEmpty {
        state.verses.mapIndexed { _, v ->
            BibleVerseEntity(state.book?.id ?: "", chapterNum, v.verse, "cuv", v.text)
        }
    }

    LazyColumn(
        state   = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Chapter intro note (if study mode active)
        val introNote = state.studyNotes[0]?.firstOrNull()
        if (state.showStudyPanel && introNote != null) {
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(introNote.title.ifBlank { "章节引言" },
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = colors.primary)
                        Text(introNote.content, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        items(count = verses.size) { idx ->
            val verse = verses[idx]
            val verseNum = verse.verse
            val hlColor  = state.highlights[verseNum]
            val aiHl     = state.aiHighlights[verseNum]
            val annotation = state.aiAnnotations[verseNum]
            val isSelected = state.selectedVerse == verseNum

            val highlightColor = when {
                aiHl != null    -> Color(android.graphics.Color.parseColor(HighlightColor.valueOf(aiHl).hex))
                hlColor != null -> Color(android.graphics.Color.parseColor(hlColor.hex))
                else            -> Color.Transparent
            }

            Column(modifier = Modifier.padding(horizontal = 0.dp)) {
                when (state.layout) {
                    ReadingLayout.PARALLEL -> ParallelVerseRow(
                        verse1 = verse,
                        verse2 = state.parallelVerses.firstOrNull { it.verse == verseNum },
                        isHighlighted = hlColor != null || aiHl != null,
                        highlightColor = highlightColor,
                        isSelected = isSelected,
                        onClick = { vm.selectVerse(if (isSelected) null else verseNum) }
                    )
                    ReadingLayout.ORIGINAL -> OriginalVerseBlock(
                        verse = verse,
                        words = state.originalWords[verseNum] ?: emptyList(),
                        isHighlighted = hlColor != null || aiHl != null,
                        highlightColor = highlightColor,
                        isSelected = isSelected,
                        onVerseClick = { vm.selectVerse(if (isSelected) null else verseNum) },
                        onWordClick  = { word -> vm.onWordTap(word) }
                    )
                    else -> {
                        AuroraVerseRow(
                            verseNumber    = verseNum,
                            text           = verse.text,
                            isHighlighted  = hlColor != null || aiHl != null,
                            highlightColor = highlightColor,
                            isSelected     = isSelected,
                            onClick        = { vm.selectVerse(if (isSelected) null else verseNum) }
                        )
                        // Secondary text (study version parallel line)
                        if (state.showParallel && state.layout != ReadingLayout.PARALLEL) {
                            state.parallelVerses.firstOrNull { it.verse == verseNum }?.let { p ->
                                Text(p.text, Modifier.padding(start = 48.dp, end = 16.dp, bottom = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant, fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }

                // AI annotation
                annotation?.let {
                    AiAnnotationBubble(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }

                // Study note for this verse
                if (state.showStudyPanel) {
                    state.studyNotes[verseNum]?.forEach { note ->
                        StudyNoteCard(note, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }

                // Verse action bar
                AnimatedVisibility(isSelected) {
                    VerseActionBar(
                        onHighlight = { vm.toggleHighlight(verseNum) },
                        onBookmark  = {}, onNote = {}, onShare = {}
                    )
                }
            }
        }

        // Chapter navigation
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick   = { vm.navigateChapter(-1) },
                    modifier  = Modifier.weight(1f),
                    shape     = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.ChevronLeft, null); Spacer(Modifier.width(4.dp)); Text("上一章")
                }
                OutlinedButton(
                    onClick   = { vm.navigateChapter(1) },
                    modifier  = Modifier.weight(1f),
                    shape     = RoundedCornerShape(14.dp)
                ) {
                    Text("下一章"); Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.ChevronRight, null)
                }
            }
        }
    }
}

// ── Card reading view (纸板模式) ──────────────────────────────────────────────

@Composable
private fun CardReadingView(
    state: ReadingUiState,
    vm: ReadingViewModel,
    modifier: Modifier = Modifier
) {
    val chapterNum = state.chapter
    val verses = state.versesLibrary.ifEmpty {
        state.verses.mapIndexed { _, v ->
            BibleVerseEntity(state.book?.id ?: "", chapterNum, v.verse, "cuv", v.text)
        }
    }
    var cardIndex by remember { mutableIntStateOf(0) }
    val colors    = MaterialTheme.colorScheme
    val verse     = verses.getOrNull(cardIndex)

    if (verse == null) return

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        // Progress
        Text("${cardIndex + 1} / ${verses.size}", style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { (cardIndex + 1f) / verses.size },
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color    = colors.primary.copy(0.6f), trackColor = colors.primary.copy(0.12f)
        )

        // Verse card
        AuroraCard(
            modifier = Modifier.fillMaxWidth().weight(1f),
            glowColor = colors.primary
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement   = Arrangement.Center,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    // Verse number chip
                    Surface(shape = RoundedCornerShape(20.dp), color = colors.primary.copy(0.12f)) {
                        Text("第 ${verse.verse} 节", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text      = verse.text,
                        style     = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    // Parallel text
                    state.parallelVerses.firstOrNull { it.verse == verse.verse }?.let { p ->
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = colors.outline.copy(0.2f))
                        Spacer(Modifier.height(12.dp))
                        Text(p.text, style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic)
                        Text(state.secondaryVersionId?.uppercase() ?: "", style = MaterialTheme.typography.labelSmall,
                            color = colors.primary.copy(0.6f))
                    }
                }
            }
        }

        // Study note for this verse (card mode)
        if (state.showStudyPanel) {
            state.studyNotes[verse.verse]?.firstOrNull()?.let { note ->
                StudyNoteCard(note, Modifier.fillMaxWidth())
            }
        }

        // Navigation buttons
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick  = { if (cardIndex > 0) cardIndex-- },
                enabled  = cardIndex > 0,
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Outlined.ChevronLeft, null); Text("上一节") }
            Button(
                onClick  = { if (cardIndex < verses.size - 1) cardIndex++ else vm.navigateChapter(1) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (cardIndex < verses.size - 1) "下一节" else "下一章")
                Icon(Icons.Outlined.ChevronRight, null)
            }
        }
    }
}

// ── Parallel verse row ────────────────────────────────────────────────────────

@Composable
private fun ParallelVerseRow(
    verse1: BibleVerseEntity,
    verse2: BibleVerseEntity?,
    isHighlighted: Boolean,
    highlightColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.45f)
        else if (isHighlighted) highlightColor.copy(0.35f)
        else Color.Transparent,
        tween(180), label = "parallelBg"
    )
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(bg).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Verse number
        Text("${verse1.verse}", Modifier.width(22.dp).paddingFromBaseline(20.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(0.75f), fontWeight = FontWeight.Bold)
        // Primary
        Text(verse1.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        // Divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(0.2f)))
        // Secondary
        Text(
            verse2?.text ?: "—", Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (verse2 == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            fontStyle = if (verse2 == null) FontStyle.Italic else FontStyle.Normal
        )
    }
}

// ── Original text verse block ─────────────────────────────────────────────────

@Composable
private fun OriginalVerseBlock(
    verse: BibleVerseEntity,
    words: List<OriginalWordEntity>,
    isHighlighted: Boolean,
    highlightColor: Color,
    isSelected: Boolean,
    onVerseClick: () -> Unit,
    onWordClick: (OriginalWordEntity) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (isSelected) colors.primaryContainer.copy(0.45f)
        else if (isHighlighted) highlightColor.copy(0.35f) else Color.Transparent,
        tween(180), label = "origBg"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp)).background(bg)
            .clickable(onClick = onVerseClick).padding(8.dp)
    ) {
        // Main text
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${verse.verse}", Modifier.width(22.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary.copy(0.75f), fontWeight = FontWeight.Bold)
            Text(verse.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
        // Original words row
        if (words.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = colors.outline.copy(0.15f))
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.padding(start = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = words.size) { i ->
                    val word = words[i]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.primaryContainer.copy(0.4f),
                        modifier = Modifier.clickable { onWordClick(word) }
                    ) {
                        Column(
                            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(word.surface, style = MaterialTheme.typography.bodyMedium,
                                color = colors.primary, fontWeight = FontWeight.Medium)
                            if (word.gloss.isNotBlank())
                                Text(word.gloss, style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant)
                            if (word.strongs.isNotBlank())
                                Text(word.strongs, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = colors.primary.copy(0.5f))
                        }
                    }
                }
            }
        }
    }
}

// ── Study note card ───────────────────────────────────────────────────────────

@Composable
private fun StudyNoteCard(note: StudyNoteEntity, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        color     = colors.secondaryContainer.copy(0.4f)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Article, null, Modifier.size(14.dp), colors.secondary)
                    Text(note.title.ifBlank { NoteType.valueOf(note.noteType.name).label() },
                        style = MaterialTheme.typography.labelMedium, color = colors.secondary,
                        fontWeight = FontWeight.SemiBold)
                }
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, Modifier.size(14.dp), colors.onSurfaceVariant)
            }
            AnimatedVisibility(expanded) {
                Text(note.content, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            }
        }
    }
}

private fun NoteType.label() = when (this) {
    NoteType.INTRODUCTION -> "引言"
    NoteType.VERSE_NOTE   -> "节注"
    NoteType.CROSS_REF    -> "交叉参考"
    NoteType.WORD_STUDY   -> "词语研究"
    NoteType.THEME_NOTE   -> "主题"
}

// ── Version / Layer selector sheet ───────────────────────────────────────────

@Composable
private fun VersionLayerSheet(
    state: ReadingUiState,
    onToggleParallel: (String?) -> Unit,
    onToggleOriginal: (String?) -> Unit,
    onToggleStudy:    (String?) -> Unit,
    onToggleCard:     () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("阅读模式 & 版本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider()

        // Layout modes
        Text("布局", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LayoutModeButton("单栏", Icons.Outlined.Article,
                state.layout == ReadingLayout.SINGLE) { onToggleParallel(null) }
            LayoutModeButton("纸板", Icons.Outlined.CreditCard,
                state.layout == ReadingLayout.CARD) { onToggleCard() }
        }

        // Parallel versions
        val bibleVersions = state.availableBibleVersions.filter {
            it.type == BookType.BIBLE_TEXT && it.bookId != state.primaryVersionId
        }
        if (bibleVersions.isNotEmpty()) {
            Text("平行对照译本", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            bibleVersions.forEach { v ->
                val isActive = state.showParallel && state.secondaryVersionId == v.bookId
                LayerToggleRow(
                    title    = v.title,
                    subtitle = v.abbreviation,
                    icon     = Icons.Outlined.CompareArrows,
                    iconColor = Color(0xFF8FA3B1),
                    isActive = isActive,
                    badge    = "译本",
                    onClick  = { onToggleParallel(if (isActive) null else v.bookId) }
                )
            }
        }

        // Original text versions
        val origVersions = state.availableBibleVersions.filter { it.type == BookType.ORIGINAL_TEXT }
        if (origVersions.isNotEmpty()) {
            Text("原文版本", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            origVersions.forEach { v ->
                val isActive = state.showOriginal && state.originalVersionId == v.bookId
                LayerToggleRow(
                    title     = v.title,
                    subtitle  = v.abbreviation,
                    icon      = Icons.Outlined.Translate,
                    iconColor = Color(0xFF9B8EC4),
                    isActive  = isActive,
                    badge     = "原文",
                    onClick   = { onToggleOriginal(if (isActive) null else v.bookId) }
                )
            }
        }

        // Study versions
        val studyVersions = state.availableBibleVersions.filter {
            it.type == BookType.STUDY_BIBLE || it.type == BookType.COMMENTARY
        }
        if (studyVersions.isNotEmpty()) {
            Text("研读注释", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            studyVersions.forEach { v ->
                val isActive = state.showStudyPanel && state.studyVersionId == v.bookId
                LayerToggleRow(
                    title     = v.title,
                    subtitle  = v.abbreviation,
                    icon      = Icons.Outlined.School,
                    iconColor = Color(0xFFB08C88),
                    isActive  = isActive,
                    badge     = "注释",
                    onClick   = { onToggleStudy(if (isActive) null else v.bookId) }
                )
            }
        }

        if (bibleVersions.isEmpty() && origVersions.isEmpty() && studyVersions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant.copy(0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("尚未安装其他版本", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text("前往「我的 → 书库管理」下载：\n• 平行对照译本（NIV、ESV等）\n• 原文版本（BHS希伯来文、NA28希腊文）\n• 研读本（含注释）",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun LayerChip(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(0.15f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = color)
    }
}

@Composable
private fun RowScope.LayoutModeButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primaryContainer.copy(0.6f) else colors.surfaceVariant.copy(0.5f))
            .clickable(onClick = onClick).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = if (selected) colors.primary else colors.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun LayerToggleRow(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color, isActive: Boolean, badge: String, onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) iconColor.copy(0.12f) else colors.surfaceVariant.copy(0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(4.dp), color = iconColor.copy(0.12f)) {
            Text(badge, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = iconColor)
        }
        Switch(checked = isActive, onCheckedChange = null)
    }
}

@Composable
private fun VerseActionBar(onHighlight: () -> Unit, onBookmark: () -> Unit, onNote: () -> Unit, onShare: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 4.dp)
            .glassBackground(RoundedCornerShape(14.dp), if (isDark) colors.surfaceVariant else Color.White, 0.85f, elevation = 4.dp),
        Arrangement.SpaceEvenly
    ) {
        listOf(Icons.Outlined.Highlight to "高亮" to onHighlight,
               Icons.Outlined.Bookmark  to "收藏" to onBookmark,
               Icons.Outlined.Edit      to "笔记" to onNote,
               Icons.Outlined.Share     to "分享" to onShare).forEach { (iconLabel, action) ->
            val (icon, label) = iconLabel
            IconButton(onClick = action) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, label, Modifier.size(19.dp), colors.primary)
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
            .glassBackground(RoundedCornerShape(12.dp), if (isDark) colors.surfaceVariant else Color.White, 0.75f, elevation = 3.dp)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("✦", color = colors.primary, fontSize = 12.sp)
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    itemContent: @Composable (Int) -> Unit
) {
    repeat(count) { idx -> item { itemContent(idx) } }
}
