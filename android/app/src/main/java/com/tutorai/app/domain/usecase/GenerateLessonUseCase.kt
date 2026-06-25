package com.tutorai.app.domain.usecase

import com.tutorai.app.domain.model.GenerationStatus
import com.tutorai.app.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow

class GenerateLessonUseCase(private val repository: LessonRepository) {
    operator fun invoke(topic: String): Flow<GenerationStatus> =
        repository.generateLesson(topic.trim())
}
