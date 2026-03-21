package com.scrolllight.bible.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * 所有可供 AI 调用的工具定义（OpenAI tool calling 格式）
 *
 * 注意事项（兼容百炼、DeepSeek 等严格校验接口）：
 * 1. 每个工具的 parameters 必须是合法 JSON Schema object
 * 2. 无参数工具用空 properties + additionalProperties:false，不能放假字段
 * 3. additionalProperties 只能是 true/false，不能是字符串
 */
object ToolDefinitions {

    const val NAVIGATE_TO_CHAPTER   = "navigate_to_chapter"
    const val HIGHLIGHT_VERSES      = "highlight_verses"
    const val SEARCH_SCRIPTURE      = "search_scripture"
    const val SCROLL_TO_VERSE       = "scroll_to_verse"
    const val SHOW_ANNOTATION       = "show_annotation"
    const val SHOW_CROSS_REFERENCES = "show_cross_references"
    const val CLEAR_OVERLAY         = "clear_overlay"
    const val GET_CURRENT_READING   = "get_current_reading"

    // ── Schema builders ───────────────────────────────────────────────────────

    /** 构建完整工具对象 */
    private fun tool(
        name: String,
        description: String,
        properties: JsonObject,      // JSON Schema properties object
        required: List<String>,
        additionalProperties: Boolean = false
    ): JsonObject {
        val params = JsonObject().apply {
            addProperty("type", "object")
            add("properties", properties)
            // additionalProperties must be boolean — never a string
            addProperty("additionalProperties", additionalProperties)
            if (required.isNotEmpty()) {
                add("required", JsonArray().also { arr -> required.forEach { arr.add(it) } })
            }
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

    /** 无参数工具专用构建器 */
    private fun noParamTool(name: String, description: String): JsonObject {
        val params = JsonObject().apply {
            addProperty("type", "object")
            add("properties", JsonObject())          // empty properties = no params
            addProperty("additionalProperties", false)
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

    /** 字符串 / 整数 属性 */
    private fun prop(type: String, description: String, enum: List<String>? = null): JsonObject =
        JsonObject().apply {
            addProperty("type", type)
            addProperty("description", description)
            if (enum != null) add("enum", JsonArray().also { a -> enum.forEach { a.add(it) } })
        }

    /** 数组属性 */
    private fun arrayProp(itemType: String, description: String): JsonObject =
        JsonObject().apply {
            addProperty("type", "array")
            addProperty("description", description)
            add("items", JsonObject().apply { addProperty("type", itemType) })
        }

    // ── Tool definitions ──────────────────────────────────────────────────────

    private val navigateToChapter = tool(
        name        = NAVIGATE_TO_CHAPTER,
        description = "导航到指定的圣经书卷和章节。需要引导用户阅读特定经文时调用。",
        properties  = JsonObject().apply {
            add("book_id",   prop("string",  "书卷ID，如 mat=马太福音, jhn=约翰福音, rom=罗马书, gen=创世记"))
            add("chapter",   prop("integer", "章节数字，从1开始"))
            add("book_name", prop("string",  "书卷中文名称，如马太福音"))
        },
        required = listOf("book_id", "chapter")
    )

    private val highlightVerses = tool(
        name        = HIGHLIGHT_VERSES,
        description = "在当前章节中高亮指定的经节。引用或解释具体经节时调用。",
        properties  = JsonObject().apply {
            add("verses", arrayProp("integer", "要高亮的节次列表，如 [3, 5, 7]"))
            add("color",  prop("string", "高亮颜色",
                listOf("YELLOW", "GREEN", "BLUE", "PINK", "ORANGE", "PURPLE")))
            add("reason", prop("string", "高亮原因，可选"))
        },
        required = listOf("verses")
    )

    private val searchScripture = tool(
        name        = SEARCH_SCRIPTURE,
        description = "在圣经中搜索关键词。用户询问某主题在圣经中的出处时调用。",
        properties  = JsonObject().apply {
            add("query", prop("string", "搜索关键词，如爱、信心、以利亚"))
            add("scope", prop("string", "搜索范围",
                listOf("WHOLE_BIBLE", "OLD_TESTAMENT", "NEW_TESTAMENT", "GOSPELS")))
        },
        required = listOf("query")
    )

    private val scrollToVerse = tool(
        name        = SCROLL_TO_VERSE,
        description = "将阅读视图滚动到当前章节的指定节次。",
        properties  = JsonObject().apply {
            add("verse", prop("integer", "要定位的节次数字"))
        },
        required = listOf("verse")
    )

    private val showAnnotation = tool(
        name        = SHOW_ANNOTATION,
        description = "在指定节次旁边显示注解气泡。",
        properties  = JsonObject().apply {
            add("verse",      prop("integer", "要注解的节次"))
            add("annotation", prop("string",  "注解内容，60字以内"))
        },
        required = listOf("verse", "annotation")
    )

    private val showCrossReferences = tool(
        name        = SHOW_CROSS_REFERENCES,
        description = "显示交叉参考面板，列出相关经文引用。",
        properties  = JsonObject().apply {
            add("references", arrayProp("string", "交叉参考列表，如 [约 3:16, 罗 5:8]"))
            add("title",      prop("string", "面板标题"))
        },
        required = listOf("references")
    )

    // ── 无参数工具（用 noParamTool 避免 _dummy 字段问题）────────────────────

    private val clearOverlay = noParamTool(
        name        = CLEAR_OVERLAY,
        description = "清除所有AI添加的高亮、注解、交叉参考标注，恢复原始状态。无需任何参数。"
    )

    private val getCurrentReading = noParamTool(
        name        = GET_CURRENT_READING,
        description = "获取用户当前正在阅读的书卷和章节信息。无需任何参数。"
    )

    // ── Public tool list ──────────────────────────────────────────────────────

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
}

// ── Tool call models ──────────────────────────────────────────────────────────

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

data class ToolResult(
    val callId: String,
    val name: String,
    val result: String,
    val isError: Boolean = false
)
