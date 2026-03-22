package com.scrolllight.bible.data.library

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Book Catalog ──────────────────────────────────────────────────────────────

@Dao
interface BookCatalogDao {
    @Query("SELECT * FROM book_catalog ORDER BY isDefault DESC, type, language")
    fun getAllBooks(): Flow<List<BookCatalogEntry>>

    @Query("SELECT * FROM book_catalog WHERE isInstalled = 1 ORDER BY type, language")
    fun getInstalledBooks(): Flow<List<BookCatalogEntry>>

    @Query("SELECT * FROM book_catalog WHERE type = :type AND isInstalled = 1")
    fun getInstalledByType(type: BookType): Flow<List<BookCatalogEntry>>

    @Query("SELECT * FROM book_catalog WHERE bookId = :id LIMIT 1")
    suspend fun getById(id: String): BookCatalogEntry?

    @Query("SELECT * FROM book_catalog WHERE isDefault = 1 AND type = :type LIMIT 1")
    suspend fun getDefault(type: BookType): BookCatalogEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BookCatalogEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<BookCatalogEntry>)

    @Query("UPDATE book_catalog SET isInstalled = :installed WHERE bookId = :id")
    suspend fun setInstalled(id: String, installed: Boolean)

    @Query("UPDATE book_catalog SET isDefault = 0 WHERE type = :type")
    suspend fun clearDefault(type: BookType)

    @Query("UPDATE book_catalog SET isDefault = 1 WHERE bookId = :id")
    suspend fun setDefault(id: String)

    @Query("DELETE FROM book_catalog WHERE bookId = :id AND isBuiltIn = 0")
    suspend fun deleteNonBuiltIn(id: String)
}

// ── Bible Verses ──────────────────────────────────────────────────────────────

@Dao
interface BibleVerseDao {
    /** 获取单章所有节 */
    @Query("""
        SELECT * FROM bible_verses 
        WHERE bookId = :bookId AND chapter = :chapter AND versionId = :versionId
        ORDER BY verse
    """)
    suspend fun getChapter(bookId: String, chapter: Int, versionId: String): List<BibleVerseEntity>

    /** 同时获取两个版本（平行对照） */
    @Query("""
        SELECT * FROM bible_verses 
        WHERE bookId = :bookId AND chapter = :chapter 
          AND versionId IN (:v1, :v2)
        ORDER BY verse, versionId
    """)
    suspend fun getChapterParallel(bookId: String, chapter: Int, v1: String, v2: String): List<BibleVerseEntity>

    /** 获取单节 */
    @Query("""
        SELECT * FROM bible_verses 
        WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse AND versionId = :versionId
        LIMIT 1
    """)
    suspend fun getVerse(bookId: String, chapter: Int, verse: Int, versionId: String): BibleVerseEntity?

    /** 全文搜索（FTS 前简单实现） */
    @Query("""
        SELECT * FROM bible_verses 
        WHERE versionId = :versionId AND text LIKE '%' || :query || '%'
        LIMIT 200
    """)
    suspend fun search(query: String, versionId: String): List<BibleVerseEntity>

    /** 按书卷搜索 */
    @Query("""
        SELECT * FROM bible_verses 
        WHERE versionId = :versionId AND bookId = :bookId AND text LIKE '%' || :query || '%'
        LIMIT 100
    """)
    suspend fun searchInBook(query: String, bookId: String, versionId: String): List<BibleVerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<BibleVerseEntity>)

    @Query("DELETE FROM bible_verses WHERE versionId = :versionId")
    suspend fun deleteVersion(versionId: String)

    @Query("SELECT COUNT(*) FROM bible_verses WHERE versionId = :versionId")
    suspend fun countVerses(versionId: String): Int
}

// ── Original Words ────────────────────────────────────────────────────────────

@Dao
interface OriginalWordDao {
    @Query("""
        SELECT * FROM original_words 
        WHERE bookId = :bookId AND chapter = :chapter AND versionId = :versionId
        ORDER BY verse, wordIndex
    """)
    suspend fun getChapter(bookId: String, chapter: Int, versionId: String): List<OriginalWordEntity>

    @Query("""
        SELECT * FROM original_words 
        WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse AND versionId = :versionId
        ORDER BY wordIndex
    """)
    suspend fun getVerse(bookId: String, chapter: Int, verse: Int, versionId: String): List<OriginalWordEntity>

    /** 查找同一 Strongs 编号在某章的所有出现 */
    @Query("""
        SELECT * FROM original_words 
        WHERE bookId = :bookId AND chapter = :chapter AND strongs = :strongsId
    """)
    suspend fun findByStrongs(bookId: String, chapter: Int, strongsId: String): List<OriginalWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<OriginalWordEntity>)

    @Query("DELETE FROM original_words WHERE versionId = :versionId")
    suspend fun deleteVersion(versionId: String)
}

// ── Study Notes ───────────────────────────────────────────────────────────────

@Dao
interface StudyNoteDao {
    /** 获取一章内所有注释 */
    @Query("""
        SELECT * FROM study_notes 
        WHERE versionId = :versionId AND bookId = :bookId AND chapterFrom = :chapter
        ORDER BY verseFrom
    """)
    suspend fun getNotesForChapter(versionId: String, bookId: String, chapter: Int): List<StudyNoteEntity>

    /** 获取一节的注释（包含跨节范围的注释） */
    @Query("""
        SELECT * FROM study_notes 
        WHERE versionId = :versionId AND bookId = :bookId 
          AND chapterFrom <= :chapter AND chapterTo >= :chapter
          AND verseFrom  <= :verse   AND verseTo   >= :verse
        ORDER BY noteType
    """)
    suspend fun getNotesForVerse(versionId: String, bookId: String, chapter: Int, verse: Int): List<StudyNoteEntity>

    /** 获取书卷引言 */
    @Query("""
        SELECT * FROM study_notes 
        WHERE versionId = :versionId AND bookId = :bookId AND noteType = 'INTRODUCTION'
        LIMIT 1
    """)
    suspend fun getBookIntroduction(versionId: String, bookId: String): StudyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<StudyNoteEntity>)

    @Query("DELETE FROM study_notes WHERE versionId = :versionId")
    suspend fun deleteVersion(versionId: String)
}

// ── Strongs Lexicon ───────────────────────────────────────────────────────────

@Dao
interface StrongsDao {
    @Query("SELECT * FROM strongs_lexicon WHERE strongsId = :id LIMIT 1")
    suspend fun lookup(id: String): StrongsEntry?

    @Query("SELECT * FROM strongs_lexicon WHERE original LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchOriginal(query: String): List<StrongsEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StrongsEntry>)
}
