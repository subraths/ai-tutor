package com.tutorai.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tutorai.app.domain.model.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Source-agnostic: [loader] returns (lesson, svgMarkup). The factory wires either a
 * remote loader (fetch manifest + svg over HTTP) or a local one (read from the
 * on-device library), so the same player serves online and offline lessons.
 */
class PlayerViewModel(
    private val loader: suspend () -> Pair<Lesson, String>,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = PlayerUiState.Loading
            try {
                val (lesson, svg) = loader()
                _state.value = PlayerUiState.Ready(lesson, svg)
            } catch (e: Exception) {
                _state.value = PlayerUiState.Error(e.message ?: "Failed to load lesson")
            }
        }
    }

    companion object {
        fun factory(loader: suspend () -> Pair<Lesson, String>) = viewModelFactory {
            initializer { PlayerViewModel(loader) }
        }
    }
}
