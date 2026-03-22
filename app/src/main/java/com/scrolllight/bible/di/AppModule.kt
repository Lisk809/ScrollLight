package com.scrolllight.bible.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.scrolllight.bible.ai.*
import com.scrolllight.bible.data.db.*
import com.scrolllight.bible.data.library.*
import com.scrolllight.bible.ui.theme.ThemeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ScrollLightDatabase =
        Room.databaseBuilder(ctx, ScrollLightDatabase::class.java, "scrolllight.db")
            .fallbackToDestructiveMigration().build()

    @Provides @Singleton
    fun provideLibraryDatabase(@ApplicationContext ctx: Context): LibraryDatabase =
        Room.databaseBuilder(ctx, LibraryDatabase::class.java, "scrolllight_library.db")
            .fallbackToDestructiveMigration().build()

    @Provides fun provideCatalogDao(db: LibraryDatabase)      = db.catalogDao()
    @Provides fun provideVerseLibDao(db: LibraryDatabase)     = db.verseDao()
    @Provides fun provideOriginalWordDao(db: LibraryDatabase) = db.originalWordDao()
    @Provides fun provideStudyNoteDao(db: LibraryDatabase)    = db.studyNoteDao()
    @Provides fun provideStrongsDao(db: LibraryDatabase)      = db.strongsDao()

    @Provides @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @Provides fun provideHighlightDao(db: ScrollLightDatabase) = db.highlightDao()
    @Provides fun provideNoteDao(db: ScrollLightDatabase)      = db.noteDao()
    @Provides fun provideBookmarkDao(db: ScrollLightDatabase)  = db.bookmarkDao()
    @Provides fun provideProgressDao(db: ScrollLightDatabase)  = db.readingProgressDao()

    @Provides @Singleton fun provideGson(): Gson = Gson()

    // AI
    @Provides @Singleton fun provideAiController()                                       = AiController()
    @Provides @Singleton fun provideAiApiClient(gson: Gson)                              = AiApiClient(gson)
    @Provides @Singleton fun provideAiConfigRepo(@ApplicationContext ctx: Context)       = AiConfigRepository(ctx)
    @Provides @Singleton fun provideToolExecutor(ctrl: AiController, gson: Gson)         = ToolExecutor(ctrl, gson)

    // Linked reading config
    @Provides @Singleton fun provideLinkedReadingConfigRepository(@ApplicationContext ctx: Context) = com.scrolllight.bible.data.library.LinkedReadingConfigRepository(ctx)

    // Theme
    @Provides @Singleton fun provideThemeRepository(@ApplicationContext ctx: Context)    = ThemeRepository(ctx)
}
