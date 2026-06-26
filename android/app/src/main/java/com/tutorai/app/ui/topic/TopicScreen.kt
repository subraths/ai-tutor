package com.tutorai.app.ui.topic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.ui.components.DiagramThumbnail
import com.tutorai.app.ui.components.GenerationProgress
import com.tutorai.app.ui.components.SectionEyebrow
import com.tutorai.app.ui.components.SuggestionChip
import com.tutorai.app.ui.components.TutorAiMascot
import com.tutorai.app.ui.components.TutorIcons
import com.tutorai.app.ui.theme.LocalExtendedColors
import com.tutorai.app.ui.theme.LocalSpacing
import com.tutorai.app.ui.theme.ScholarShapeTokens

/** Staged checklist for the generation state (the design's fixed 4-step flow). */
private val GenerationStages = listOf(
    "Understanding the topic",
    "Drawing the diagram",
    "Writing the narration",
    "Recording the audio",
)

/**
 * Home / Topic. One screen, four states (Idle · Generating · Success · Error)
 * cross-faded with [AnimatedContent]. A large flexible top app bar collapses on
 * scroll. State comes from [TopicViewModel] unchanged.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicScreen(
    viewModel: TopicViewModel,
    onPlayLesson: (String) -> Unit,
    onOpenSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = listOf("Photosynthesis", "The water cycle", "Pythagoras", "Black holes"),
    reducedMotion: Boolean = false,
    bottomBar: @Composable () -> Unit = {},
) {
    val topic by viewModel.topic.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val recentLessons by viewModel.recentLessons.collectAsState()

    val spacing = LocalSpacing.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    // A back affordance once a lesson is generated (or errored): return to search.
    val showBack = state is TopicUiState.Success || state is TopicUiState.Error

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("TutorAI", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = viewModel::reset) {
                            Icon(TutorIcons.Back, contentDescription = "Back to search")
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
    ) { inner ->
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                if (reducedMotion) fadeIn() togetherWith fadeOut()
                else (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut()
            },
            contentKey = { it::class },
            label = "homeState",
        ) { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenH),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    is TopicUiState.Idle -> IdleBody(
                        topic = topic,
                        onTopicChange = viewModel::onTopicChange,
                        onGenerate = viewModel::generate,
                        onPickSuggestion = { picked ->
                            viewModel.onTopicChange(picked)
                            viewModel.generate()
                        },
                        suggestions = suggestions,
                        recentLessons = recentLessons,
                        onOpenLesson = onOpenSaved,
                    )
                    is TopicUiState.Generating -> GenerationProgress(
                        percent = s.progress,
                        stageIndex = stageIndexFor(s.stage, s.progress),
                        stages = GenerationStages,
                        topic = topic,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.padding(top = spacing.l),
                    )
                    is TopicUiState.Success -> LessonResultCard(
                        lesson = s.lesson,
                        saveState = saveState,
                        onPlay = { onPlayLesson(s.lesson.id) },
                        onSaveOffline = { viewModel.save(s.lesson) },
                    )
                    is TopicUiState.Error -> ErrorBody(message = s.message, onRetry = viewModel::generate)
                }
                Spacer(Modifier.height(spacing.xxxl))
            }
        }
    }
}

/** Map the backend's free-form stage label to the fixed checklist index. */
private fun stageIndexFor(stage: String, progress: Int): Int {
    val byName = GenerationStages.indexOfFirst { it.equals(stage.trim(), ignoreCase = true) }
    if (byName >= 0) return byName
    // Fall back to a progress-derived bucket for unknown stage labels.
    return (progress / 25).coerceIn(0, GenerationStages.lastIndex)
}

