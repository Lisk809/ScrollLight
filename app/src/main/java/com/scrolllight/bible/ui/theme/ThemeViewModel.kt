package com.scrolllight.bible.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ── DataStore ─────────────────────────────────────────────────────────────────

private val Context.themeDataStore by preferencesDataStore(name = "aurora_theme")
private val KEY_THEME = stringPreferencesKey("theme_mode")

// ── Repository ────────────────────────────────────────────────────────────────

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val stored = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
        ThemeMode.values().firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[KEY_THEME] = mode.name }
    }
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repo: ThemeRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
}
