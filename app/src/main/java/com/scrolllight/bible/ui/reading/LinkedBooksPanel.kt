package com.scrolllight.bible.ui.reading

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolllight.bible.data.library.BookCatalogEntry
import com.scrolllight.bible.data.library.BookType
import com.scrolllight.bible.data.library.LinkedReadingConfig
import com.scrolllight.bible.data.library.ReadingLayout

// ─────────────────────────────────────────────────────────────────────────────
//  三书联动面板
//
//  三类书各自独立下载（.slbook），但通过 bookId:chapter:verse 统一寻址键同步：
//
//   正文译本  ──┐
//   平行译本  ──┼──► VerseRef(mat:17:1) ──► 阅读视图同步滚动
//   原文版本  ──┤
//   研读注释  ──┘
//
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LinkedBooksPanel(
    installedBooks: List<BookCatalogEntry>,
    config: LinkedReadingConfig,
    onSave:         (LinkedReadingConfig) -> Unit,
    onGoToLibrary:  () -> Unit,
    onDismiss:      () -> Unit
) {
    var draft by remember { mutableStateOf(config) }
    val colors  = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f

    val bibleBooks    = installedBooks.filter { it.type == BookType.BIBLE_TEXT }
    val origBooks     = installedBooks.filter { it.type == BookType.ORIGINAL_TEXT }
    val studyBooks    = installedBooks.filter { it.type == BookType.STUDY_BIBLE || it.type == BookType.COMMENTARY }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("三书联动", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("各书独立下载，通过节次地址同步翻页",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "关闭") }
        }

        // Visual link diagram
        LinkedDiagram(draft)

        HorizontalDivider(color = colors.outline.copy(0.2f))

        // Layer 1: Primary Bible (required)
        BookLayerSelector(
            title       = "① 正文译本",
            subtitle    = "主阅读文本（必选）",
            icon        = Icons.Outlined.Book,
            accentColor = Color(0xFF7A9E7E),
            books       = bibleBooks,
            selectedId  = draft.bibleVersionId,
            isRequired  = true,
            onSelect    = { draft = draft.copy(bibleVersionId = it) },
            onGoToLibrary = onGoToLibrary
        )

        // Layer 2: Parallel Bible (optional)
        BookLayerSelector(
            title       = "② 平行对照译本",
            subtitle    = "与正文并排显示（可选）",
            icon        = Icons.Outlined.CompareArrows,
            accentColor = Color(0xFF8FA3B1),
            books       = bibleBooks.filter { it.bookId != draft.bibleVersionId },
            selectedId  = draft.parallelId,
            isRequired  = false,
            onSelect    = { draft = draft.copy(parallelId = it.takeIf { s -> s != NONE_ID }) },
            onGoToLibrary = onGoToLibrary
        )

        // Layer 3: Original text (optional)
        BookLayerSelector(
            title       = "③ 原文版本",
            subtitle    = "希伯来文（BHS）/ 希腊文（NA28），点词查 Strong 编号（可选）",
            icon        = Icons.Outlined.Translate,
            accentColor = Color(0xFF9B8EC4),
            books       = origBooks,
            selectedId  = draft.originalId,
            isRequired  = false,
            onSelect    = { draft = draft.copy(originalId = it.takeIf { s -> s != NONE_ID }) },
            onGoToLibrary = onGoToLibrary
        )

        // Layer 4: Study notes (optional)
        BookLayerSelector(
            title       = "④ 研读注释",
            subtitle    = "每节旁边显示注释内容（可选）",
            icon        = Icons.Outlined.School,
            accentColor = Color(0xFFB08C88),
            books       = studyBooks,
            selectedId  = draft.studyId,
            isRequired  = false,
            onSelect    = { draft = draft.copy(studyId = it.takeIf { s -> s != NONE_ID }) },
            onGoToLibrary = onGoToLibrary
        )

        // Layout mode
        Text("阅读布局", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LayoutChip(ReadingLayout.SINGLE,   "单栏",    Icons.Outlined.Article,       draft.layout) { draft = draft.copy(layout = it) }
            LayoutChip(ReadingLayout.PARALLEL, "平行",    Icons.Outlined.CompareArrows, draft.layout) { draft = draft.copy(layout = it) }
            LayoutChip(ReadingLayout.ORIGINAL, "原文",    Icons.Outlined.Translate,     draft.layout) { draft = draft.copy(layout = it) }
            LayoutChip(ReadingLayout.STUDY,    "注释",    Icons.Outlined.School,        draft.layout) { draft = draft.copy(layout = it) }
            LayoutChip(ReadingLayout.CARD,     "纸板",    Icons.Outlined.CreditCard,    draft.layout) { draft = draft.copy(layout = it) }
        }

        // Save button
        Button(
            onClick   = { onSave(draft); onDismiss() },
            modifier  = Modifier.fillMaxWidth().height(52.dp),
            shape     = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.Link, null)
            Spacer(Modifier.width(8.dp))
            Text("应用联动配置", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Visual connection diagram ──────────────────────────────────────────────────

@Composable
private fun LinkedDiagram(config: LinkedReadingConfig) {
    val colors = MaterialTheme.colorScheme
    val layers = listOfNotNull(
        Triple(Icons.Outlined.Book,           config.bibleVersionId,        Color(0xFF7A9E7E)),
        config.parallelId?.let  { Triple(Icons.Outlined.CompareArrows, it, Color(0xFF8FA3B1)) },
        config.originalId?.let  { Triple(Icons.Outlined.Translate,     it, Color(0xFF9B8EC4)) },
        config.studyId?.let     { Triple(Icons.Outlined.School,        it, Color(0xFFB08C88)) }
    )

    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    layers.map { (_, _, c) -> c.copy(alpha = 0.12f) }
                        .ifEmpty { listOf(colors.surfaceVariant.copy(0.3f)) }
                )
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        layers.forEachIndexed { i, (icon, id, color) ->
            if (i > 0) {
                // Link connector
                Icon(Icons.Outlined.Link, null,
                    tint = colors.onSurfaceVariant.copy(0.4f),
                    modifier = Modifier.size(14.dp).padding(horizontal = 2.dp))
            }
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.18f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(14.dp), tint = color)
                    Text(id, style = MaterialTheme.typography.labelSmall,
                        color = color, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 64.dp))
                }
            }
        }

        if (layers.size < 2) {
            Spacer(Modifier.width(8.dp))
            Text("选择更多书来建立联动",
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

// ── Per-layer book selector ────────────────────────────────────────────────────

private const val NONE_ID = "__none__"

@Composable
private fun BookLayerSelector(
    title: String, subtitle: String,
    icon: ImageVector, accentColor: Color,
    books: List<BookCatalogEntry>,
    selectedId: String?,
    isRequired: Boolean,
    onSelect: (String) -> Unit,
    onGoToLibrary: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }

        if (books.isEmpty()) {
            // No books installed → prompt to download
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = accentColor.copy(0.08f),
                modifier = Modifier.fillMaxWidth().clickable { onGoToLibrary() }
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CloudDownload, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("尚未下载此类书籍", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("点击前往书库下载", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            // Book selector dropdown
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = colors.surfaceVariant.copy(0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Current selection row
                    Row(
                        Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        val selected = books.firstOrNull { it.bookId == selectedId }
                        if (selectedId == null || selected == null) {
                            Text(if (isRequired) "请选择" else "不使用",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isRequired && selectedId == null) colors.error
                                        else colors.onSurfaceVariant)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(4.dp), color = accentColor.copy(0.12f)) {
                                    Text(selected.abbreviation,
                                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall, color = accentColor,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text(selected.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Icon(
                            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null, Modifier.size(16.dp), tint = colors.onSurfaceVariant
                        )
                    }

                    // Dropdown list
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            HorizontalDivider(color = colors.outline.copy(0.15f))
                            // "不使用" option for optional layers
                            if (!isRequired) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { onSelect(NONE_ID); expanded = false }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("不使用此层", style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant)
                                    if (selectedId == null)
                                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = colors.primary)
                                }
                                HorizontalDivider(color = colors.outline.copy(0.1f))
                            }
                            books.forEach { book ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { onSelect(book.bookId); expanded = false }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = accentColor.copy(0.12f)) {
                                            Text(book.abbreviation, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall, color = accentColor)
                                        }
                                        Column {
                                            Text(book.title, style = MaterialTheme.typography.bodyMedium)
                                            Text(book.language.displayName + if (book.copyright.isNotBlank()) " · ${book.copyright}" else "",
                                                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                        }
                                    }
                                    if (book.bookId == selectedId)
                                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = colors.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Layout chip ────────────────────────────────────────────────────────────────

@Composable
private fun RowScope.LayoutChip(
    layout: ReadingLayout, label: String, icon: ImageVector,
    current: ReadingLayout, onSelect: (ReadingLayout) -> Unit
) {
    val colors   = MaterialTheme.colorScheme
    val selected = current == layout
    Column(
        Modifier.weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primaryContainer.copy(0.6f) else colors.surfaceVariant.copy(0.4f))
            .clickable { onSelect(layout) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (selected) colors.primary else colors.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
