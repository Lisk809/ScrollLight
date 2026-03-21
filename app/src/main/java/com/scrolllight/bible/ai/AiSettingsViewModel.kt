package com.scrolllight.bible.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSettingsUiState(
    val draft: AiConfig = AiConfig(),
    val isSaved: Boolean = false,
    val models: List<ModelInfo> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelError: String? = null
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val repo: AiConfigRepository,
    private val apiClient: AiApiClient
) : ViewModel() {

    private val _state = MutableStateFlow(AiSettingsUiState())
    val state: StateFlow<AiSettingsUiState> = _state.asStateFlow()

    val draft: StateFlow<AiConfig> get() = _state.map { it.draft }.stateIn(
        viewModelScope, SharingStarted.Eagerly, AiConfig()
    )

    init {
        viewModelScope.launch {
            repo.config.first().let { _state.update { s -> s.copy(draft = it) } }
        }
    }

    fun setBaseUrl(url: String)     = _state.update { it.copy(draft = it.draft.copy(baseUrl = url)) }
    fun setApiKey(key: String)      = _state.update { it.copy(draft = it.draft.copy(apiKey = key)) }
    fun setModel(model: String)     = _state.update { it.copy(draft = it.draft.copy(model = model)) }
    fun setMaxTokens(v: Int)        = _state.update { it.copy(draft = it.draft.copy(maxTokens = v)) }
    fun setTemperature(v: Float)    = _state.update { it.copy(draft = it.draft.copy(temperature = v)) }
    fun setStream(v: Boolean)       = _state.update { it.copy(draft = it.draft.copy(streamEnabled = v)) }
    fun setToolCalling(v: Boolean)  = _state.update { it.copy(draft = it.draft.copy(toolCallingEnabled = v)) }
    fun setSystemPrompt(p: String)  = _state.update { it.copy(draft = it.draft.copy(systemPrompt = p)) }
    fun resetSystemPrompt()         = _state.update { it.copy(draft = it.draft.copy(systemPrompt = DEFAULT_SYSTEM_PROMPT)) }

    fun save() = viewModelScope.launch {
        repo.save(_state.value.draft)
        _state.update { it.copy(isSaved = true) }
    }

    fun resetSaved() = _state.update { it.copy(isSaved = false) }

    fun fetchModels() {
        val cfg = _state.value.draft
        if (!cfg.isConfigured) {
            _state.update { it.copy(modelError = "请先填写 Base URL 和 API Key") }
            return
        }
        _state.update { it.copy(isLoadingModels = true, modelError = null) }
        viewModelScope.launch {
            apiClient.fetchModels(cfg).fold(
                onSuccess = { models ->
                    _state.update { it.copy(models = models, isLoadingModels = false) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoadingModels = false, modelError = err.message ?: "获取失败") }
                }
            )
        }
    }

    fun clearModelError() = _state.update { it.copy(modelError = null) }
}
