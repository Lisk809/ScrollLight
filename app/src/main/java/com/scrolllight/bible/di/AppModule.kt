package com.scrolllight.bible.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.scrolllight.bible.ai.AiApiClient
import com.scrolllight.bible.ai.AiConfigRepository
import com.scrolllight.bible.ai.AiController
import com.scrolllight.bible.ai.ToolExecutor
import com.scrolllight.bible.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ScrollLightDatabase =
        Room.databaseBuilder(ctx, ScrollLightDatabase::class.java, "scrolllight.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideHighlightDao(db: ScrollLightDatabase) = db.highlightDao()
    @Provides fun provideNoteDao(db: ScrollLightDatabase) = db.noteDao()
    @Provides fun provideBookmarkDao(db: ScrollLightDatabase) = db.bookmarkDao()
    @Provides fun provideProgressDao(db: ScrollLightDatabase) = db.readingProgressDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    // AI
    @Provides @Singleton fun provideAiController() = AiController()
    @Provides @Singleton fun provideAiApiClient(gson: Gson) = AiApiClient(gson)
    @Provides @Singleton fun provideAiConfigRepository(@ApplicationContext ctx: Context) = AiConfigRepository(ctx)
    @Provides @Singleton fun provideToolExecutor(ctrl: AiController, gson: Gson) = ToolExecutor(ctrl, gson)
}

