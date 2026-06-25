package com.tutorai.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val library: LibraryRepository) : ViewModel() {

    val items: StateFlow<List<Lesson>> = library.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(lessonId: String) {
        viewModelScope.launch { library.delete(lessonId) }
    }

    companion object {
        fun factory(library: LibraryRepository) = viewModelFactory {
            initializer { HistoryViewModel(library) }
        }
    }
}
