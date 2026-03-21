package com.scrolllight.bible.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.data.model.ReadingPlan
import com.scrolllight.bible.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(val allPlans: List<ReadingPlan> = emptyList())

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PlansUiState())
    val state: StateFlow<PlansUiState> = _state

    init {
        viewModelScope.launch {
            _state.value = PlansUiState(allPlans = bibleRepo.getReadingPlans())
        }
    }

    fun startPlan(plan: ReadingPlan) { /* Navigate to plan detail */ }
}
