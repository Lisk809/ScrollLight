package com.scrolllight.bible.ui.reading

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.model.BibleBook
import com.scrolllight.bible.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookContentsScreen(
    onBack: () -> Unit,
    onSelectChapter: (String, Int) -> Unit,
    vm: ReadingViewModel = hiltViewModel()
) {
    val books = vm.allBooks
    var selectedTestament by remember { mutableIntStateOf(0) }
    var expandedBook by remember { mutableStateOf<String?>(null) }
    val filteredBooks = if (selectedTestament == 0) books.filter { it.isOldTestament }
                        else books.filter { it.isNewTestament }
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("圣经目录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Testament toggle pill
            val baseColor = if (isDark) colors.surfaceVariant else Color.White
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .glassBackground(RoundedCornerShape(50), baseColor, 0.75f, elevation = 4.dp)
                    .padding(4.dp)
            ) {
                listOf("旧约", "新约").forEachIndexed { idx, label ->
                    val selected = selectedTestament == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) Brush.horizontalGradient(
                                    listOf(colors.primary.copy(0.85f), colors.secondary.copy(0.75f))
                                ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { selectedTestament = idx; expandedBook = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selected) Color.White else colors.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredBooks) { book ->
                    BookItem(
                        book = book,
                        isExpanded  = expandedBook == book.id,
                        onToggle    = { expandedBook = if (expandedBook == book.id) null else book.id },
                        onSelectChapter = { ch -> onSelectChapter(book.id, ch) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun BookItem(book: BibleBook, isExpanded: Boolean, onToggle: () -> Unit, onSelectChapter: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f
    val glass   = LocalGlassParams.current
    val baseColor = if (isDark) colors.surfaceVariant else Color.White

    AuroraCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = if (isExpanded) colors.primary else null
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(if (isExpanded) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(book.abbr, style = MaterialTheme.typography.labelMedium,
                        color = colors.primary, fontWeight = FontWeight.Bold)
                }
                Text(book.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${book.chapterCount}章", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Icon(
                    if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant
                )
            }
        }
        // Chapter grid
        if (isExpanded) {
            HorizontalDivider(color = colors.outline.copy(0.2f), modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..book.chapterCount).chunked(7).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { ch ->
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(colors.primary.copy(0.10f))
                                    .clickable { onSelectChapter(ch) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$ch", style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
