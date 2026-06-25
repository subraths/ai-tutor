package com.tutorai.app.ui.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tutorai.app.domain.model.Lesson

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
