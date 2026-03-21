package com.scrolllight.bible.data.model

import com.google.gson.annotations.SerializedName

// ─── Bible Structure ──────────────────────────────────────────────────────────

data class BibleData(
    @SerializedName("books") val books: List<BibleBook>
)

data class BibleBook(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("abbr") val abbr: String,
    @SerializedName("testament") val testament: String, // "old" or "new"
    @SerializedName("chapters") val chapterCount: Int,
    @SerializedName("content") val content: Map<String, List<BibleVerse>> = emptyMap()
) {
    val isOldTestament: Boolean get() = testament == "old"
    val isNewTestament: Boolean get() = testament == "new"
}

data class BibleVerse(
    @SerializedName("v") val verse: Int,
    @SerializedName("t") val text: String,
    @SerializedName("t_en") val textEn: String? = null // NIV parallel text
)

// ─── Reading State ────────────────────────────────────────────────────────────

data class VerseRef(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int
) {
    fun toDisplayString(): String = "$bookName $chapter:$verse"
    fun toShortString(): String = "$bookName $chapter"
}

data class DailyVerse(
    val theme: String,
    val subTheme: String,
    val text: String,
    val reference: String,
    val date: String
)

// ─── Reading Plans ────────────────────────────────────────────────────────────

data class ReadingPlan(
    val id: String,
    val title: String,
    val subtitle: String?,
    val days: Int,
    val category: PlanCategory,
    val coverDescription: String,
    val description: String,
    val readings: List<PlanReading> = emptyList()
)

enum class PlanCategory(val displayName: String) {
    WHOLE_BIBLE("整本圣经"),
    NEW_TESTAMENT("新约书卷"),
    OLD_TESTAMENT("旧约书卷"),
    THEME("按主题"),
    CUSTOM("自定义")
}

data class PlanReading(
    val day: Int,
    val bookId: String,
    val chapter: Int,
    val verseStart: Int = 1,
    val verseEnd: Int? = null
)

// ─── Search ───────────────────────────────────────────────────────────────────

data class SearchResult(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val matchStart: Int,
    val matchEnd: Int
)

enum class SearchScope(val displayName: String) {
    WHOLE_BIBLE("整本圣经"),
    OLD_TESTAMENT("旧约"),
    NEW_TESTAMENT("新约"),
    GOSPELS("四福音"),
    LAW("律法书"),
    PSALMS("诗歌书")
}
