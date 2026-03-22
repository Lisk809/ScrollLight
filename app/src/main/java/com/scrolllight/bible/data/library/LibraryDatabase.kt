package com.scrolllight.bible.data.library

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

// ── Room Database ─────────────────────────────────────────────────────────────

@Database(
    entities = [
        BookCatalogEntry::class,
        BibleVerseEntity::class,
        OriginalWordEntity::class,
        StudyNoteEntity::class,
        StrongsEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun catalogDao(): BookCatalogDao
    abstract fun verseDao(): BibleVerseDao
    abstract fun originalWordDao(): OriginalWordDao
    abstract fun studyNoteDao(): StudyNoteDao
    abstract fun strongsDao(): StrongsDao
}

// ── .slbook File Parser ───────────────────────────────────────────────────────

/**
 * 解析 .slbook 压缩包并写入 LibraryDatabase
 *
 * .slbook 格式 (ZIP):
 *   metadata.json  — SlBookMetadata
 *   verses.jsonl   — 每行一个 VerseRecord JSON（适合流式解析大文件）
 *   words.jsonl    — 每行一个 WordRecord JSON（原文，可选）
 *   notes.jsonl    — 每行一个 NoteRecord JSON（注释，可选）
 *   strongs.jsonl  — 每行一个 StrongsRecord（词典，可选）
 *
 * JSONL 格式比 SQLite 更便于用户自制和版本管理（Git friendly）
 */
object SlBookParser {

    // 行数据临时模型
    private data class VerseRecord(
        val b: String, val c: Int, val v: Int, val t: String, val t2: String = ""
    )
    private data class WordRecord(
        val b: String, val c: Int, val v: Int, val i: Int,
        val s: String, val l: String = "", val st: String = "",
        val m: String = "", val tr: String = "", val g: String = ""
    )
    private data class NoteRecord(
        val id: String, val b: String,
        val cf: Int, val vf: Int, val ct: Int, val vt: Int,
        val title: String = "", val content: String, val type: String = "VERSE_NOTE",
        val tags: String = "[]"
    )
    private data class StrongsRecord(
        val id: String, val orig: String, val tr: String,
        val def: String, val defEn: String = "", val usage: String = "", val lang: String
    )

    suspend fun parse(
        inputStream: InputStream,
        db: LibraryDatabase,
        gson: Gson,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<SlBookMetadata> = withContext(Dispatchers.IO) {
        try {
            var metadata: SlBookMetadata? = null
            var verseBatch = mutableListOf<BibleVerseEntity>()
            var wordBatch  = mutableListOf<OriginalWordEntity>()
            var noteBatch  = mutableListOf<StudyNoteEntity>()
            var strongsBatch = mutableListOf<StrongsEntry>()

            val zip = ZipInputStream(inputStream.buffered())
            var entry = zip.nextEntry

            while (entry != null) {
                when (entry.name) {
                    "metadata.json" -> {
                        onProgress(0.05f, "读取元数据…")
                        metadata = gson.fromJson(
                            zip.bufferedReader().readText(),
                            SlBookMetadata::class.java
                        )
                    }
                    "verses.jsonl" -> {
                        onProgress(0.10f, "导入经文…")
                        val versionId = metadata?.bookId ?: "unknown"
                        // Collect all lines first (forEachLine is NOT a coroutine lambda)
                        zip.bufferedReader().forEachLine { line ->
                            if (line.isBlank()) return@forEachLine
                            val r = gson.fromJson(line, VerseRecord::class.java)
                            verseBatch.add(BibleVerseEntity(r.b, r.c, r.v, versionId, r.t, r.t2))
                        }
                        // Insert outside the lambda where suspend is allowed
                        if (verseBatch.isNotEmpty()) {
                            db.verseDao().insertAll(verseBatch)
                            verseBatch = mutableListOf()
                        }
                        onProgress(0.50f, "经文导入完成")
                    }
                    "words.jsonl" -> {
                        onProgress(0.55f, "导入原文…")
                        val versionId = metadata?.bookId ?: "unknown"
                        zip.bufferedReader().forEachLine { line ->
                            if (line.isBlank()) return@forEachLine
                            val r = gson.fromJson(line, WordRecord::class.java)
                            wordBatch.add(OriginalWordEntity(r.b, r.c, r.v, r.i, versionId, r.s, r.l, r.st, r.m, r.tr, r.g))
                        }
                        if (wordBatch.isNotEmpty()) {
                            db.originalWordDao().insertAll(wordBatch)
                            wordBatch = mutableListOf()
                        }
                        onProgress(0.75f, "原文导入完成")
                    }
                    "notes.jsonl" -> {
                        onProgress(0.78f, "导入注释…")
                        val versionId = metadata?.bookId ?: "unknown"
                        zip.bufferedReader().forEachLine { line ->
                            if (line.isBlank()) return@forEachLine
                            val r = gson.fromJson(line, NoteRecord::class.java)
                            val noteType = try { NoteType.valueOf(r.type) } catch (_: Exception) { NoteType.VERSE_NOTE }
                            noteBatch.add(StudyNoteEntity(r.id, versionId, r.b, r.cf, r.vf, r.ct, r.vt, r.title, r.content, noteType, r.tags))
                        }
                        if (noteBatch.isNotEmpty()) {
                            db.studyNoteDao().insertAll(noteBatch)
                            noteBatch = mutableListOf()
                        }
                        onProgress(0.90f, "注释导入完成")
                    }
                    "strongs.jsonl" -> {
                        onProgress(0.92f, "导入词典…")
                        zip.bufferedReader().forEachLine { line ->
                            if (line.isBlank()) return@forEachLine
                            val r = gson.fromJson(line, StrongsRecord::class.java)
                            strongsBatch.add(StrongsEntry(r.id, r.orig, r.tr, r.def, r.defEn, r.usage, r.lang))
                        }
                        if (strongsBatch.isNotEmpty()) {
                            db.strongsDao().insertAll(strongsBatch)
                            strongsBatch = mutableListOf()
                        }
                        onProgress(0.98f, "词典导入完成")
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            val meta = metadata ?: return@withContext Result.failure(Exception("缺少 metadata.json"))
            onProgress(1f, "导入完成")
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Download State ─────────────────────────────────────────────────────────────

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val bookId: String, val progress: Float, val message: String) : DownloadState()
    data class Installing(val bookId: String, val progress: Float, val message: String) : DownloadState()
    data class Success(val bookId: String) : DownloadState()
    data class Failed(val bookId: String, val error: String) : DownloadState()
}

// ── Library Repository ────────────────────────────────────────────────────────

@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LibraryDatabase,
    private val gson: Gson,
    private val httpClient: OkHttpClient
) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    val installedBooks: Flow<List<BookCatalogEntry>> = db.catalogDao().getInstalledBooks()
    val allBooks:       Flow<List<BookCatalogEntry>> = db.catalogDao().getAllBooks()

    // ── Read operations ───────────────────────────────────────────────────────

    suspend fun getChapter(
        bookId: String, chapter: Int, versionId: String
    ): List<BibleVerseEntity> =
        db.verseDao().getChapter(bookId, chapter, versionId)

    suspend fun getChapterParallel(
        bookId: String, chapter: Int, v1: String, v2: String
    ): Map<Int, Pair<BibleVerseEntity?, BibleVerseEntity?>> {
        val rows = db.verseDao().getChapterParallel(bookId, chapter, v1, v2)
        val byVerse = rows.groupBy { it.verse }
        return byVerse.mapValues { (_, list) ->
            Pair(list.firstOrNull { it.versionId == v1 }, list.firstOrNull { it.versionId == v2 })
        }
    }

    suspend fun getOriginalWords(bookId: String, chapter: Int, versionId: String): List<OriginalWordEntity> =
        db.originalWordDao().getChapter(bookId, chapter, versionId)

    suspend fun getStudyNotes(versionId: String, bookId: String, chapter: Int): List<StudyNoteEntity> =
        db.studyNoteDao().getNotesForChapter(versionId, bookId, chapter)

    suspend fun getVerseNotes(versionId: String, bookId: String, chapter: Int, verse: Int): List<StudyNoteEntity> =
        db.studyNoteDao().getNotesForVerse(versionId, bookId, chapter, verse)

    suspend fun lookupStrongs(id: String): StrongsEntry? =
        db.strongsDao().lookup(id)

    suspend fun search(query: String, versionId: String): List<BibleVerseEntity> =
        db.verseDao().search(query, versionId)

    suspend fun getDefaultVersion(type: BookType): BookCatalogEntry? =
        db.catalogDao().getDefault(type)

    // ── Install from file ─────────────────────────────────────────────────────

    suspend fun installFromFile(file: File): Result<String> {
        val bookId = file.nameWithoutExtension
        _downloadState.value = DownloadState.Installing(bookId, 0f, "开始导入…")
        return withContext(Dispatchers.IO) {
            try {
                val meta = SlBookParser.parse(
                    file.inputStream(), db, gson
                ) { progress, msg ->
                    _downloadState.value = DownloadState.Installing(bookId, progress, msg)
                }.getOrThrow()

                val bookType = try { BookType.valueOf(meta.type.uppercase()) } catch (_: Exception) { BookType.BIBLE_TEXT }
                val lang     = BookLanguage.values().firstOrNull { it.code == meta.language } ?: BookLanguage.EN

                db.catalogDao().upsert(BookCatalogEntry(
                    bookId       = meta.bookId,
                    title        = meta.title,
                    abbreviation = meta.abbreviation,
                    type         = bookType,
                    language     = lang,
                    testament    = meta.testament,
                    description  = meta.description,
                    copyright    = meta.copyright,
                    publisher    = meta.publisher,
                    version      = meta.version,
                    isInstalled  = true,
                    installedAt  = System.currentTimeMillis()
                ))

                _downloadState.value = DownloadState.Success(meta.bookId)
                Result.success(meta.bookId)
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Failed(bookId, e.message ?: "导入失败")
                Result.failure(e)
            }
        }
    }

    // ── Download from URL ─────────────────────────────────────────────────────

    suspend fun downloadAndInstall(entry: BookCatalogEntry): Result<String> {
        if (entry.downloadUrl.isBlank()) return Result.failure(Exception("无下载地址"))

        _downloadState.value = DownloadState.Downloading(entry.bookId, 0f, "连接服务器…")

        return withContext(Dispatchers.IO) {
            try {
                val request  = Request.Builder().url(entry.downloadUrl).build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _downloadState.value = DownloadState.Failed(entry.bookId, "HTTP ${response.code}")
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val tempFile = File(context.cacheDir, "${entry.bookId}.slbook")
                val body     = response.body ?: return@withContext Result.failure(Exception("空响应"))
                val total    = body.contentLength()
                var received = 0L

                tempFile.outputStream().use { out ->
                    body.byteStream().use { inp ->
                        val buf = ByteArray(8192)
                        var n = inp.read(buf)
                        while (n >= 0) {
                            out.write(buf, 0, n)
                            received += n
                            val progress = if (total > 0) received.toFloat() / total else 0f
                            _downloadState.value = DownloadState.Downloading(
                                entry.bookId, progress * 0.7f,
                                "下载中 ${(progress * 100).toInt()}%…"
                            )
                            n = inp.read(buf)
                        }
                    }
                }

                installFromFile(tempFile).also { tempFile.delete() }
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Failed(entry.bookId, e.message ?: "下载失败")
                Result.failure(e)
            }
        }
    }

    // ── Uninstall ─────────────────────────────────────────────────────────────

    suspend fun uninstall(bookId: String) = withContext(Dispatchers.IO) {
        db.verseDao().deleteVersion(bookId)
        db.originalWordDao().deleteVersion(bookId)
        db.studyNoteDao().deleteVersion(bookId)
        db.catalogDao().deleteNonBuiltIn(bookId)
    }

    // ── Set default ───────────────────────────────────────────────────────────

    suspend fun setDefault(bookId: String, type: BookType) {
        db.catalogDao().clearDefault(type)
        db.catalogDao().setDefault(bookId)
    }

    // ── Fetch remote catalog ──────────────────────────────────────────────────

    suspend fun fetchRemoteCatalog(catalogUrl: String): Result<List<RemoteBookEntry>> =
        withContext(Dispatchers.IO) {
            try {
                val request  = Request.Builder().url(catalogUrl).build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val json     = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
                val type     = object : TypeToken<List<RemoteBookEntry>>() {}.type
                val entries: List<RemoteBookEntry> = gson.fromJson(json, type)
                Result.success(entries)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun resetDownloadState() { _downloadState.value = DownloadState.Idle }

    // ── Install built-in .slbook files from assets/ on first launch ──────────

    /**
     * 扫描 assets/ 目录，安装所有 .slbook 文件（已安装的跳过）。
     * 通过 SharedPreferences 记录已安装版本，避免重复解析。
     */
    suspend fun installBuiltInAssets(): List<String> = withContext(Dispatchers.IO) {
        val prefs    = context.getSharedPreferences("built_in_assets", android.content.Context.MODE_PRIVATE)
        val installed = mutableListOf<String>()

        val assetFiles = try {
            context.assets.list("") ?: emptyArray()
        } catch (_: Exception) { emptyArray() }

        val slbooks = assetFiles.filter { it.endsWith(".slbook") }
        if (slbooks.isEmpty()) return@withContext installed

        for (filename in slbooks) {
            val bookId = filename.removeSuffix(".slbook")
            val existingEntry = db.catalogDao().getById(bookId)

            // Skip if already installed and version hasn't changed
            val assetVersion = getAssetVersion(filename)
            val installedVersion = prefs.getInt("${bookId}_version", -1)
            if (existingEntry != null && existingEntry.isInstalled && installedVersion >= assetVersion) {
                android.util.Log.d("Library", "Built-in $filename already installed, skipping")
                continue
            }

            android.util.Log.d("Library", "Installing built-in: $filename")
            _downloadState.value = DownloadState.Installing(bookId, 0f, "安装内置资源：$filename…")

            try {
                val result = context.assets.open(filename).use { inputStream ->
                    SlBookParser.parse(inputStream, db, gson) { progress, msg ->
                        _downloadState.value = DownloadState.Installing(bookId, progress, msg)
                    }
                }
                result.onSuccess { meta ->
                    val bookType = try { BookType.valueOf(meta.type.uppercase()) } catch (_: Exception) { BookType.BIBLE_TEXT }
                    val lang     = BookLanguage.values().firstOrNull { it.code == meta.language } ?: BookLanguage.ZH_CN
                    db.catalogDao().upsert(BookCatalogEntry(
                        bookId       = meta.bookId,
                        title        = meta.title,
                        abbreviation = meta.abbreviation,
                        type         = bookType,
                        language     = lang,
                        testament    = meta.testament,
                        description  = meta.description,
                        copyright    = meta.copyright,
                        version      = meta.version,
                        isInstalled  = true,
                        isBuiltIn    = true,   // 内置，不可卸载
                        isDefault    = existingEntry == null,  // 第一个安装的设为默认
                        installedAt  = System.currentTimeMillis()
                    ))
                    prefs.edit().putInt("${meta.bookId}_version", meta.version).apply()
                    installed.add(meta.bookId)
                    _downloadState.value = DownloadState.Success(meta.bookId)
                    android.util.Log.d("Library", "Installed: ${meta.bookId} (${meta.title})")
                }
                result.onFailure { e ->
                    android.util.Log.e("Library", "Failed to install $filename: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Library", "Exception installing $filename: ${e.message}")
            }
        }

        resetDownloadState()
        installed
    }

    /** 从 assets 文件名读取版本号（metadata.json 里的 version 字段）*/
    private fun getAssetVersion(filename: String): Int {
        return try {
            context.assets.open(filename).use { inp ->
                val zip = java.util.zip.ZipInputStream(inp.buffered())
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "metadata.json") {
                        val text = zip.bufferedReader().readText()
                        val json = gson.fromJson(text, com.google.gson.JsonObject::class.java)
                        return json.get("version")?.asInt ?: 1
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                1
            }
        } catch (_: Exception) { 1 }
    }
}
