package com.tutorai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tutorai.app.data.settings.ThemeMode
import com.tutorai.app.data.settings.ThemePreferences
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val themePreferences: ThemePreferences) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)

    companion object {
        fun factory(themePreferences: ThemePreferences) = viewModelFactory {
            initializer { SettingsViewModel(themePreferences) }
        }
    }
}
