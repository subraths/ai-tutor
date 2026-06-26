package com.tutorai.app.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tutorai.app.data.local.LessonFileStore
import com.tutorai.app.data.local.TutorDatabase
import com.tutorai.app.data.remote.TutorApi
import com.tutorai.app.data.settings.ThemePreferences
import com.tutorai.app.data.repository.LessonRepositoryImpl
import com.tutorai.app.data.repository.LibraryRepositoryImpl
import com.tutorai.app.domain.repository.LessonRepository
import com.tutorai.app.domain.repository.LibraryRepository
import com.tutorai.app.domain.usecase.GenerateLessonUseCase
import com.tutorai.app.domain.usecase.SaveLessonUseCase
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Manual dependency-injection container (composition root). Created once by
 * [com.tutorai.app.TutorApplication]. A clean seam to swap for Hilt later.
 */
class AppContainer(context: Context, baseUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: TutorApi = retrofit.create(TutorApi::class.java)

    private val database = Room.databaseBuilder(
        context.applicationContext,
        TutorDatabase::class.java,
        "tutor.db",
    ).build()

    private val fileStore = LessonFileStore(context.applicationContext.filesDir)

    val lessonRepository: LessonRepository = LessonRepositoryImpl(api, baseUrl)
    val libraryRepository: LibraryRepository =
        LibraryRepositoryImpl(api, database.lessonDao(), fileStore, json)

    val generateLessonUseCase = GenerateLessonUseCase(lessonRepository)
    val saveLessonUseCase = SaveLessonUseCase(libraryRepository)

    val themePreferences = ThemePreferences(context)
}
