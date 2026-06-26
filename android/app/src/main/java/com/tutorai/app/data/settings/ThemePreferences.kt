package com.tutorai.app.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User-selectable app theme. SYSTEM follows the device dark/light setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Lightweight persistence for the app's theme preference. Backed by
 * SharedPreferences (a single enum value), exposed reactively so the Activity
 * can recompose the whole tree when the user changes it on the Settings screen.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tutor_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(read())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun read(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, null) ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
