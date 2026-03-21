package com.scrolllight.bible.ui.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.ai.AiCommand
import com.scrolllight.bible.ai.AiController
import com.scrolllight.bible.ai.AiMessage
import com.scrolllight.bible.ai.AiReadingContext
import com.scrolllight.bible.ai.AiService
import com.scrolllight.bible.ai.StubAiService
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
    val verses: List<BibleVerse> = emptyList(),
    val highlights: Map<Int, HighlightColor> = emptyMap(),
    val notes: Set<Int> = emptySet(),
    val bookmarks: Set<Int> = emptySet(),
    val selectedVerse: Int? = null,
    val showParallel: Boolean = false,
    val fontSize: Float = 18f,
    val isLoading: Boolean = true,
    // AI
    val aiHighlights: Map<Int, String> = emptyMap(),     // verse → color
    val aiAnnotations: Map<Int, String> = emptyMap(),    // verse → annotation text
    val aiPanelOpen: Boolean = false,
    val aiMessages: List<AiMessage> = emptyList(),
    val scrollToVerse: Int? = null,
    val crossRefs: List<String> = emptyList()
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val bibleRepo: BibleRepository,
    private val userRepo: UserRepository,
    val aiController: AiController
) : ViewModel() {

    val allBooks: List<BibleBook> get() = bibleRepo.getAllBooks()

    private val _state = MutableStateFlow(ReadingUiState())
    val state: StateFlow<ReadingUiState> = _state

    private val aiService: AiService = StubAiService()

    // ── Load chapter ──────────────────────────────────────────────────────

    fun loadChapter(bookId: String, chapter: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val book   = bibleRepo.getBook(bookId)
            val verses = bibleRepo.getChapter(bookId, chapter)
            _state.update { it.copy(book = book, chapter = chapter, verses = verses, isLoading = false) }

            // Load user highlights for chapter
            userRepo.getHighlightsForChapter(bookId, chapter).collect { hl ->
                val map = hl.associate { it.verse to it.color }
                _state.update { it.copy(highlights = map) }
            }
        }
    }

    // ── User interactions ─────────────────────────────────────────────────

    fun selectVerse(verse: Int?) = _state.update { it.copy(selectedVerse = verse) }

    fun toggleHighlight(verse: Int, color: HighlightColor = HighlightColor.YELLOW) {
        val book = _state.value.book ?: return
        viewModelScope.launch {
            userRepo.toggleHighlight(book.id, book.name, _state.value.chapter, verse, color)
        }
    }

    fun toggleParallel() = _state.update { it.copy(showParallel = !it.showParallel) }

    fun setFontSize(size: Float) = _state.update { it.copy(fontSize = size) }

    fun navigateChapter(delta: Int) {
        val book = _state.value.book ?: return
        val next = (_state.value.chapter + delta).coerceIn(1, book.chapterCount)
        if (next != _state.value.chapter) loadChapter(book.id, next)
    }

    // ── AI ────────────────────────────────────────────────────────────────

    fun sendAiMessage(text: String) {
        val state = _state.value
        val ctx = AiReadingContext(
            bookId   = state.book?.id ?: "",
            bookName = state.book?.name ?: "",
            chapter  = state.chapter,
            selectedVerse = state.selectedVerse
        )
        val userMsg = AiMessage(AiMessage.Role.USER, text)
        val msgs = state.aiMessages + userMsg
        _state.update { it.copy(aiMessages = msgs) }

        viewModelScope.launch {
            val reply = aiService.chat(msgs, ctx, aiController)
            _state.update { it.copy(aiMessages = it.aiMessages + reply) }
        }
    }

    fun observeAiCommands() {
        viewModelScope.launch {
            aiController.aiCommand.collect { cmd ->
                when (cmd) {
                    is AiCommand.HighlightVerses -> {
                        val map = cmd.verses.associateWith { cmd.color }
                        _state.update { it.copy(aiHighlights = it.aiHighlights + map) }
                        aiController.resetIdle()
                    }
                    is AiCommand.ShowAnnotation -> {
                        _state.update { it.copy(aiAnnotations = it.aiAnnotations + (cmd.verse to cmd.text)) }
                        aiController.resetIdle()
                    }
                    is AiCommand.ToggleAiPanel -> {
                        _state.update { it.copy(aiPanelOpen = cmd.show) }
                        aiController.resetIdle()
                    }
                    is AiCommand.ScrollToVerse -> {
                        _state.update { it.copy(scrollToVerse = cmd.verse) }
                        aiController.resetIdle()
                    }
                    is AiCommand.ShowCrossReferences -> {
                        _state.update { it.copy(crossRefs = cmd.refs) }
                        aiController.resetIdle()
                    }
                    is AiCommand.ClearOverlay -> {
                        _state.update { it.copy(aiHighlights = emptyMap(), aiAnnotations = emptyMap(), crossRefs = emptyList()) }
                        aiController.resetIdle()
                    }
                    is AiCommand.NavigateToChapter -> {
                        loadChapter(cmd.bookId, cmd.chapter)
                        aiController.resetIdle()
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearScrollTarget() = _state.update { it.copy(scrollToVerse = null) }
}
