package com.scrolllight.bible.data.db

import androidx.room.*
import com.scrolllight.bible.data.model.Bookmark
import com.scrolllight.bible.data.model.Highlight
import com.scrolllight.bible.data.model.Note
import com.scrolllight.bible.data.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

// ─── Highlight DAO ────────────────────────────────────────────────────────────

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun getAllHighlights(): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND chapter = :chapter")
    fun getHighlightsForChapter(bookId: String, chapter: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse LIMIT 1")
    suspend fun getHighlightForVerse(bookId: String, chapter: Int, verse: Int): Highlight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: Highlight): Long

    @Delete
    suspend fun delete(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteForVerse(bookId: String, chapter: Int, verse: Int)

    @Query("SELECT COUNT(*) FROM highlights")
    fun getHighlightCount(): Flow<Int>
}

// ─── Note DAO ─────────────────────────────────────────────────────────────────

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    fun getNotesForVerse(bookId: String, chapter: Int, verse: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId AND chapter = :chapter")
    fun getNotesForChapter(bookId: String, chapter: Int): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT COUNT(*) FROM notes")
    fun getNoteCount(): Flow<Int>
}

// ─── Bookmark DAO ─────────────────────────────────────────────────────────────

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse LIMIT 1")
    suspend fun getBookmark(bookId: String, chapter: Int, verse: Int): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark): Long

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteByRef(bookId: String, chapter: Int, verse: Int)
}

// ─── Reading Progress DAO ─────────────────────────────────────────────────────

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress")
    fun getAllProgress(): Flow<List<ReadingProgress>>

    @Query("SELECT * FROM reading_progress WHERE planId = :planId LIMIT 1")
    suspend fun getProgressForPlan(planId: String): ReadingProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgress)

    @Delete
    suspend fun delete(progress: ReadingProgress)
}
