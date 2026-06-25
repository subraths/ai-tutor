package com.tutorai.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tutorai.app.domain.model.GenerationStatus
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.usecase.GenerateLessonUseCase
import com.tutorai.app.domain.usecase.SaveLessonUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopicViewModel(
    private val generateLesson: GenerateLessonUseCase,
    private val saveLesson: SaveLessonUseCase,
) : ViewModel() {

    private val _topic = MutableStateFlow("")
    val topic: StateFlow<String> = _topic.asStateFlow()

    private val _uiState = MutableStateFlow<TopicUiState>(TopicUiState.Idle)
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun onTopicChange(value: String) {
        _topic.value = value
    }

    fun generate() {
        val topic = _topic.value.trim()
        if (topic.isEmpty()) return
        _saveState.value = SaveState.Idle
        viewModelScope.launch {
            generateLesson(topic).collect { status ->
                _uiState.value = when (status) {
                    is GenerationStatus.InProgress ->
                        TopicUiState.Generating(status.stage, status.progress)
                    is GenerationStatus.Completed ->
                        TopicUiState.Success(status.lesson)
                    is GenerationStatus.Failed ->
                        TopicUiState.Error(status.message)
                }
            }
        }
    }

    fun save(lesson: Lesson) {
        if (_saveState.value == SaveState.Saving) return
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

    fun reset() {
        _uiState.value = TopicUiState.Idle
    }

    companion object {
        fun factory(
            generateUseCase: GenerateLessonUseCase,
            saveUseCase: SaveLessonUseCase,
        ) = viewModelFactory {
            initializer { TopicViewModel(generateUseCase, saveUseCase) }
        }
    }
}
