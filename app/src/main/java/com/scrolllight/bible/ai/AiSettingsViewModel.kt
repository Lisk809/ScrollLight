package com.scrolllight.bible.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val repo: AiConfigRepository
) : ViewModel() {

    // Editable draft state
    private val _draft = MutableStateFlow(AiConfig())
    val draft: StateFlow<AiConfig> = _draft.asStateFlow()

    val isSaved = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repo.config.first().let { _draft.value = it }
        }
    }

    fun setBaseUrl(url: String)       = _draft.update { it.copy(baseUrl = url) }
    fun setApiKey(key: String)        = _draft.update { it.copy(apiKey = key) }
    fun setModel(model: String)       = _draft.update { it.copy(model = model) }
    fun setMaxTokens(v: Int)          = _draft.update { it.copy(maxTokens = v) }
    fun setTemperature(v: Float)      = _draft.update { it.copy(temperature = v) }
    fun setStream(v: Boolean)         = _draft.update { it.copy(streamEnabled = v) }
    fun setSystemPrompt(p: String)    = _draft.update { it.copy(systemPrompt = p) }

    fun save() = viewModelScope.launch {
        repo.save(_draft.value)
        isSaved.value = true
    }

    fun resetSystemPrompt() = _draft.update { it.copy(systemPrompt = DEFAULT_SYSTEM_PROMPT) }
}
