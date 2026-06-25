package com.tutorai.app.ui.topic

import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.ui.player.buildSvgHtml

@Composable
fun TopicScreen(
    viewModel: TopicViewModel,
    onPlayLesson: (String) -> Unit,
    onShowHistory: () -> Unit,
) {
    val topic by viewModel.topic.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TutorAI", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onShowHistory) { Text("History") }
        }
        Text(
            "Type a topic to generate an interactive visual lesson.",
            style = MaterialTheme.typography.bodyMedium,
        )

        SvgRenderTest()

        OutlinedTextField(
            value = topic,
            onValueChange = viewModel::onTopicChange,
            label = { Text("Topic") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val generating = state is TopicUiState.Generating
        Button(
            onClick = viewModel::generate,
            enabled = topic.isNotBlank() && !generating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Generate lesson")
        }

        when (val s = state) {
            is TopicUiState.Idle -> Unit
            is TopicUiState.Generating -> GeneratingView(s)
            is TopicUiState.Error -> ErrorView(s.message, onRetry = viewModel::reset)
            is TopicUiState.Success -> LessonView(
                lesson = s.lesson,
                saveState = saveState,
                onPlay = { onPlayLesson(s.lesson.id) },
                onSave = { viewModel.save(s.lesson) },
            )
        }
    }
}

/** TEMPORARY diagnostic: renders a hardcoded SVG via the same buildSvgHtml +
 *  WebView path as the player, to confirm WebView SVG rendering works at all. */
@Composable
private fun SvgRenderTest() {
    val dummySvg = """
        <svg viewBox="0 0 300 150" xmlns="http://www.w3.org/2000/svg" font-family="Arial">
          <rect x="6" y="6" width="288" height="138" rx="12" fill="#e8f0fe" stroke="#1976d2" stroke-width="3"/>
          <circle id="dot" cx="70" cy="75" r="42" fill="#ff9800"/>
          <text x="135" y="68" font-size="22" fill="#333">SVG renders</text>
          <text x="135" y="100" font-size="22" fill="#2e7d32">correctly</text>
        </svg>
    """.trimIndent()
    val html = remember { buildSvgHtml(dummySvg) }
    Text("SVG render test (temporary):", style = MaterialTheme.typography.labelMedium)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.domStorageEnabled = true
                setBackgroundColor(Color.WHITE)
                webViewClient = WebViewClient()
                loadDataWithBaseURL(
                    "https://appassets.androidplatform.net/",
                    html, "text/html", "utf-8", null,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().height(200.dp),
    )
}

@Composable
private fun GeneratingView(state: TopicUiState.Generating) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Generating… ${state.stage} (${state.progress}%)")
        LinearProgressIndicator(
            progress = { state.progress / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Error: $message", color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) { Text("Dismiss") }
    }
}

@Composable
private fun LessonView(
    lesson: Lesson,
    saveState: SaveState,
    onPlay: () -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(lesson.title, style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f)) { Text("▶ Play") }
                Button(
                    onClick = onSave,
                    enabled = saveState !is SaveState.Saving && saveState !is SaveState.Saved,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (saveState) {
                            is SaveState.Saving -> "Saving…"
                            is SaveState.Saved -> "Saved ✓"
                            else -> "⭳ Save offline"
                        }
                    )
                }
            }
            if (saveState is SaveState.Error) {
                Text("Save failed: ${saveState.message}", color = MaterialTheme.colorScheme.error)
            }

            Text(
                "${lesson.segments.size} segments • ${lesson.totalDurationMs / 1000}s",
                style = MaterialTheme.typography.labelMedium,
            )
            lesson.segments.forEach { segment ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "[${segment.index}] ${segment.durationMs} ms — " +
                            "highlights: ${segment.svgElementIds.joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(segment.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
