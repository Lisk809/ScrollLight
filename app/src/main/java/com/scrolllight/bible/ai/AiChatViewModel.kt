package com.scrolllight.bible.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI message model ──────────────────────────────────────────────────────────

sealed class ChatBubble {
    abstract val id: String

    data class User(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val text: String
    ) : ChatBubble()

    data class Assistant(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val text: String,
        val isStreaming: Boolean = false
    ) : ChatBubble()

    data class ToolExecution(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val toolName: String,
        val displayName: String,
        val params: String,
        val result: String? = null,
        val isRunning: Boolean = true,
        val isError: Boolean = false
    ) : ChatBubble()

    data class Error(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val message: String
    ) : ChatBubble()
}

fun String.toolDisplayName(): String = when (this) {
    ToolDefinitions.NAVIGATE_TO_CHAPTER   -> "📖 导航章节"
    ToolDefinitions.HIGHLIGHT_VERSES      -> "🖊 高亮经文"
    ToolDefinitions.SEARCH_SCRIPTURE      -> "🔍 搜索经文"
    ToolDefinitions.SCROLL_TO_VERSE       -> "⬇ 滚动到节"
    ToolDefinitions.SHOW_ANNOTATION       -> "💬 添加注解"
    ToolDefinitions.SHOW_CROSS_REFERENCES -> "🔗 交叉参考"
    ToolDefinitions.CLEAR_OVERLAY         -> "🧹 清除标注"
    ToolDefinitions.GET_CURRENT_READING   -> "📍 获取位置"
    else -> this
}

// ── State ─────────────────────────────────────────────────────────────────────

