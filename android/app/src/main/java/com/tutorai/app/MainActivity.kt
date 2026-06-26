package com.tutorai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tutorai.app.ui.navigation.TutorNavHost
import com.tutorai.app.ui.theme.TutorAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars; Compose Scaffold insets handle padding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TutorApplication).container
        setContent {
            TutorAiTheme {
                TutorNavHost(container = container)
            }
        }
    }
}
