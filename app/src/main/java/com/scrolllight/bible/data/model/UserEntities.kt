package com.scrolllight.bible.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Highlight Entity ─────────────────────────────────────────────────────────

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val color: HighlightColor = HighlightColor.YELLOW,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HighlightColor(val hex: String) {
    YELLOW("#FEF08A"),
    GREEN("#BBF7D0"),
    BLUE("#BAE6FD"),
    PINK("#FBCFE8"),
    ORANGE("#FED7AA"),
    PURPLE("#DDD6FE")
}

// ─── Note Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ─── Bookmark Entity ──────────────────────────────────────────────────────────

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Reading Progress ─────────────────────────────────────────────────────────

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey val planId: String,
    val currentDay: Int = 0,
    val completedDays: String = "", // JSON array of completed day numbers
    val startedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis()
)

// ─── User Preferences ────────────────────────────────────────────────────────

data class UserPreferences(
    val fontSize: Float = 18f,
    val fontFamily: String = "default",
    val theme: AppTheme = AppTheme.LIGHT,
    val showParallel: Boolean = false,
    val parallelVersion: String = "NIV",
    val primaryVersion: String = "CUV",
    val language: String = "zh-CN"
)

enum class AppTheme { LIGHT, DARK, SEPIA, SYSTEM }
