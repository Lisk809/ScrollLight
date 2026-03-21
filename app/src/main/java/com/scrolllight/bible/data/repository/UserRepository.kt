package com.scrolllight.bible.data.repository

import com.scrolllight.bible.data.db.*
import com.scrolllight.bible.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val highlightDao: HighlightDao,
    private val noteDao: NoteDao,
    private val bookmarkDao: BookmarkDao,
    private val progressDao: ReadingProgressDao
) {
    // Highlights
    fun getAllHighlights(): Flow<List<Highlight>> = highlightDao.getAllHighlights()
    fun getHighlightsForChapter(bookId: String, chapter: Int) = highlightDao.getHighlightsForChapter(bookId, chapter)
    fun highlightCount(): Flow<Int> = highlightDao.getHighlightCount()
    suspend fun toggleHighlight(bookId: String, bookName: String, chapter: Int, verse: Int, color: HighlightColor = HighlightColor.YELLOW) {
        val existing = highlightDao.getHighlightForVerse(bookId, chapter, verse)
        if (existing != null) highlightDao.delete(existing)
        else highlightDao.insert(Highlight(bookId = bookId, bookName = bookName, chapter = chapter, verse = verse, color = color))
    }
    suspend fun removeHighlight(bookId: String, chapter: Int, verse: Int) = highlightDao.deleteForVerse(bookId, chapter, verse)

    // Notes
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    fun getNoteCount(): Flow<Int> = noteDao.getNoteCount()
    fun getNotesForChapter(bookId: String, chapter: Int) = noteDao.getNotesForChapter(bookId, chapter)
    suspend fun saveNote(note: Note) = if (note.id == 0L) noteDao.insert(note) else { noteDao.update(note); note.id }
    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    // Bookmarks
    fun getAllBookmarks(): Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    suspend fun toggleBookmark(bookId: String, bookName: String, chapter: Int, verse: Int) {
        val existing = bookmarkDao.getBookmark(bookId, chapter, verse)
        if (existing != null) bookmarkDao.delete(existing)
        else bookmarkDao.insert(Bookmark(bookId = bookId, bookName = bookName, chapter = chapter, verse = verse))
    }

    // Reading progress
    fun getAllProgress(): Flow<List<ReadingProgress>> = progressDao.getAllProgress()
    suspend fun getProgressForPlan(planId: String) = progressDao.getProgressForPlan(planId)
    suspend fun updateProgress(progress: ReadingProgress) = progressDao.upsert(progress)
}
