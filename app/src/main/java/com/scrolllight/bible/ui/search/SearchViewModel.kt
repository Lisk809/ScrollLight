package com.scrolllight.bible.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.data.model.SearchResult
import com.scrolllight.bible.data.model.SearchScope
import com.scrolllight.bible.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val scope: SearchScope = SearchScope.WHOLE_BIBLE,
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // debounce
                search(query)
            }
        } else {
            _state.value = _state.value.copy(results = emptyList())
        }
    }

    fun setScope(scope: SearchScope) {
        _state.value = _state.value.copy(scope = scope)
        if (_state.value.query.isNotBlank()) search(_state.value.query)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            val results = bibleRepo.search(query, _state.value.scope)
            _state.value = _state.value.copy(results = results, isLoading = false)
        }
    }
}
