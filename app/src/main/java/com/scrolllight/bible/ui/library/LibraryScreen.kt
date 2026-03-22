package com.scrolllight.bible.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.library.*
import com.scrolllight.bible.ui.theme.AuroraCard
import com.scrolllight.bible.ui.theme.glassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    vm: LibraryViewModel = hiltViewModel()
) {
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    val colors   = MaterialTheme.colorScheme
    val isDark   = colors.background.luminance() < 0.15f

    // File picker for importing .slbook
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.importFromUri(it, context) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.LibraryBooks, null, tint = colors.primary)
                        Text("书库管理", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "返回") } },
                actions = {
                    IconButton(onClick = { filePicker.launch("*/*") }) {
                        Icon(Icons.Outlined.FileUpload, "导入文件")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Tab row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor   = Color.Transparent,
                contentColor     = colors.primary
            ) {
                Tab(selected = state.selectedTab == 0, onClick = { vm.setTab(0) },
                    text = { Text("已安装") })
                Tab(selected = state.selectedTab == 1, onClick = { vm.setTab(1) },
                    text = { Text("下载中心") })
            }

            // Download progress banner
            AnimatedVisibility(visible = state.downloadState !is DownloadState.Idle
                    && state.downloadState !is DownloadState.Success) {
                DownloadProgressBanner(state.downloadState)
            }

            when (state.selectedTab) {
                0 -> InstalledTab(state, vm, isDark)
                1 -> DownloadTab(state, vm)
            }
        }
    }
}

// ── Installed tab ─────────────────────────────────────────────────────────────

