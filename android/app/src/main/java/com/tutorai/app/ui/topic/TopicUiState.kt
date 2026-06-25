package com.tutorai.app.ui.topic

import com.tutorai.app.domain.model.Lesson

sealed interface TopicUiState {
    data object Idle : TopicUiState
    data class Generating(val stage: String, val progress: Int) : TopicUiState
    data class Success(val lesson: Lesson) : TopicUiState
    data class Error(val message: String) : TopicUiState
}
