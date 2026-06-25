package com.tutorai.app.domain.repository

import com.tutorai.app.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

/** On-device library of saved lessons (offline storage + history). */
interface LibraryRepository {
    /** Download the lesson's svg + audio assets and persist it for offline replay. */
    suspend fun save(lesson: Lesson)

    /** Saved lessons, newest first. */
    fun history(): Flow<List<Lesson>>

    /** Load a saved lesson (local file URIs) plus its SVG markup. */
    suspend fun load(lessonId: String): Pair<Lesson, String>

    suspend fun delete(lessonId: String)

    suspend fun isSaved(lessonId: String): Boolean
}
