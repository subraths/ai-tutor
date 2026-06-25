package com.tutorai.app.domain.model

/** Progress of a lesson generation request (mirrors the backend's async job model). */
sealed interface GenerationStatus {
    data class InProgress(val stage: String, val progress: Int) : GenerationStatus
    data class Completed(val lesson: Lesson) : GenerationStatus
    data class Failed(val message: String) : GenerationStatus
}