private fun durationLabel(totalDurationMs: Int): String {
    val seconds = totalDurationMs / 1000
    return if (seconds < 60) "~${seconds}s" else "~${(seconds + 30) / 60} min"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.IdleBody(
    topic: String,
    onTopicChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onPickSuggestion: (String) -> Unit,
    suggestions: List<String>,
    recentLessons: List<Lesson>,
    onOpenLesson: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    TutorAiMascot(
        modifier = Modifier
            .padding(top = spacing.s)
            .size(150.dp),
    )
    Text(
        text = "What shall we learn today?",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = spacing.s),
    )
    Text(
        text = "Name any topic and I'll draw it, narrate it, and walk you through — step by step.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = spacing.s, start = spacing.l, end = spacing.l),
    )

    OutlinedTextField(
        value = topic,
        onValueChange = onTopicChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.xl),
        placeholder = { Text("Try “Photosynthesis”") },
        leadingIcon = { Icon(TutorIcons.Search, contentDescription = null) },
        trailingIcon = {
            FilledIconButton(
                onClick = onGenerate,
                enabled = topic.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) { Icon(TutorIcons.Forward, contentDescription = "Generate lesson") }
        },
        singleLine = true,
        shape = ScholarShapeTokens.Field,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { if (topic.isNotBlank()) onGenerate() }),
    )

    if (recentLessons.isNotEmpty()) {
        SectionEyebrow(
            text = "Recent lessons",
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = spacing.xl, bottom = spacing.m),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            recentLessons.forEach { lesson ->
                RecentLessonRow(lesson = lesson, onClick = { onOpenLesson(lesson.id) })
            }
        }
    }

    // Suggestions only fill in until the learner has a few lessons of their own.
    if (recentLessons.size < 3) {
        SectionEyebrow(
            text = "Try one of these",
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = spacing.xl, bottom = spacing.m),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            suggestions.forEach { s ->
                SuggestionChip(label = s, onClick = { onPickSuggestion(s) })
            }
        }
    }
}

/** Compact, tappable row for a recently generated (and auto-saved) lesson. */
@Composable
private fun RecentLessonRow(lesson: Lesson, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.s),
        ) {
            DiagramThumbnail(modifier = Modifier.size(52.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.m),
            ) {
                Text(
                    lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${lesson.segments.size} segments · ${lesson.totalDurationMs / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                TutorIcons.Play,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    val spacing = LocalSpacing.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = spacing.huge)) {
        Surface(
            shape = ScholarShapeTokens.LessonCard,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(74.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(TutorIcons.Error, contentDescription = null, modifier = Modifier.size(36.dp))
            }
        }
        Text(
            "That didn't go through",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = spacing.l),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.s, start = spacing.l, end = spacing.l),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = spacing.xl)
                .height(50.dp),
            shape = ScholarShapeTokens.Field,
        ) {
            Icon(TutorIcons.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(spacing.s))
            Text("Try again", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Success state — the generated lesson, ready to play or save. */
@Composable
private fun LessonResultCard(
    lesson: Lesson,
    saveState: SaveState,
    onPlay: () -> Unit,
    onSaveOffline: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = spacing.s)) {
        // "Lesson ready" pill
        Surface(
            shape = ScholarShapeTokens.Pill,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = spacing.m, vertical = 7.dp),
            ) {
                Icon(TutorIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(spacing.s))
                Text("Lesson ready", style = MaterialTheme.typography.labelMedium)
            }
        }

        Surface(
            shape = ScholarShapeTokens.LessonCard,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.m),
        ) {
            Column {
                DiagramThumbnail(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp),
                )
                Column(Modifier.padding(spacing.l)) {
                    Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.m),
                        modifier = Modifier.padding(top = spacing.s),
                    ) {
                        MetaItem(TutorIcons.Segments, "${lesson.segments.size} segments")
                        MetaItem(TutorIcons.Clock, durationLabel(lesson.totalDurationMs))
                    }
                    Button(
                        onClick = onPlay,
                        modifier = Modifier
                            .padding(top = spacing.l)
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = ScholarShapeTokens.Field,
                    ) {
                        Icon(TutorIcons.Play, contentDescription = null)
                        Spacer(Modifier.width(spacing.s))
                        Text("Play lesson", style = MaterialTheme.typography.labelLarge)
                    }
                    // Lessons are saved to the device automatically — show status only.
                    AutoSaveStatus(saveState = saveState, onRetry = onSaveOffline)
                }
            }
        }
    }
}

/** Passive feedback for the automatic offline save: saving → saved, or retry on failure. */
@Composable
private fun AutoSaveStatus(saveState: SaveState, onRetry: () -> Unit) {
    val spacing = LocalSpacing.current
    when (saveState) {
        is SaveState.Saved -> SaveNote(
            text = "Saved offline — available in History",
            color = LocalExtendedColors.current.success,
            icon = TutorIcons.Check,
        )
        is SaveState.Error -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = spacing.m),
        ) {
            Icon(TutorIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(spacing.s))
            Text(
                "Couldn't save offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = spacing.s)) {
                Text("Retry", style = MaterialTheme.typography.labelMedium)
            }
        }
        else -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = spacing.m),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(spacing.s))
            Text(
                "Saving for offline…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SaveNote(text: String, color: androidx.compose.ui.graphics.Color, icon: ImageVector) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = spacing.m),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(spacing.s))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
