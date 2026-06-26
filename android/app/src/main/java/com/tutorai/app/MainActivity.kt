package com.tutorai.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tutorai.app.data.settings.ThemeMode
import com.tutorai.app.ui.navigation.TutorNavHost
import com.tutorai.app.ui.theme.TutorAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars; Compose Scaffold insets handle padding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TutorApplication).container
        setContent {
            val themeMode by container.themePreferences.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep system-bar icon contrast in step with the chosen theme.
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            TutorAiTheme(darkTheme = darkTheme) {
                TutorNavHost(container = container)
            }
        }
    }
}
