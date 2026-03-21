package com.scrolllight.bible.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.model.SearchResult
import com.scrolllight.bible.data.model.SearchScope
import com.scrolllight.bible.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    onBack: () -> Unit,
    onVerseClick: (String, Int) -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val focus  = remember { FocusRequester() }
    val colors = MaterialTheme.colorScheme
    val isDark  = colors.background.luminance() < 0.15f

    LaunchedEffect(initialQuery) { if (initialQuery.isNotBlank()) vm.search(initialQuery) }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") } },
                title = {
                    OutlinedTextField(
                        value = state.query, onValueChange = { vm.onQueryChange(it) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        placeholder = { Text("搜索经文…") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            if (state.query.isNotBlank()) {
                                IconButton(onClick = { vm.onQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, "清除", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = colors.primary.copy(0.5f),
                            unfocusedBorderColor = colors.outline.copy(0.4f),
                            focusedContainerColor   = if (isDark) colors.surfaceVariant.copy(0.6f) else Color.White.copy(0.7f),
                            unfocusedContainerColor = if (isDark) colors.surfaceVariant.copy(0.4f) else Color.White.copy(0.5f),
                        )
                    )
                },
                actions = {
                    TextButton(onClick = { vm.search(state.query) }) {
                        Text("搜索", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Scope chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchScope.values().toList()) { scope ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick  = { vm.setScope(scope) },
                        label    = { Text(scope.displayName, style = MaterialTheme.typography.labelMedium) },
                        shape    = RoundedCornerShape(20.dp)
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
                state.results.isEmpty() && state.query.isNotBlank() && !state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.SearchOff, null, Modifier.size(48.dp),
                                tint = colors.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("在${state.scope.displayName}中找到 0 个结果",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                state.results.isNotEmpty() -> {
                    Text("在${state.scope.displayName}中找到 ${state.results.size} 个结果",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.results) { result ->
                            SearchResultCard(result, state.query) { onVerseClick(result.bookId, result.chapter) }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                else -> {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("热门搜索", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant)
                        listOf("爱", "信心", "盼望", "平安", "恩典", "救恩", "祷告", "智慧").chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { kw ->
                                    AuroraSurface(
                                        shape = RoundedCornerShape(20.dp),
                                        onClick = { vm.onQueryChange(kw); vm.search(kw) },
                                        modifier = Modifier
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.TrendingUp, null, Modifier.size(14.dp),
                                                tint = colors.primary)
                                            Text(kw, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResult, query: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AuroraCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${result.bookName}  ${result.chapter}:${result.verse}",
                style = MaterialTheme.typography.labelLarge, color = colors.primary, fontWeight = FontWeight.Bold)
            val annotated = buildAnnotatedString {
                val text = result.text
                val s = result.matchStart.coerceAtLeast(0)
                val e = result.matchEnd.coerceAtMost(text.length)
                append(text.substring(0, s))
                withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold,
                    background = colors.primaryContainer.copy(0.5f))) { append(text.substring(s, e)) }
                append(text.substring(e))
            }
            Text(annotated, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
