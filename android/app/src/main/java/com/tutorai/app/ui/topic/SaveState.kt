package com.tutorai.app.ui.topic

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Saved : SaveState
    data class Error(val message: String) : SaveState
}