@Composable
private fun InstalledTab(state: LibraryUiState, vm: LibraryViewModel, isDark: Boolean) {
    val colors = MaterialTheme.colorScheme

    // Type filter chips
    val typeFilters = listOf(
        null to "全部",
        BookType.BIBLE_TEXT to "译本",
        BookType.ORIGINAL_TEXT to "原文",
        BookType.STUDY_BIBLE to "研读本",
        BookType.COMMENTARY to "注释",
    )

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(typeFilters) { (type, label) ->
                FilterChip(
                    selected = state.filterType == type,
                    onClick  = { vm.setFilter(type) },
                    label    = { Text(label) },
                    shape    = RoundedCornerShape(20.dp)
                )
            }
        }

        val filtered = if (state.filterType == null) state.installedBooks
                       else state.installedBooks.filter { it.type == state.filterType }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.MenuBook, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant)
                    Text("暂无已安装的书籍", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text("点击右上角 ↑ 导入 .slbook 文件", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Group by type
                BookType.values().forEach { type ->
                    val booksOfType = filtered.filter { it.type == type }
                    if (booksOfType.isEmpty()) return@forEach

                    item {
                        Text(
                            type.displayName(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(booksOfType) { book ->
                        InstalledBookCard(book,
                            onSetDefault = { vm.setDefault(book.bookId, book.type) },
                            onUninstall  = { vm.uninstall(book.bookId) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun InstalledBookCard(
    book: BookCatalogEntry,
    onSetDefault: () -> Unit,
    onUninstall: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    AuroraCard(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BookTypeIcon(book.type)
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (book.isDefault) {
                                Surface(shape = RoundedCornerShape(4.dp), color = colors.primaryContainer) {
                                    Text("默认", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall, color = colors.primary)
                                }
                            }
                        }
                        Text(
                            "${book.abbreviation} · ${book.language.displayName} · ${book.type.displayName()}",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, Modifier.size(18.dp), tint = colors.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (book.description.isNotBlank()) {
                        Text(book.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    if (book.copyright.isNotBlank()) {
                        Text("© ${book.copyright}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!book.isDefault) {
                            OutlinedButton(onClick = onSetDefault, shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                Text("设为默认", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (!book.isBuiltIn) {
                            OutlinedButton(onClick = onUninstall, shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.error.copy(0.5f))) {
                                Text("卸载", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Download tab ──────────────────────────────────────────────────────────────

@Composable
private fun DownloadTab(state: LibraryUiState, vm: LibraryViewModel) {
    var catalogUrl by remember { mutableStateOf("https://scrolllight.example.com/catalog.json") }
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize()) {
        // Catalog URL input
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = catalogUrl, onValueChange = { catalogUrl = it },
                modifier = Modifier.weight(1f),
                label = { Text("书目服务地址") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            FilledTonalButton(
                onClick = { vm.fetchRemoteCatalog(catalogUrl) },
                enabled = !state.isLoadingRemote,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoadingRemote)
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
            }
        }

        state.remoteError?.let { err ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp), color = colors.errorContainer.copy(0.5f)) {
                Text(err, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = colors.error)
            }
        }

        if (state.remoteBooks.isEmpty() && !state.isLoadingRemote) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CloudDownload, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant)
                    Text("点击刷新按钮获取可下载书目", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text("或修改书目服务地址", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
        } else {
            val installedIds = state.installedBooks.map { it.bookId }.toSet()
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.remoteBooks) { entry ->
                    RemoteBookCard(
                        entry       = entry,
                        isInstalled = entry.bookId in installedIds,
                        isDownloading = state.downloadState.let {
                            it is DownloadState.Downloading && it.bookId == entry.bookId ||
                            it is DownloadState.Installing  && it.bookId == entry.bookId
                        },
                        onDownload = {
                            val catalog = BookCatalogEntry(
                                bookId       = entry.bookId,
                                title        = entry.title,
                                abbreviation = entry.abbreviation,
                                type         = try { BookType.valueOf(entry.type.uppercase()) } catch (_: Exception) { BookType.BIBLE_TEXT },
                                language     = BookLanguage.values().firstOrNull { it.code == entry.language } ?: BookLanguage.EN,
                                downloadUrl  = entry.downloadUrl,
                                fileSizeKb   = entry.fileSizeKb,
                                checksum     = entry.checksum
                            )
                            vm.downloadBook(catalog)
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun RemoteBookCard(
    entry: RemoteBookEntry,
    isInstalled: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val type = try { BookType.valueOf(entry.type.uppercase()) } catch (_: Exception) { BookType.BIBLE_TEXT }

    AuroraCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BookTypeIcon(type)
                Column {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        "${entry.abbreviation} · ${entry.language} · ${formatSize(entry.fileSizeKb)}",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
                    )
                    if (entry.description.isNotBlank()) {
                        Text(entry.description, style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            when {
                isInstalled    -> Icon(Icons.Outlined.CheckCircle, "已安装", tint = colors.primary)
                isDownloading  -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else           -> IconButton(onClick = onDownload) {
                    Icon(Icons.Outlined.CloudDownload, "下载", tint = colors.primary)
                }
            }
        }
    }
}

// ── Progress banner ───────────────────────────────────────────────────────────

@Composable
private fun DownloadProgressBanner(state: DownloadState) {
    val colors = MaterialTheme.colorScheme
    val (progress, msg) = when (state) {
        is DownloadState.Downloading -> state.progress to state.message
        is DownloadState.Installing  -> state.progress to state.message
        is DownloadState.Failed      -> 0f to "失败：${state.error}"
        else -> 0f to ""
    }
    val isError = state is DownloadState.Failed

    Surface(
        Modifier.fillMaxWidth(),
        color = if (isError) colors.errorContainer.copy(0.6f) else colors.primaryContainer.copy(0.5f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(msg, style = MaterialTheme.typography.bodySmall)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            if (!isError) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color    = colors.primary, trackColor = colors.primaryContainer
                )
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun BookTypeIcon(type: BookType) {
    val (icon, color) = when (type) {
        BookType.BIBLE_TEXT    -> Icons.Outlined.Book to Color(0xFF7A9E7E)
        BookType.ORIGINAL_TEXT -> Icons.Outlined.Translate to Color(0xFF8FA3B1)
        BookType.STUDY_BIBLE   -> Icons.Outlined.School to Color(0xFFB08C88)
        BookType.COMMENTARY    -> Icons.Outlined.Article to Color(0xFFA89070)
        BookType.LEXICON       -> Icons.Outlined.MenuBook to Color(0xFF9B8EC4)
    }
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.15f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
}

private fun BookType.displayName() = when (this) {
    BookType.BIBLE_TEXT    -> "译本"
    BookType.ORIGINAL_TEXT -> "原文"
    BookType.STUDY_BIBLE   -> "研读本"
    BookType.COMMENTARY    -> "注释书"
    BookType.LEXICON       -> "词典"
}

private fun formatSize(kb: Long) = when {
    kb <= 0   -> ""
    kb < 1024 -> "${kb}KB"
    else      -> "${"%.1f".format(kb / 1024.0)}MB"
}
