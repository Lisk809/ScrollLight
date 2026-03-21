package com.scrolllight.bible.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.model.BibleBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookContentsScreen(
    onBack: () -> Unit,
    onSelectChapter: (String, Int) -> Unit,
    vm: ReadingViewModel = hiltViewModel()
) {
    val books = vm.allBooks
    var selectedTestament by remember { mutableStateOf(0) } // 0=OT, 1=NT
    var expandedBook by remember { mutableStateOf<String?>(null) }

    val filteredBooks = if (selectedTestament == 0) books.filter { it.isOldTestament }
                        else books.filter { it.isNewTestament }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("圣经目录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Testament toggle
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("旧约", "新约").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedTestament == idx) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedTestament = idx; expandedBook = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selectedTestament == idx) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedTestament == idx) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredBooks) { book ->
                    BookItem(
                        book = book,
                        isExpanded = expandedBook == book.id,
                        onToggle = { expandedBook = if (expandedBook == book.id) null else book.id },
                        onSelectChapter = { ch -> onSelectChapter(book.id, ch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookItem(
    book: BibleBook,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelectChapter: (Int) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isExpanded) 0.dp else 1.dp
    ) {
        Column {
            // Book header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Abbr chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            book.abbr,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(book.name, style = MaterialTheme.typography.titleMedium)
                }
                Text("${book.chapterCount}章", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Chapter grid (expanded)
            if (isExpanded) {
                val chunks = (1..book.chapterCount).chunked(7)
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chunks.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { ch ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable { onSelectChapter(ch) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$ch", style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
