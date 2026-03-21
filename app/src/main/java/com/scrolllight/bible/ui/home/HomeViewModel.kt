package com.scrolllight.bible.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.data.model.BibleBook
import com.scrolllight.bible.data.model.DailyVerse
import com.scrolllight.bible.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val dailyVerse: DailyVerse? = null,
    val recentBook: BibleBook? = null,
    val recentChapter: Int = 1
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bibleRepo: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            val verse = bibleRepo.getDailyVerse()
            val book  = bibleRepo.getBook("mat")
            _uiState.value = HomeUiState(dailyVerse = verse, recentBook = book, recentChapter = 17)
        }
    }
}
