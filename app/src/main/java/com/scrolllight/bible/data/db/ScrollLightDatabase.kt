package com.scrolllight.bible.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scrolllight.bible.data.model.Bookmark
import com.scrolllight.bible.data.model.Highlight
import com.scrolllight.bible.data.model.Note
import com.scrolllight.bible.data.model.ReadingProgress

@Database(
    entities = [
        Highlight::class,
        Note::class,
        Bookmark::class,
        ReadingProgress::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScrollLightDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingProgressDao(): ReadingProgressDao
}
