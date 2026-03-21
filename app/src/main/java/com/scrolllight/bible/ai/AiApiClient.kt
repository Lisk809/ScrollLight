package com.scrolllight.bible.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Sealed result types ───────────────────────────────────────────────────────

sealed class StreamChunk {
    data class Token(val text: String)              : StreamChunk()
    data class ToolCallStart(val call: ToolCall)    : StreamChunk()
    data class ToolCallDelta(val id: String, val argDelta: String) : StreamChunk()
    data class ToolCallComplete(val calls: List<ToolCall>) : StreamChunk()
    data class Done(val finishReason: String)       : StreamChunk()
    data class Error(val message: String)           : StreamChunk()
}

// ── OpenAI-compatible message builder ────────────────────────────────────────

object MessageBuilder {
    fun system(content: String) = JsonObject().apply {
        addProperty("role", "system"); addProperty("content", content)
    }
    fun user(content: String) = JsonObject().apply {
        addProperty("role", "user"); addProperty("content", content)
    }
    fun assistant(content: String?, toolCalls: List<ToolCall>? = null) = JsonObject().apply {
        addProperty("role", "assistant")
        if (!content.isNullOrBlank()) addProperty("content", content)
        if (!toolCalls.isNullOrEmpty()) {
            add("tool_calls", JsonArray().also { arr ->
                toolCalls.forEach { tc ->
                    arr.add(JsonObject().apply {
                        addProperty("id", tc.id)
                        addProperty("type", "function")
                        add("function", JsonObject().apply {
                            addProperty("name", tc.name)
                            addProperty("arguments", tc.arguments)
                        })
                    })
                }
            })
        }
    }
    fun toolResult(result: ToolResult) = JsonObject().apply {
        addProperty("role", "tool")
        addProperty("tool_call_id", result.callId)
        addProperty("content", result.result)
    }
}

// ── API Client ────────────────────────────────────────────────────────────────

@Singleton
class AiApiClient @Inject constructor(private val gson: Gson) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // ── Build request body ────────────────────────────────────────────────

    private fun buildBody(
        config: AiConfig,
        messages: JsonArray,
        stream: Boolean,
        withTools: Boolean = true
    ): String {
        return JsonObject().apply {
            addProperty("model", config.model)
            add("messages", messages)
            addProperty("max_tokens", config.maxTokens)
            addProperty("temperature", config.temperature.toDouble())
            addProperty("stream", stream)
            if (withTools) {
                add("tools", ToolDefinitions.all)
                addProperty("tool_choice", "auto")
            }
        }.toString()
    }

    // ── Streaming chat (SSE) ──────────────────────────────────────────────

    fun streamChat(config: AiConfig, messages: JsonArray): Flow<StreamChunk> = callbackFlow {
        val body = buildBody(config, messages, stream = true)
        val request = Request.Builder()
            .url(config.chatEndpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Accept", "text/event-stream")
            .post(body.toRequestBody(JSON))
            .build()

        // Accumulate partial tool call arguments across deltas
        val toolCallBuffers = mutableMapOf<Int, Triple<String, String, StringBuilder>>() // idx → (id, name, argBuf)

        val factory = EventSources.createFactory(client)
        val source = factory.newEventSource(request, object : EventSourceListener() {

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    // Flush any buffered tool calls
                    if (toolCallBuffers.isNotEmpty()) {
                        val calls = toolCallBuffers.values.map { (tcId, name, buf) ->
                            ToolCall(tcId, name, buf.toString().ifBlank { "{}" })
                        }
                        trySend(StreamChunk.ToolCallComplete(calls))
                    }
                    trySend(StreamChunk.Done("stop"))
                    close()
                    return
                }
                try {
                    val json = JsonParser.parseString(data).asJsonObject
                    val choice = json.getAsJsonArray("choices")?.get(0)?.asJsonObject ?: return
                    val delta  = choice.getAsJsonObject("delta") ?: return
                    val finish = choice.get("finish_reason")?.takeIf { !it.isJsonNull }?.asString

                    // Text token
                    delta.get("content")?.takeIf { !it.isJsonNull }?.asString?.let { token ->
                        if (token.isNotEmpty()) trySend(StreamChunk.Token(token))
                    }

                    // Tool call deltas
                    delta.getAsJsonArray("tool_calls")?.forEach { tcElem ->
                        val tc  = tcElem.asJsonObject
                        val idx = tc.get("index")?.asInt ?: 0
                        val tcId   = tc.get("id")?.takeIf { !it.isJsonNull }?.asString
                        val fn     = tc.getAsJsonObject("function")
                        val name   = fn?.get("name")?.takeIf { !it.isJsonNull }?.asString
                        val argsDelta = fn?.get("arguments")?.takeIf { !it.isJsonNull }?.asString ?: ""

                        if (tcId != null && name != null) {
                            toolCallBuffers[idx] = Triple(tcId, name, StringBuilder(argsDelta))
                        } else {
                            toolCallBuffers[idx]?.third?.append(argsDelta)
                        }
                    }

                    if (finish == "tool_calls") {
                        val calls = toolCallBuffers.values.map { (tcId, name, buf) ->
                            ToolCall(tcId, name, buf.toString().ifBlank { "{}" })
                        }
                        trySend(StreamChunk.ToolCallComplete(calls))
                        toolCallBuffers.clear()
                    } else if (finish == "stop" || finish == "length") {
                        trySend(StreamChunk.Done(finish))
                        close()
                    }
                } catch (_: Exception) { /* skip malformed chunk */ }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val msg = t?.message ?: response?.let { "HTTP ${it.code}: ${it.message}" } ?: "未知错误"
                trySend(StreamChunk.Error(msg))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose { source.cancel() }
    }

    // ── Non-streaming chat ────────────────────────────────────────────────

    fun chat(config: AiConfig, messages: JsonArray): Flow<StreamChunk> = flow {
        val body = buildBody(config, messages, stream = false)
        val request = Request.Builder()
            .url(config.chatEndpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody(JSON))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(StreamChunk.Error("HTTP ${response.code}: ${response.message}"))
                return@flow
            }
            val json = JsonParser.parseString(response.body?.string() ?: "{}").asJsonObject
            val choice  = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
            val message = choice?.getAsJsonObject("message")
            val finish  = choice?.get("finish_reason")?.asString ?: "stop"

            message?.get("content")?.takeIf { !it.isJsonNull }?.asString?.let { text ->
                if (text.isNotBlank()) emit(StreamChunk.Token(text))
            }

            val toolCalls = message?.getAsJsonArray("tool_calls")
            if (toolCalls != null && toolCalls.size() > 0) {
                val calls = toolCalls.map { elem ->
                    val tc = elem.asJsonObject
                    ToolCall(
                        id        = tc.get("id").asString,
                        name      = tc.getAsJsonObject("function").get("name").asString,
                        arguments = tc.getAsJsonObject("function").get("arguments").asString
                    )
                }
                emit(StreamChunk.ToolCallComplete(calls))
            }

            emit(StreamChunk.Done(finish))
        } catch (e: IOException) {
            emit(StreamChunk.Error("网络错误: ${e.message}"))
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────

    fun send(config: AiConfig, messages: JsonArray): Flow<StreamChunk> =
        if (config.streamEnabled) streamChat(config, messages) else chat(config, messages)
}
