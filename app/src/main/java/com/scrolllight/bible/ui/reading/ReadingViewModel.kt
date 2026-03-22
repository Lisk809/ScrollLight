package com.scrolllight.bible.ui.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.ai.*
import com.scrolllight.bible.data.library.*
import com.scrolllight.bible.data.model.*
import com.scrolllight.bible.data.repository.BibleRepository
import com.scrolllight.bible.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingUiState(
    val book: BibleBook? = null,
    val chapter: Int = 1,
    // Primary verses (from BibleRepository for built-in, or LibraryRepository for installed)
    val verses: List<BibleVerse> = emptyList(),
    val versesLibrary: List<BibleVerseEntity> = emptyList(),
    // Parallel reading
    val parallelVerses: List<BibleVerseEntity> = emptyList(),
    val showParallel: Boolean = false,
    val secondaryVersionId: String? = null,
    // Original text
    val originalWords: Map<Int, List<OriginalWordEntity>> = emptyMap(),
    val showOriginal: Boolean = false,
    val originalVersionId: String? = null,
    // Study notes
    val studyNotes: Map<Int, List<StudyNoteEntity>> = emptyMap(),
    val showStudyPanel: Boolean = false,
    val studyVersionId: String? = null,
    // User annotations
    val highlights: Map<Int, HighlightColor> = emptyMap(),
    val selectedVerse: Int? = null,
    // Layout
    val layout: ReadingLayout = ReadingLayout.SINGLE,
    val fontSize: Float = 18f,
    val isLoading: Boolean = true,
    // AI
    val aiHighlights: Map<Int, String> = emptyMap(),
    val aiAnnotations: Map<Int, String> = emptyMap(),
    val scrollToVerse: Int? = null,
    // Available installed versions
    val availableBibleVersions: List<BookCatalogEntry> = emptyList(),
    val primaryVersionId: String = "cuv",
    // Strongs word popup
    val selectedWord: OriginalWordEntity? = null,
    val strongsEntry: StrongsEntry? = null
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val bibleRepo: BibleRepository,
    private val libraryRepo: LibraryRepository,
    private val userRepo: UserRepository,
    private val linkedConfigRepo: com.scrolllight.bible.data.library.LinkedReadingConfigRepository,
    val aiController: AiController
) : ViewModel() {

    val allBooks: List<BibleBook> get() = bibleRepo.getAllBooks()

    private val _state = MutableStateFlow(ReadingUiState())
    val state: StateFlow<ReadingUiState> = _state.asStateFlow()

    /** 三书联动配置（用于 LinkedBooksPanel） */
    val linkedConfig = linkedConfigRepo.config

    fun applyLinkedConfig(config: com.scrolllight.bible.data.library.LinkedReadingConfig) {
        viewModelScope.launch {
            linkedConfigRepo.save(config)
            _state.update { s -> s.copy(
                primaryVersionId   = config.bibleVersionId,
                secondaryVersionId = config.parallelId,
                originalVersionId  = config.originalId,
                studyVersionId     = config.studyId,
                showParallel       = config.parallelId != null,
                showOriginal       = config.originalId != null,
                showStudyPanel     = config.studyId    != null,
                layout             = config.layout
            )}
            // Reload with new config
            val s = _state.value
            s.book?.let { loadChapter(it.id, s.chapter) }
        }
    }

    fun loadLinkedConfig() {
        viewModelScope.launch {
            linkedConfigRepo.config.collect { cfg ->
                _state.update { s -> s.copy(
                    primaryVersionId   = cfg.bibleVersionId,
                    secondaryVersionId = cfg.parallelId,
                    originalVersionId  = cfg.originalId,
                    studyVersionId     = cfg.studyId,
                    showParallel       = cfg.parallelId != null,
                    showOriginal       = cfg.originalId != null,
                    showStudyPanel     = cfg.studyId    != null,
                    layout             = cfg.layout
                )}
            }
        }
    }

    fun loadChapter(bookId: String, chapter: Int) {
        val book = bibleRepo.getBook(bookId) ?: return
        _state.update { it.copy(isLoading = true, book = book, chapter = chapter) }
        viewModelScope.launch {
            val primaryId = _state.value.primaryVersionId
            // Try library first, fall back to built-in
            val libVerses = libraryRepo.getChapter(bookId, chapter, primaryId)
            val legacyVerses: List<BibleVerse> = if (libVerses.isEmpty()) bibleRepo.getChapter(bookId, chapter) else emptyList()

            val secondaryId = _state.value.secondaryVersionId
            val parallelVerses = if (secondaryId != null && _state.value.showParallel)
                libraryRepo.getChapter(bookId, chapter, secondaryId) else emptyList()

            val origId = _state.value.originalVersionId
            val origWords = if (origId != null && _state.value.showOriginal)
                libraryRepo.getOriginalWords(bookId, chapter, origId).groupBy { it.verse }
            else emptyMap()

            val studyId = _state.value.studyVersionId
            val studyNotes = if (studyId != null)
                libraryRepo.getStudyNotes(studyId, bookId, chapter).groupBy { it.verseFrom }
            else emptyMap()

            _state.update { s -> s.copy(
                verses         = legacyVerses,
                versesLibrary  = libVerses,
                parallelVerses = parallelVerses,
                originalWords  = origWords,
                studyNotes     = studyNotes,
                isLoading      = false
            )}
            userRepo.getHighlightsForChapter(bookId, chapter).collect { hl ->
                _state.update { it.copy(highlights = hl.associate { h -> h.verse to h.color }) }
            }
        }
        viewModelScope.launch {
            libraryRepo.installedBooks.collect { books ->
                _state.update { it.copy(availableBibleVersions = books.filter { b ->
                    b.type == BookType.BIBLE_TEXT || b.type == BookType.STUDY_BIBLE
                })}
            }
        }
    }

    fun setLayout(layout: ReadingLayout) {
        _state.update { it.copy(layout = layout) }
        val s = _state.value
        if (s.book != null) reloadSecondary(s.book.id, s.chapter)
    }

    fun toggleParallel(versionId: String? = null) {
        val show = !_state.value.showParallel
        _state.update { it.copy(showParallel = show,
            secondaryVersionId = if (show && versionId != null) versionId else it.secondaryVersionId,
            layout = if (show) ReadingLayout.PARALLEL else ReadingLayout.SINGLE) }
        val s = _state.value; if (s.book != null) reloadSecondary(s.book.id, s.chapter)
    }

    fun toggleOriginal(versionId: String? = null) {
        val show = !_state.value.showOriginal
        _state.update { it.copy(showOriginal = show,
            originalVersionId = if (show && versionId != null) versionId else it.originalVersionId,
            layout = if (show) ReadingLayout.ORIGINAL else ReadingLayout.SINGLE) }
        val s = _state.value; if (s.book != null) reloadSecondary(s.book.id, s.chapter)
    }

    fun toggleStudyPanel(versionId: String? = null) {
        val show = !_state.value.showStudyPanel
        _state.update { it.copy(showStudyPanel = show,
            studyVersionId = if (show && versionId != null) versionId else it.studyVersionId,
            layout = if (show) ReadingLayout.STUDY else ReadingLayout.SINGLE) }
        val s = _state.value; if (s.book != null) reloadSecondary(s.book.id, s.chapter)
    }

    fun setCardMode(enabled: Boolean) = _state.update {
        it.copy(layout = if (enabled) ReadingLayout.CARD else ReadingLayout.SINGLE)
    }

    private fun reloadSecondary(bookId: String, chapter: Int) {
        viewModelScope.launch {
            val s = _state.value
            val parallel = if (s.showParallel && s.secondaryVersionId != null)
                libraryRepo.getChapter(bookId, chapter, s.secondaryVersionId) else emptyList()
            val origWords = if (s.showOriginal && s.originalVersionId != null)
                libraryRepo.getOriginalWords(bookId, chapter, s.originalVersionId).groupBy { it.verse }
            else emptyMap()
            val studyNotes = if (s.studyVersionId != null)
                libraryRepo.getStudyNotes(s.studyVersionId, bookId, chapter).groupBy { it.verseFrom }
            else emptyMap()
            _state.update { it.copy(parallelVerses = parallel, originalWords = origWords, studyNotes = studyNotes) }
        }
    }

    fun onWordTap(word: OriginalWordEntity) {
        _state.update { it.copy(selectedWord = word, strongsEntry = null) }
        if (word.strongs.isNotBlank()) {
            viewModelScope.launch {
                val entry = libraryRepo.lookupStrongs(word.strongs)
                _state.update { it.copy(strongsEntry = entry) }
            }
        }
    }

    fun dismissWordPopup() = _state.update { it.copy(selectedWord = null, strongsEntry = null) }
    fun selectVerse(verse: Int?) = _state.update { it.copy(selectedVerse = verse) }
    fun toggleParallel() = toggleParallel(null)
    fun setFontSize(size: Float) = _state.update { it.copy(fontSize = size) }
    fun clearScrollTarget() = _state.update { it.copy(scrollToVerse = null) }

    fun toggleHighlight(verse: Int, color: HighlightColor = HighlightColor.YELLOW) {
        val book = _state.value.book ?: return
        viewModelScope.launch { userRepo.toggleHighlight(book.id, book.name, _state.value.chapter, verse, color) }
    }

    fun navigateChapter(delta: Int) {
        val book = _state.value.book ?: return
        val next = (_state.value.chapter + delta).coerceIn(1, book.chapterCount)
        if (next != _state.value.chapter) loadChapter(book.id, next)
    }

    fun observeAiCommands() {
        viewModelScope.launch {
            aiController.aiCommand.collect { cmd ->
                when (cmd) {
                    is AiCommand.HighlightVerses -> { _state.update { it.copy(aiHighlights = it.aiHighlights + cmd.verses.associateWith { cmd.color }) }; aiController.resetIdle() }
                    is AiCommand.ShowAnnotation  -> { _state.update { it.copy(aiAnnotations = it.aiAnnotations + (cmd.verse to cmd.text)) }; aiController.resetIdle() }
                    is AiCommand.ScrollToVerse   -> { _state.update { it.copy(scrollToVerse = cmd.verse) }; aiController.resetIdle() }
                    is AiCommand.NavigateToChapter -> { loadChapter(cmd.bookId, cmd.chapter); aiController.resetIdle() }
                    is AiCommand.ClearOverlay    -> { _state.update { it.copy(aiHighlights = emptyMap(), aiAnnotations = emptyMap()) }; aiController.resetIdle() }
                    else -> {}
                }
            }
        }
    }
}
