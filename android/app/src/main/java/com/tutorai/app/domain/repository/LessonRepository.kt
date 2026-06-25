package com.tutorai.app.domain.repository

import com.tutorai.app.domain.model.GenerationStatus
import com.tutorai.app.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    /** Request a lesson for [topic], emitting progress until it completes or fails. */
    fun generateLesson(topic: String): Flow<GenerationStatus>

    /** Fetch a completed lesson's manifest (with absolute asset URLs). */
    suspend fun getLesson(lessonId: String): Lesson

    /** Fetch the raw SVG markup for a lesson from its (absolute) URL. */
    suspend fun getSvg(svgUrl: String): String
}
