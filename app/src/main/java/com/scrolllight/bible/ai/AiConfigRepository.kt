package com.scrolllight.bible.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ── DataStore extension ───────────────────────────────────────────────────────

val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

// ── Config model ──────────────────────────────────────────────────────────────

data class AiConfig(
    val baseUrl: String  = "https://api.openai.com/v1",
    val apiKey: String   = "",
    val model: String    = "gpt-4o-mini",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxTokens: Int   = 2048,
    val temperature: Float = 0.7f,
    val streamEnabled: Boolean = true
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank()
    val chatEndpoint: String  get() = baseUrl.trimEnd('/') + "/chat/completions"
}

const val DEFAULT_SYSTEM_PROMPT = """你是「光言助读」，一位专精圣经知识的AI助手，内嵌于光言圣经阅读应用。

你的能力：
- 解释经文含义、背景与神学意涵
- 提供跨文化、跨时代的圣经洞见
- 引用原文（希伯来文/希腊文）辅助解释
- 主动使用工具高亮、跳转、注释经文

规则：
- 回答简洁有力，一般不超过200字
- 引用经文时使用工具高亮对应节次
- 主动使用navigate_to_chapter引导用户阅读相关章节
- 使用中文回答，引用经文用和合本
- 若用户问的问题与当前阅读章节有关，优先结合上下文作答"""

// ── Keys ──────────────────────────────────────────────────────────────────────

object AiConfigKeys {
    val BASE_URL      = stringPreferencesKey("ai_base_url")
    val API_KEY       = stringPreferencesKey("ai_api_key")
    val MODEL         = stringPreferencesKey("ai_model")
    val SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
    val MAX_TOKENS    = stringPreferencesKey("ai_max_tokens")
    val TEMPERATURE   = stringPreferencesKey("ai_temperature")
    val STREAM        = stringPreferencesKey("ai_stream")
}

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class AiConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val config: Flow<AiConfig> = context.aiConfigDataStore.data.map { prefs ->
        AiConfig(
            baseUrl      = prefs[AiConfigKeys.BASE_URL]      ?: "https://api.openai.com/v1",
            apiKey       = prefs[AiConfigKeys.API_KEY]       ?: "",
            model        = prefs[AiConfigKeys.MODEL]         ?: "gpt-4o-mini",
            systemPrompt = prefs[AiConfigKeys.SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT,
            maxTokens    = prefs[AiConfigKeys.MAX_TOKENS]?.toIntOrNull() ?: 2048,
            temperature  = prefs[AiConfigKeys.TEMPERATURE]?.toFloatOrNull() ?: 0.7f,
            streamEnabled = prefs[AiConfigKeys.STREAM]?.toBooleanStrictOrNull() ?: true
        )
    }

    suspend fun save(config: AiConfig) {
        context.aiConfigDataStore.edit { prefs ->
            prefs[AiConfigKeys.BASE_URL]      = config.baseUrl
            prefs[AiConfigKeys.API_KEY]       = config.apiKey
            prefs[AiConfigKeys.MODEL]         = config.model
            prefs[AiConfigKeys.SYSTEM_PROMPT] = config.systemPrompt
            prefs[AiConfigKeys.MAX_TOKENS]    = config.maxTokens.toString()
            prefs[AiConfigKeys.TEMPERATURE]   = config.temperature.toString()
            prefs[AiConfigKeys.STREAM]        = config.streamEnabled.toString()
        }
    }

    suspend fun saveBaseUrl(url: String)  = context.aiConfigDataStore.edit { it[AiConfigKeys.BASE_URL] = url }
    suspend fun saveApiKey(key: String)   = context.aiConfigDataStore.edit { it[AiConfigKeys.API_KEY]  = key }
    suspend fun saveModel(model: String)  = context.aiConfigDataStore.edit { it[AiConfigKeys.MODEL]    = model }
}
