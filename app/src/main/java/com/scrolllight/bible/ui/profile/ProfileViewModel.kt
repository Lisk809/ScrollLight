package com.scrolllight.bible.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolllight.bible.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfileUiState(
    val username: String = "Lisk",
    val noteCount: Int = 0,
    val highlightCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepo: UserRepository
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(
        userRepo.getNoteCount(),
        userRepo.highlightCount()
    ) { notes, highlights ->
        ProfileUiState(noteCount = notes, highlightCount = highlights)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())
}
