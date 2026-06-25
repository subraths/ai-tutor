package com.tutorai.app.ui.player

import com.tutorai.app.domain.model.Lesson

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(val lesson: Lesson, val svg: String) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
