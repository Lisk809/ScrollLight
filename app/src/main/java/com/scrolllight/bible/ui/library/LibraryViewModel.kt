package com.scrolllight.bible.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.data.library.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val installedBooks: List<BookCatalogEntry>    = emptyList(),
    val remoteBooks:    List<RemoteBookEntry>      = emptyList(),
    val downloadState:  DownloadState             = DownloadState.Idle,
    val selectedTab:    Int                       = 0,       // 0=我的书库 1=下载中心
    val filterType:     BookType?                  = null,
    val isLoadingRemote: Boolean                   = false,
    val remoteError:    String?                    = null,
    val defaultBibleId: String                    = "cuv",
    val defaultOrigId:  String?                   = null,
    val defaultStudyId: String?                   = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    // 默认书目服务地址（可由用户配置）
    private var catalogUrl = "https://scrolllight.example.com/catalog.json"

    init {
        viewModelScope.launch {
            repo.installedBooks.collect { books ->
                _state.update { it.copy(installedBooks = books) }
            }
        }
        viewModelScope.launch {
            repo.downloadState.collect { ds ->
                _state.update { it.copy(downloadState = ds) }
                // Auto-refresh after success
                if (ds is DownloadState.Success) {
                    kotlinx.coroutines.delay(1500)
                    repo.resetDownloadState()
                }
            }
        }
    }

    fun setTab(tab: Int) = _state.update { it.copy(selectedTab = tab) }
    fun setFilter(type: BookType?) = _state.update { it.copy(filterType = type) }

    fun fetchRemoteCatalog(url: String = catalogUrl) {
        catalogUrl = url
        _state.update { it.copy(isLoadingRemote = true, remoteError = null) }
        viewModelScope.launch {
            repo.fetchRemoteCatalog(url).fold(
                onSuccess = { entries ->
                    _state.update { it.copy(remoteBooks = entries, isLoadingRemote = false) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoadingRemote = false, remoteError = err.message) }
                }
            )
        }
    }

    fun downloadBook(entry: BookCatalogEntry) {
        viewModelScope.launch { repo.downloadAndInstall(entry) }
    }

    fun importFromUri(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            val tempFile = java.io.File(context.cacheDir, "import_${System.currentTimeMillis()}.slbook")
            context.contentResolver.openInputStream(uri)?.use { inp ->
                tempFile.outputStream().use { out -> inp.copyTo(out) }
            }
            repo.installFromFile(tempFile)
            tempFile.delete()
        }
    }

    fun uninstall(bookId: String) {
        viewModelScope.launch { repo.uninstall(bookId) }
    }

    fun setDefault(bookId: String, type: BookType) {
        viewModelScope.launch {
            repo.setDefault(bookId, type)
            when (type) {
                BookType.BIBLE_TEXT    -> _state.update { it.copy(defaultBibleId = bookId) }
                BookType.ORIGINAL_TEXT -> _state.update { it.copy(defaultOrigId = bookId) }
                BookType.STUDY_BIBLE   -> _state.update { it.copy(defaultStudyId = bookId) }
                else -> {}
            }
        }
    }
}
