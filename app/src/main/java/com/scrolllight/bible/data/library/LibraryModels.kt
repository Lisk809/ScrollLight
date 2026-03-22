package com.scrolllight.bible.data.library

import androidx.room.*
import com.google.gson.annotations.SerializedName

// ══════════════════════════════════════════════════════════════════════════════
//  通用寻址键  bookId:chapter:verse
//  例如：mat:17:1  gen:1:1  rev:22:21
// ══════════════════════════════════════════════════════════════════════════════

data class VerseRef(
    val bookId:  String,
    val chapter: Int,
    val verse:   Int
) {
    val key: String get() = "$bookId:$chapter:$verse"
    override fun toString() = key

    companion object {
        fun parse(key: String): VerseRef? {
            val parts = key.split(":")
            if (parts.size < 3) return null
            return VerseRef(parts[0], parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null)
        }
    }
}

data class VerseRange(val from: VerseRef, val to: VerseRef) {
    /** true if the given ref falls within this range */
    fun contains(ref: VerseRef): Boolean {
        if (ref.bookId != from.bookId) return false
        val fromFlat = from.chapter * 1000 + from.verse
        val toFlat   = to.chapter   * 1000 + to.verse
        val refFlat  = ref.chapter  * 1000 + ref.verse
        return refFlat in fromFlat..toFlat
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  书库目录  —  描述已安装/可下载的书籍
// ══════════════════════════════════════════════════════════════════════════════

/**
 * 书籍类型
 * BIBLE_TEXT    — 圣经译本正文（和合本、NIV、ESV…）
 * ORIGINAL_TEXT — 圣经原文（希伯来文BHS、希腊文NA28/LXX…）
 * STUDY_BIBLE   — 研读本注释（添加了注解的版本）
 * COMMENTARY    — 独立注释书（Matthew Henry、Calvin…）
 * LEXICON       — 词典（Strong's、BDB、BDAG…）
 */
enum class BookType {
    BIBLE_TEXT,
    ORIGINAL_TEXT,
    STUDY_BIBLE,
    COMMENTARY,
    LEXICON
}

enum class BookLanguage(val displayName: String, val code: String) {
    ZH_CN("简体中文", "zh-CN"),
    ZH_TW("繁體中文", "zh-TW"),
    EN("English", "en"),
    HE("עברית (Hebrew)", "he"),
    EL("Ελληνικά (Greek)", "el"),
    KO("한국어", "ko"),
    JA("日本語", "ja"),
    ES("Español", "es"),
    FR("Français", "fr"),
    DE("Deutsch", "de"),
    RU("Русский", "ru"),
    MULTI("多语言", "multi")
}

/** 每本书的元数据，存入数据库目录 */
@Entity(tableName = "book_catalog")
data class BookCatalogEntry(
    @PrimaryKey val bookId: String,         // 全局唯一ID, 如 "cuv", "niv", "bhs", "na28"
    val title: String,                      // 显示名称
    val abbreviation: String,               // 简写, 如 "和合本", "NIV"
    val type: BookType,
    val language: BookLanguage,
    val testament: String = "both",         // "ot" | "nt" | "both"
    val description: String = "",
    val copyright: String = "",
    val publisher: String = "",
    val version: Int = 1,                   // 版本号，用于增量更新
    val isInstalled: Boolean = false,
    val isBuiltIn: Boolean = false,         // 内置，不可删除
    val isDefault: Boolean = false,
    val downloadUrl: String = "",           // 空=内置或用户上传
    val fileSizeKb: Long = 0,
    val installedAt: Long = 0,
    val checksum: String = ""               // SHA-256 校验值
)

/** 可下载书目条目（来自服务器的 catalog.json） */
data class RemoteBookEntry(
    @SerializedName("bookId")     val bookId: String,
    @SerializedName("title")      val title: String,
    @SerializedName("abbr")       val abbreviation: String,
    @SerializedName("type")       val type: String,
    @SerializedName("language")   val language: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("downloadUrl") val downloadUrl: String,
    @SerializedName("fileSizeKb") val fileSizeKb: Long = 0,
    @SerializedName("version")    val version: Int = 1,
    @SerializedName("checksum")   val checksum: String = ""
)

// ══════════════════════════════════════════════════════════════════════════════
//  .slbook 文件格式规范
//
//  .slbook 是一个 ZIP 压缩包，内含：
//  ├── metadata.json      (BookMeta)
//  ├── books.json         (List<BibleBookMeta>) — 书卷列表
//  ├── verses.db          (SQLite, 表: verses)
//  ├── originals.db       (可选, SQLite, 表: words) — 仅原文书
//  └── notes.db           (可选, SQLite, 表: notes) — 仅研读本/注释
//
//  verses 表 schema:
//    book_id TEXT, chapter INT, verse INT, text TEXT,
//    text_secondary TEXT (如并列译本),
//    PRIMARY KEY (book_id, chapter, verse)
//
//  words 表 schema:
//    book_id TEXT, chapter INT, verse INT,
//    word_index INT, surface TEXT,
//    lemma TEXT, strongs TEXT,
//    morph TEXT, transliteration TEXT, gloss TEXT,
//    PRIMARY KEY (book_id, chapter, verse, word_index)
//
//  notes 表 schema:
//    note_id TEXT PRIMARY KEY,
//    book_id TEXT, chapter_from INT, verse_from INT,
//    chapter_to INT, verse_to INT,
//    title TEXT, content TEXT,
//    note_type TEXT (INTRODUCTION|VERSE_NOTE|CROSS_REF|WORD_STUDY),
//    tags TEXT (JSON array)
// ══════════════════════════════════════════════════════════════════════════════

data class SlBookMetadata(
    @SerializedName("bookId")      val bookId: String,
    @SerializedName("title")       val title: String,
    @SerializedName("abbr")        val abbreviation: String,
    @SerializedName("type")        val type: String,          // BookType.name
    @SerializedName("language")    val language: String,      // BookLanguage.code
    @SerializedName("testament")   val testament: String = "both",
    @SerializedName("description") val description: String = "",
    @SerializedName("copyright")   val copyright: String = "",
    @SerializedName("publisher")   val publisher: String = "",
    @SerializedName("version")     val version: Int = 1,
    @SerializedName("hasOriginals") val hasOriginals: Boolean = false,
    @SerializedName("hasNotes")    val hasNotes: Boolean = false,
    @SerializedName("checksum")    val checksum: String = ""
)

// ══════════════════════════════════════════════════════════════════════════════
//  Room 实体  —  已安装书籍的内容存入本地 Room 数据库
// ══════════════════════════════════════════════════════════════════════════════

/** 经文节 */
@Entity(
    tableName = "bible_verses",
    primaryKeys = ["bookId", "chapter", "verse", "versionId"],
    indices = [Index(value = ["bookId", "chapter", "versionId"])]
)
data class BibleVerseEntity(
    val bookId:    String,
    val chapter:   Int,
    val verse:     Int,
    val versionId: String,     // 对应 BookCatalogEntry.bookId
    val text:      String,
    val textSecondary: String = ""   // 可选的辅助译文
)

/** 原文单词（逐字级，带 Strong 编号） */
@Entity(
    tableName = "original_words",
    primaryKeys = ["bookId", "chapter", "verse", "wordIndex", "versionId"],
    indices = [Index(value = ["bookId", "chapter", "verse", "versionId"])]
)
data class OriginalWordEntity(
    val bookId:          String,
    val chapter:         Int,
    val verse:           Int,
    val wordIndex:       Int,
    val versionId:       String,       // "bhs" 或 "na28"
    val surface:         String,       // 原文字符
    val lemma:           String = "",  // 词根形式
    val strongs:         String = "",  // 如 "H1234" 或 "G5678"
    val morph:           String = "",  // 形态编码（如 OSHM = Noun-masculine-singular）
    val transliteration: String = "",  // 音译
    val gloss:           String = ""   // 简短释义（中文）
)

/** 研读注释 */
@Entity(
    tableName = "study_notes",
    indices = [
        Index(value = ["bookId", "chapterFrom", "verseFrom", "versionId"]),
        Index(value = ["versionId"])
    ]
)
data class StudyNoteEntity(
    @PrimaryKey val noteId:     String,   // "{versionId}:{bookId}:{ch}:{v}"
    val versionId:   String,              // 研读本 ID
    val bookId:      String,
    val chapterFrom: Int,
    val verseFrom:   Int,
    val chapterTo:   Int,
    val verseTo:     Int,
    val title:       String = "",
    val content:     String,              // Markdown 格式
    val noteType:    NoteType = NoteType.VERSE_NOTE,
    val tagsJson:    String = "[]"        // JSON array of tags
)

enum class NoteType {
    INTRODUCTION,    // 书卷或章节引言
    VERSE_NOTE,      // 节注（最常见）
    CROSS_REF,       // 交叉参考
    WORD_STUDY,      // 原文词汇研究
    THEME_NOTE       // 主题注释
}

/** Strongs 词典条目（跨所有版本共享） */
@Entity(tableName = "strongs_lexicon", primaryKeys = ["strongsId"])
data class StrongsEntry(
    val strongsId:    String,    // "H1234" 或 "G5678"
    val original:     String,    // 希伯来文/希腊文
    val transliteration: String,
    val definition:   String,    // 中文释义
    val definitionEn: String = "",
    val usage:        String = "",
    val language:     String    // "he" 或 "el"
)

// ══════════════════════════════════════════════════════════════════════════════
//  阅读模式
// ══════════════════════════════════════════════════════════════════════════════

enum class ReadingLayout {
    SINGLE,         // 单栏（默认）
    PARALLEL,       // 双栏平行对照（两个译本）
    ORIGINAL,       // 正文 + 原文逐字
    STUDY,          // 正文 + 研读注释侧边栏
    CARD            // 纸板模式：每节一张卡片，可左右滑动
}

data class ReadingSession(
    val primaryVersionId:   String = "cuv",
    val secondaryVersionId: String? = null,   // 平行对照版本
    val originalVersionId:  String? = null,   // 原文版本（"bhs" 或 "na28"）
    val studyVersionId:     String? = null,   // 研读本版本
    val layout: ReadingLayout = ReadingLayout.SINGLE,
    val currentRef: VerseRef = VerseRef("mat", 1, 1)
)
