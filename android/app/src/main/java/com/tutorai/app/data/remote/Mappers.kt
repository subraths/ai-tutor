package com.tutorai.app.data.remote

import com.tutorai.app.data.remote.dto.LessonManifestDto
import com.tutorai.app.data.remote.dto.SegmentDto
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.model.Segment

/** Resolve a possibly-relative asset path (e.g. "/api/v1/assets/..") against the base URL. */
internal fun resolveUrl(baseUrl: String, path: String): String =
    if (path.startsWith("http")) path
    else baseUrl.trimEnd('/') + "/" + path.trimStart('/')

fun LessonManifestDto.toLesson(baseUrl: String): Lesson = Lesson(
    id = id,
    topic = topic,
    title = title,
    totalDurationMs = total_duration_ms,
    svgUrl = resolveUrl(baseUrl, svg.url),
    segments = segments.map { it.toSegment(baseUrl) },
)

fun SegmentDto.toSegment(baseUrl: String): Segment = Segment(
    index = index,
    text = text,
    svgElementIds = svg_element_ids,
    audioUrl = resolveUrl(baseUrl, audio.url),
    durationMs = duration_ms,
)
