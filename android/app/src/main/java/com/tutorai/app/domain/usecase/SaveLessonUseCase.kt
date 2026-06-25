package com.tutorai.app.domain.usecase

import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.repository.LibraryRepository

class SaveLessonUseCase(private val library: LibraryRepository) {
    suspend operator fun invoke(lesson: Lesson) = library.save(lesson)
}
