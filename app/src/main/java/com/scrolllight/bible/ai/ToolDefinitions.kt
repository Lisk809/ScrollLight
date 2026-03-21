package com.scrolllight.bible.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * 所有可供 AI 调用的工具定义（OpenAI tool calling 格式）
 * AI 可通过这些工具直接操控阅读界面
 */
object ToolDefinitions {

    // ── Tool names (must match executor map keys) ─────────────────────────────

    const val NAVIGATE_TO_CHAPTER   = "navigate_to_chapter"
    const val HIGHLIGHT_VERSES      = "highlight_verses"
    const val SEARCH_SCRIPTURE      = "search_scripture"
    const val SCROLL_TO_VERSE       = "scroll_to_verse"
    const val SHOW_ANNOTATION       = "show_annotation"
    const val SHOW_CROSS_REFERENCES = "show_cross_references"
    const val CLEAR_OVERLAY         = "clear_overlay"
    const val GET_CURRENT_READING   = "get_current_reading"

    // ── Schema builder helper ─────────────────────────────────────────────────

    private fun tool(name: String, description: String, properties: JsonObject, required: List<String>): JsonObject {
        val params = JsonObject().apply {
            addProperty("type", "object")
            add("properties", properties)
            add("required", JsonArray().also { arr -> required.forEach { arr.add(it) } })
        }
        return JsonObject().apply {
            addProperty("type", "function")
            add("function", JsonObject().apply {
                addProperty("name", name)
                addProperty("description", description)
                add("parameters", params)
            })
        }
    }

    private fun prop(type: String, description: String, enum: List<String>? = null): JsonObject =
        JsonObject().apply {
            addProperty("type", type)
            addProperty("description", description)
            if (enum != null) add("enum", JsonArray().also { a -> enum.forEach { a.add(it) } })
        }

    private fun arrayProp(itemType: String, description: String): JsonObject =
        JsonObject().apply {
            addProperty("type", "array")
            addProperty("description", description)
            add("items", JsonObject().apply { addProperty("type", itemType) })
        }

    // ── Tool list (send to API) ───────────────────────────────────────────────

    val all: JsonArray by lazy {
        JsonArray().apply {
            add(navigateToChapter)
            add(highlightVerses)
            add(searchScripture)
            add(scrollToVerse)
            add(showAnnotation)
            add(showCrossReferences)
            add(clearOverlay)
            add(getCurrentReading)
        }
    }

    private val navigateToChapter = tool(
        name = NAVIGATE_TO_CHAPTER,
        description = "导航到指定的圣经书卷和章节。当需要引导用户阅读特定经文时调用。",
        properties = JsonObject().apply {
            add("book_id", prop("string", "书卷ID，如 'mat'=马太福音, 'jhn'=约翰福音, 'rom'=罗马书, 'gen'=创世记 等"))
            add("chapter",  prop("integer", "章节数字，从1开始"))
            add("book_name", prop("string", "书卷中文名称，如'马太福音'"))
        },
        required = listOf("book_id", "chapter")
    )

    private val highlightVerses = tool(
        name = HIGHLIGHT_VERSES,
        description = "在当前章节中高亮指定的经节。引用或解释具体经节时调用，帮助用户定位。",
        properties = JsonObject().apply {
            add("verses", arrayProp("integer", "要高亮的节次列表，如 [3, 5, 7]"))
            add("color",  prop("string", "高亮颜色",
                listOf("YELLOW", "GREEN", "BLUE", "PINK", "ORANGE", "PURPLE")))
            add("reason", prop("string", "高亮原因（可选，显示在工具执行卡片中）"))
        },
        required = listOf("verses")
    )

    private val searchScripture = tool(
        name = SEARCH_SCRIPTURE,
        description = "在圣经中搜索特定关键词或短语，并返回结果。当用户询问某个主题或词汇在圣经中的出处时调用。",
        properties = JsonObject().apply {
            add("query", prop("string", "搜索关键词，如'爱'、'信心'、'以利亚'"))
            add("scope", prop("string", "搜索范围",
                listOf("WHOLE_BIBLE", "OLD_TESTAMENT", "NEW_TESTAMENT", "GOSPELS")))
        },
        required = listOf("query")
    )

    private val scrollToVerse = tool(
        name = SCROLL_TO_VERSE,
        description = "将阅读视图滚动到当前章节的指定节次。",
        properties = JsonObject().apply {
            add("verse", prop("integer", "要定位的节次"))
        },
        required = listOf("verse")
    )

    private val showAnnotation = tool(
        name = SHOW_ANNOTATION,
        description = "在指定节次旁边显示一个注解气泡，展示简短的解释文字。",
        properties = JsonObject().apply {
            add("verse",      prop("integer", "要注解的节次"))
            add("annotation", prop("string",  "注解内容，保持简短（≤60字）"))
        },
        required = listOf("verse", "annotation")
    )

    private val showCrossReferences = tool(
        name = SHOW_CROSS_REFERENCES,
        description = "显示交叉参考面板，列出相关的经文引用。",
        properties = JsonObject().apply {
            add("references", arrayProp("string", "交叉参考经文列表，格式如 ['约 3:16', '罗 5:8', '诗 23:1']"))
            add("title",      prop("string", "面板标题，如'相关经文'"))
        },
        required = listOf("references")
    )

    private val clearOverlay = tool(
        name = CLEAR_OVERLAY,
        description = "清除所有AI添加的高亮、注解、交叉参考等标注，恢复原始状态。无需参数。",
        properties = JsonObject().apply { addProperty("_dummy", "unused") },
        required = emptyList()
    )

    private val getCurrentReading = tool(
        name = GET_CURRENT_READING,
        description = "获取用户当前正在阅读的书卷、章节信息，以便提供更精准的上下文解答。无需参数。",
        properties = JsonObject().apply { addProperty("_dummy", "unused") },
        required = emptyList()
    )
}

// ── Tool call result model ────────────────────────────────────────────────────

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String   // JSON string
)

data class ToolResult(
    val callId: String,
    val name: String,
    val result: String,     // JSON string or plain text
    val isError: Boolean = false
)
