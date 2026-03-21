package com.scrolllight.bible.ai

import com.scrolllight.bible.data.model.SearchScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      AI INTEGRATION LAYER                           ║
 * ║                                                                      ║
 * ║  This singleton is the single entry-point for any AI model that      ║
 * ║  wants to drive the UI. Inject it into your AI ViewModel/Service     ║
 * ║  and call the methods below.                                         ║
 * ║                                                                      ║
 * ║  The UI observes [aiCommand] as a StateFlow.  When a new command     ║
 * ║  is emitted the relevant Screen's ViewModel reacts to it.            ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Singleton
class AiController @Inject constructor() {

    // ── Public command bus ────────────────────────────────────────────────

    private val _aiCommand = MutableStateFlow<AiCommand>(AiCommand.Idle)
    val aiCommand: StateFlow<AiCommand> = _aiCommand.asStateFlow()

    // ── AI → UI commands ──────────────────────────────────────────────────

    /** Navigate to a specific book + chapter. */
    fun navigateToChapter(bookId: String, chapter: Int) {
        emit(AiCommand.NavigateToChapter(bookId, chapter))
    }

    /** Highlight specific verses (by verse numbers) in the current chapter. */
    fun highlightVerses(verseNumbers: List<Int>, color: String = "YELLOW") {
        emit(AiCommand.HighlightVerses(verseNumbers, color))
    }

    /** Open the search screen with a pre-filled query. */
    fun triggerSearch(query: String, scope: SearchScope = SearchScope.WHOLE_BIBLE) {
        emit(AiCommand.TriggerSearch(query, scope))
    }

    /** Show an AI-generated annotation balloon above a verse. */
    fun showAnnotation(verse: Int, text: String) {
        emit(AiCommand.ShowAnnotation(verse, text))
    }

    /** Show a cross-reference panel listing related passages. */
    fun showCrossReferences(refs: List<String>) {
        emit(AiCommand.ShowCrossReferences(refs))
    }

    /** Open/close the AI chat panel overlay. */
    fun toggleAiPanel(show: Boolean) {
        emit(AiCommand.ToggleAiPanel(show))
    }

    /** Scroll reading view to a specific verse. */
    fun scrollToVerse(verse: Int) {
        emit(AiCommand.ScrollToVerse(verse))
    }

    /** Ask the UI to read aloud the given verse range. */
    fun readAloud(verseStart: Int, verseEnd: Int) {
        emit(AiCommand.ReadAloud(verseStart, verseEnd))
    }

    /** Clear all AI-applied highlights / annotations. */
    fun clearAiOverlay() {
        emit(AiCommand.ClearOverlay)
    }

    fun resetIdle() {
        _aiCommand.value = AiCommand.Idle
    }

    private fun emit(cmd: AiCommand) {
        _aiCommand.value = cmd
    }
}

// ── Command ADT ───────────────────────────────────────────────────────────────

sealed class AiCommand {
    object Idle : AiCommand()
    object ClearOverlay : AiCommand()
    data class NavigateToChapter(val bookId: String, val chapter: Int) : AiCommand()
    data class HighlightVerses(val verses: List<Int>, val color: String) : AiCommand()
    data class TriggerSearch(val query: String, val scope: SearchScope) : AiCommand()
    data class ShowAnnotation(val verse: Int, val text: String) : AiCommand()
    data class ShowCrossReferences(val refs: List<String>) : AiCommand()
    data class ToggleAiPanel(val show: Boolean) : AiCommand()
    data class ScrollToVerse(val verse: Int) : AiCommand()
    data class ReadAloud(val verseStart: Int, val verseEnd: Int) : AiCommand()
}

// ── AI Chat message model ─────────────────────────────────────────────────────

data class AiMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}

/**
 * Interface to implement when connecting a real AI backend.
 * Replace [StubAiService] with your real implementation and bind it in Hilt.
 */
interface AiService {
    /**
     * Send a message to the AI. The AI may respond with text AND issue
     * side-effects via [AiController].
     */
    suspend fun chat(
        messages: List<AiMessage>,
        context: AiReadingContext,
        controller: AiController
    ): AiMessage
}

/** Context about what the user is currently reading — passed to the AI. */
data class AiReadingContext(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val version: String = "CUV",
    val selectedVerse: Int? = null,
    val visibleVerses: IntRange? = null
)

/** Stub implementation — replace with real Anthropic / OpenAI call. */
class StubAiService @Inject constructor() : AiService {
    override suspend fun chat(
        messages: List<AiMessage>,
        context: AiReadingContext,
        controller: AiController
    ): AiMessage {
        val userMsg = messages.lastOrNull { it.role == AiMessage.Role.USER }?.content ?: ""
        // Demo: if user asks about a verse number, scroll to it
        val verseMatch = Regex("第(\\d+)节").find(userMsg)
        if (verseMatch != null) {
            controller.scrollToVerse(verseMatch.groupValues[1].toInt())
            controller.highlightVerses(listOf(verseMatch.groupValues[1].toInt()), "YELLOW")
        }
        return AiMessage(
            role = AiMessage.Role.ASSISTANT,
            content = "您正在阅读${context.bookName}第${context.chapter}章。" +
                "这是AI助读的占位回复，请在 AiService 接口中接入真实的大语言模型（如 Claude / GPT）。"
        )
    }
}
