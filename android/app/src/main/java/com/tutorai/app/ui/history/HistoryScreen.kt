package com.tutorai.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.ui.components.DiagramThumbnail
import com.tutorai.app.ui.components.OfflineBadge
import com.tutorai.app.ui.components.TutorAiMascot
import com.tutorai.app.ui.components.TutorIcons
import com.tutorai.app.ui.theme.LocalSpacing

/**
 * History / Library. Saved lessons, newest first. Tap a row to open it for
 * offline replay (the `library/{lessonId}` route); delete from the row. Rows
 * animate in/out via `Modifier.animateItem()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpen: (String) -> Unit,
    onCreateLesson: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    val lessons by viewModel.items.collectAsState()
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
    ) { inner ->
        if (lessons.isEmpty()) {
            EmptyLibrary(onCreateLesson = onCreateLesson, modifier = Modifier.padding(inner))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                item {
                    Column(Modifier.padding(horizontal = 6.dp, vertical = spacing.s)) {
                        Text("Your library", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "${lessons.size} lesson${if (lessons.size == 1) "" else "s"} saved for offline",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.xs),
                        )
                    }
                }
                items(items = lessons, key = { it.id }) { lesson ->
                    HistoryRow(
                        lesson = lesson,
                        onOpen = { onOpen(lesson.id) },
                        onDelete = { viewModel.delete(lesson.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    lesson: Lesson,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            DiagramThumbnail(modifier = Modifier.size(84.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.l),
            ) {
                Text(
                    lesson.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${lesson.segments.size} segments · ${lesson.totalDurationMs / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                OfflineBadge(modifier = Modifier.padding(top = spacing.s))
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.Top)) {
                Icon(
                    TutorIcons.Delete,
                    contentDescription = "Delete ${lesson.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onCreateLesson: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TutorAiMascot(modifier = Modifier.size(130.dp))
        Text(
            "No saved lessons yet",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = spacing.l),
        )
        Text(
            "Lessons you save for offline will live here, ready whenever you are.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.s),
        )
        Button(
            onClick = onCreateLesson,
            modifier = Modifier.padding(top = spacing.xl).height(48.dp),
            shape = MaterialTheme.shapes.medium,
        ) { Text("Create a lesson", style = MaterialTheme.typography.labelLarge) }
    }
}
