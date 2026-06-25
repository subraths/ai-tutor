package com.tutorai.app.domain.model

/** A generated lesson as the UI needs it (domain model, decoupled from wire DTOs). */
data class Lesson(
    val id: String,
    val topic: String,
    val title: String,
    val totalDurationMs: Int,
    val svgUrl: String,
    val segments: List<Segment>,
)

data class Segment(
    val index: Int,
    val text: String,
    val svgElementIds: List<String>,
    val audioUrl: String,
    val durationMs: Int,
)
