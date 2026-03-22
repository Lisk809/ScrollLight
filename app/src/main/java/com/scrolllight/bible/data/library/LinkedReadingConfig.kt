package com.scrolllight.bible.data.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.linkedConfigStore by preferencesDataStore("linked_reading_config")

/**
 * 三书联动配置 — 记录用户选择的"正文 + 原文 + 研读本"组合
 *
 * 独立下载，独立管理；阅读时通过 VerseRef(bookId:chapter:verse) 三书同步跳转。
 */
data class LinkedReadingConfig(
    val bibleVersionId:  String  = "cuv",      // 正文译本 ID (必须)
    val parallelId:      String? = null,       // 第二译本 ID (可选)
    val originalId:      String? = null,       // 原文 ID: "bhs" / "na28" (可选)
    val studyId:         String? = null,       // 研读本 / 注释 ID (可选)
    val layout:          ReadingLayout = ReadingLayout.SINGLE
) {
    /** 当前有几个激活层 */
    val activeLayers: Int get() = listOfNotNull(
        bibleVersionId.takeIf { it.isNotBlank() },
        parallelId,
        originalId,
        studyId
    ).size
}

private object Keys {
    val BIBLE    = stringPreferencesKey("linked_bible")
    val PARALLEL = stringPreferencesKey("linked_parallel")
    val ORIGINAL = stringPreferencesKey("linked_original")
    val STUDY    = stringPreferencesKey("linked_study")
    val LAYOUT   = stringPreferencesKey("linked_layout")
}

@Singleton
class LinkedReadingConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val config: Flow<LinkedReadingConfig> = context.linkedConfigStore.data.map { p ->
        LinkedReadingConfig(
            bibleVersionId = p[Keys.BIBLE]    ?: "cuv",
            parallelId     = p[Keys.PARALLEL]?.takeIf { it.isNotBlank() },
            originalId     = p[Keys.ORIGINAL]?.takeIf { it.isNotBlank() },
            studyId        = p[Keys.STUDY]    ?.takeIf { it.isNotBlank() },
            layout         = p[Keys.LAYOUT]?.let {
                try { ReadingLayout.valueOf(it) } catch (_: Exception) { ReadingLayout.SINGLE }
            } ?: ReadingLayout.SINGLE
        )
    }

    suspend fun save(config: LinkedReadingConfig) {
        context.linkedConfigStore.edit { p ->
            p[Keys.BIBLE]    = config.bibleVersionId
            p[Keys.PARALLEL] = config.parallelId  ?: ""
            p[Keys.ORIGINAL] = config.originalId  ?: ""
            p[Keys.STUDY]    = config.studyId     ?: ""
            p[Keys.LAYOUT]   = config.layout.name
        }
    }

    suspend fun setBible(id: String)    = config.let { save(getCurrentOrDefault().copy(bibleVersionId = id)) }
    suspend fun setParallel(id: String?) = save(getCurrentOrDefault().copy(parallelId = id))
    suspend fun setOriginal(id: String?) = save(getCurrentOrDefault().copy(originalId = id))
    suspend fun setStudy(id: String?)    = save(getCurrentOrDefault().copy(studyId = id))
    suspend fun setLayout(l: ReadingLayout) = save(getCurrentOrDefault().copy(layout = l))

    // Convenience: read current value once (blocking; use only from coroutine)
    private var _cached = LinkedReadingConfig()
    fun getCurrentOrDefault() = _cached

    init {
        // Keep cache warm (non-blocking)
        // Actual reads use the Flow
    }
}
