package com.tutorai.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tutorai.app.domain.model.GenerationStatus
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.repository.LibraryRepository
import com.tutorai.app.domain.usecase.GenerateLessonUseCase
import com.tutorai.app.domain.usecase.SaveLessonUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_RECENT = 3

class TopicViewModel(
    private val generateLesson: GenerateLessonUseCase,
    private val saveLesson: SaveLessonUseCase,
    library: LibraryRepository,
) : ViewModel() {

    private val _topic = MutableStateFlow("")
    val topic: StateFlow<String> = _topic.asStateFlow()

    private val _uiState = MutableStateFlow<TopicUiState>(TopicUiState.Idle)
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /** The most recent auto-saved lessons (newest first, capped), for the home screen. */
    val recentLessons: StateFlow<List<Lesson>> = library.history()
        .map { it.take(MAX_RECENT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onTopicChange(value: String) {
        _topic.value = value
    }

    fun generate() {
        val topic = _topic.value.trim()
        if (topic.isEmpty()) return
        _saveState.value = SaveState.Idle
        viewModelScope.launch {
            generateLesson(topic).collect { status ->
                when (status) {
                    is GenerationStatus.InProgress ->
                        _uiState.value = TopicUiState.Generating(status.stage, status.progress)
                    is GenerationStatus.Completed -> {
                        _uiState.value = TopicUiState.Success(status.lesson)
                        // Persist every generated lesson to the device automatically.
                        save(status.lesson)
                    }
                    is GenerationStatus.Failed ->
                        _uiState.value = TopicUiState.Error(status.message)
                }
            }
        }
    }

    /** Save a lesson for offline use. Called automatically on generation, and
     *  again by the UI's "Retry" affordance if the automatic save failed. */
    fun save(lesson: Lesson) {
        if (_saveState.value == SaveState.Saving || _saveState.value == SaveState.Saved) return
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            _saveState.value = try {
                saveLesson(lesson)
                SaveState.Saved
            } catch (e: Exception) {
                SaveState.Error(e.message ?: "Save failed")
            }
        }
    }

    /** Return to the search/idle state (the "back" action on a generated lesson). */
    fun reset() {
        _uiState.value = TopicUiState.Idle
        _saveState.value = SaveState.Idle
    }

    companion object {
        fun factory(
            generateUseCase: GenerateLessonUseCase,
            saveUseCase: SaveLessonUseCase,
            library: LibraryRepository,
        ) = viewModelFactory {
            initializer { TopicViewModel(generateUseCase, saveUseCase, library) }
        }
    }
}
