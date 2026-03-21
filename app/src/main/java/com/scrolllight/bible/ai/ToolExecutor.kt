package com.scrolllight.bible.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.scrolllight.bible.data.model.SearchScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将 AI 的 tool call 参数解析并转发给 AiController 执行 UI 操作。
 * 返回 tool result 字符串回传给 AI。
 */
@Singleton
class ToolExecutor @Inject constructor(
    private val aiController: AiController,
    private val gson: Gson
) {
    /**
     * 执行一个 tool call。
     * @return result JSON string to send back to the model
     */
    suspend fun execute(
        call: ToolCall,
        readingContext: AiReadingContext
    ): ToolResult {
        return try {
            // Models sometimes return "null" or empty string for no-arg tools
            val args = when {
                call.arguments.isBlank()    -> com.google.gson.JsonObject()
                call.arguments == "{}"      -> com.google.gson.JsonObject()
                call.arguments == "null"    -> com.google.gson.JsonObject()
                else -> try {
                    val parsed = JsonParser.parseString(call.arguments)
                    if (parsed.isJsonObject) parsed.asJsonObject else com.google.gson.JsonObject()
                } catch (_: Exception) { com.google.gson.JsonObject() }
            }

            val resultStr = when (call.name) {

                ToolDefinitions.NAVIGATE_TO_CHAPTER -> {
                    val bookId   = args.get("book_id")?.asString   ?: ""
                    val chapter  = args.get("chapter")?.asInt      ?: 1
                    val bookName = args.get("book_name")?.asString ?: bookId
                    aiController.navigateToChapter(bookId, chapter)
                    """{"status":"ok","navigated_to":"${bookName} 第${chapter}章"}"""
                }

                ToolDefinitions.HIGHLIGHT_VERSES -> {
                    val versesArr = args.getAsJsonArray("verses")
                    val verses = versesArr?.map { it.asInt } ?: emptyList()
                    val color  = args.get("color")?.asString ?: "YELLOW"
                    aiController.highlightVerses(verses, color)
                    """{"status":"ok","highlighted":${verses},"color":"$color"}"""
                }

                ToolDefinitions.SEARCH_SCRIPTURE -> {
                    val query = args.get("query")?.asString ?: ""
                    val scopeStr = args.get("scope")?.asString ?: "WHOLE_BIBLE"
                    val scope = try { SearchScope.valueOf(scopeStr) } catch (_: Exception) { SearchScope.WHOLE_BIBLE }
                    aiController.triggerSearch(query, scope)
                    """{"status":"ok","searching":"$query","scope":"${scope.displayName}"}"""
                }

                ToolDefinitions.SCROLL_TO_VERSE -> {
                    val verse = args.get("verse")?.asInt ?: 1
                    aiController.scrollToVerse(verse)
                    """{"status":"ok","scrolled_to_verse":$verse}"""
                }

                ToolDefinitions.SHOW_ANNOTATION -> {
                    val verse      = args.get("verse")?.asInt      ?: 1
                    val annotation = args.get("annotation")?.asString ?: ""
                    aiController.showAnnotation(verse, annotation)
                    """{"status":"ok","annotation_shown":true}"""
                }

                ToolDefinitions.SHOW_CROSS_REFERENCES -> {
                    val refsArr = args.getAsJsonArray("references")
                    val refs = refsArr?.map { it.asString } ?: emptyList()
                    aiController.showCrossReferences(refs)
                    """{"status":"ok","cross_references":${gson.toJson(refs)}}"""
                }

                ToolDefinitions.CLEAR_OVERLAY -> {
                    aiController.clearAiOverlay()
                    """{"status":"ok","cleared":true}"""
                }

                ToolDefinitions.GET_CURRENT_READING -> {
                    """{"book_id":"${readingContext.bookId}","book_name":"${readingContext.bookName}","chapter":${readingContext.chapter},"version":"${readingContext.version}","selected_verse":${readingContext.selectedVerse ?: "null"}}"""
                }

                else -> """{"status":"error","message":"Unknown tool: ${call.name}"}"""
            }

            ToolResult(callId = call.id, name = call.name, result = resultStr)
        } catch (e: Exception) {
            ToolResult(
                callId  = call.id,
                name    = call.name,
                result  = """{"status":"error","message":"${e.message}"}""",
                isError = true
            )
        }
    }
}
