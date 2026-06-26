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
 * Source-agnostic: [loader] returns (lesson, svgMarkup). The factory wires either
 * a remote loader (fetch manifest + svg over HTTP) or a local one (read from the
 * on-device library), so the same player serves online and offline lessons.
 *
 * [saver], when provided, lets the player save a just-generated (remote) lesson
 * for offline replay. Offline lessons loaded from the library pass `saver = null`
 * and start out already-saved. [savedChecker] refreshes the saved state on load
 * (remote lessons are auto-saved at generation, so they are usually already on
 * device by the time the player opens).
 */
class PlayerViewModel(
    private val loader: suspend () -> Pair<Lesson, String>,
    private val saver: (suspend (Lesson) -> Unit)? = null,
    private val savedChecker: (suspend () -> Boolean)? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    // No saver ⇒ this lesson is already on-device (library replay).
    private val _savedOffline = MutableStateFlow(saver == null)
    val savedOffline: StateFlow<Boolean> = _savedOffline.asStateFlow()

    private var loadedLesson: Lesson? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = PlayerUiState.Loading
            try {
                val (lesson, svg) = loader()
                loadedLesson = lesson
                _state.value = PlayerUiState.Ready(lesson, svg)
                savedChecker?.let { _savedOffline.value = it() }
            } catch (e: Exception) {
                _state.value = PlayerUiState.Error(e.message ?: "Failed to load lesson")
            }
        }
    }

    fun saveOffline() {
        val save = saver ?: return
        val lesson = loadedLesson ?: return
        if (_savedOffline.value) return
        viewModelScope.launch {
            try {
                save(lesson)
                _savedOffline.value = true
            } catch (_: Exception) {
                // Leave savedOffline false so the user can retry the save.
            }
        }
    }

    companion object {
        fun factory(
            saver: (suspend (Lesson) -> Unit)? = null,
            savedChecker: (suspend () -> Boolean)? = null,
            loader: suspend () -> Pair<Lesson, String>,
        ) = viewModelFactory {
            initializer { PlayerViewModel(loader, saver, savedChecker) }
        }
    }
}