data class AiChatUiState(
    val bubbles: List<ChatBubble> = emptyList(),
    val isLoading: Boolean = false,
    val config: AiConfig = AiConfig(),
    val panelVisible: Boolean = false,
    val inputText: String = ""
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val apiClient: AiApiClient,
    private val toolExecutor: ToolExecutor,
    private val configRepo: AiConfigRepository,
    val aiController: AiController
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    // The actual messages array sent to API (includes system + all turns)
    private val messageHistory = JsonArray()

    // Track current reading context (updated from ReadingViewModel)
    private var readingContext = AiReadingContext("", "", 1)

    init {
        viewModelScope.launch {
            configRepo.config.collect { cfg ->
                _state.update { it.copy(config = cfg) }
            }
        }
    }

    fun updateReadingContext(ctx: AiReadingContext) {
        readingContext = ctx
    }

    fun setInputText(text: String) = _state.update { it.copy(inputText = text) }

    fun togglePanel() = _state.update { it.copy(panelVisible = !it.panelVisible, inputText = "") }
    fun showPanel()   = _state.update { it.copy(panelVisible = true) }
    fun hidePanel()   = _state.update { it.copy(panelVisible = false) }

    fun clearHistory() {
        messageHistory.let { arr ->
            while (arr.size() > 0) arr.remove(0)
        }
        _state.update { it.copy(bubbles = emptyList()) }
    }

    // ── Send message ──────────────────────────────────────────────────────

    fun send(userText: String) {
        if (userText.isBlank()) return
        val cfg = _state.value.config
        if (!cfg.isConfigured) {
            appendBubble(ChatBubble.Error(message = "请先在设置中配置 API Key 和 Base URL。"))
            return
        }

        _state.update { it.copy(inputText = "", isLoading = true) }

        // Initialize history with system message on first turn
        if (messageHistory.size() == 0) {
            messageHistory.add(MessageBuilder.system(cfg.systemPrompt))
        }

        // Append user message
        val userBubble = ChatBubble.User(text = userText)
        appendBubble(userBubble)
        messageHistory.add(MessageBuilder.user(userText))

        viewModelScope.launch {
            doConversationTurn(cfg)
        }
    }

    // ── Conversation loop (handles tool call → result → continue) ─────────

    private suspend fun doConversationTurn(cfg: AiConfig) {
        var assistantBubbleId = java.util.UUID.randomUUID().toString()
        var accumulatedText   = StringBuilder()
        var pendingToolCalls  = listOf<ToolCall>()

        // Add streaming assistant bubble placeholder
        appendBubble(ChatBubble.Assistant(id = assistantBubbleId, text = "", isStreaming = true))

        apiClient.send(cfg, messageHistory).collect { chunk ->
            when (chunk) {
                is StreamChunk.Token -> {
                    accumulatedText.append(chunk.text)
                    updateBubble(assistantBubbleId, ChatBubble.Assistant(
                        id          = assistantBubbleId,
                        text        = accumulatedText.toString(),
                        isStreaming = true
                    ))
                }

                is StreamChunk.ToolCallComplete -> {
                    pendingToolCalls = chunk.calls
                    // Finalize assistant text bubble if any
                    val text = accumulatedText.toString()
                    if (text.isNotBlank()) {
                        updateBubble(assistantBubbleId, ChatBubble.Assistant(
                            id = assistantBubbleId, text = text, isStreaming = false
                        ))
                    } else {
                        removeBubble(assistantBubbleId)
                    }
                }

                is StreamChunk.Done -> {
                    if (pendingToolCalls.isEmpty()) {
                        // Final text response — stop streaming indicator
                        updateBubble(assistantBubbleId, ChatBubble.Assistant(
                            id          = assistantBubbleId,
                            text        = accumulatedText.toString(),
                            isStreaming = false
                        ))
                    }
                    _state.update { it.copy(isLoading = false) }
                }

                is StreamChunk.Error -> {
                    removeBubble(assistantBubbleId)
                    appendBubble(ChatBubble.Error(message = chunk.message))
                    _state.update { it.copy(isLoading = false) }
                }

                else -> {}
            }
        }

        // ── Execute tool calls and continue conversation ───────────────────
        if (pendingToolCalls.isNotEmpty()) {
            // Record assistant message with tool calls in history
            messageHistory.add(MessageBuilder.assistant(
                content   = accumulatedText.takeIf { it.isNotBlank() }?.toString(),
                toolCalls = pendingToolCalls
            ))

            val toolResults = mutableListOf<ToolResult>()

            for (call in pendingToolCalls) {
                val toolBubbleId = java.util.UUID.randomUUID().toString()
                // Show tool execution bubble (running)
                appendBubble(ChatBubble.ToolExecution(
                    id          = toolBubbleId,
                    toolName    = call.name,
                    displayName = call.name.toolDisplayName(),
                    params      = call.arguments,
                    isRunning   = true
                ))

                val result = toolExecutor.execute(call, readingContext)
                toolResults.add(result)

                // Update bubble with result
                updateBubble(toolBubbleId, ChatBubble.ToolExecution(
                    id          = toolBubbleId,
                    toolName    = call.name,
                    displayName = call.name.toolDisplayName(),
                    params      = call.arguments,
                    result      = result.result,
                    isRunning   = false,
                    isError     = result.isError
                ))

                // Append tool result to history
                messageHistory.add(MessageBuilder.toolResult(result))
            }

            // Continue conversation after tool results
            _state.update { it.copy(isLoading = true) }
            assistantBubbleId  = java.util.UUID.randomUUID().toString()
            accumulatedText    = StringBuilder()
            appendBubble(ChatBubble.Assistant(id = assistantBubbleId, text = "", isStreaming = true))

            apiClient.send(cfg, messageHistory).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Token -> {
                        accumulatedText.append(chunk.text)
                        updateBubble(assistantBubbleId, ChatBubble.Assistant(
                            id = assistantBubbleId, text = accumulatedText.toString(), isStreaming = true
                        ))
                    }
                    is StreamChunk.Done -> {
                        updateBubble(assistantBubbleId, ChatBubble.Assistant(
                            id = assistantBubbleId, text = accumulatedText.toString(), isStreaming = false
                        ))
                        _state.update { it.copy(isLoading = false) }
                        // Record final assistant reply in history
                        messageHistory.add(MessageBuilder.assistant(accumulatedText.toString()))
                    }
                    is StreamChunk.Error -> {
                        removeBubble(assistantBubbleId)
                        appendBubble(ChatBubble.Error(message = chunk.message))
                        _state.update { it.copy(isLoading = false) }
                    }
                    else -> {}
                }
            }
        } else {
            // Record in history
            if (accumulatedText.isNotBlank()) {
                messageHistory.add(MessageBuilder.assistant(accumulatedText.toString()))
            }
        }
    }

    // ── Bubble list helpers ───────────────────────────────────────────────

    private fun appendBubble(bubble: ChatBubble) =
        _state.update { it.copy(bubbles = it.bubbles + bubble) }

    private fun updateBubble(id: String, new: ChatBubble) =
        _state.update { s -> s.copy(bubbles = s.bubbles.map { if (it.id == id) new else it }) }

    private fun removeBubble(id: String) =
        _state.update { s -> s.copy(bubbles = s.bubbles.filter { it.id != id }) }
}
