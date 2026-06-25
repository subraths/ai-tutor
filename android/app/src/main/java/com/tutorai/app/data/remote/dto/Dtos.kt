package com.tutorai.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenerateLessonRequestDto(
    val topic: String,
    val language: String = "en",
    val voice: String? = null,
)

/** POST /lessons response: 202 carries job_id; 200 (cache hit) carries the manifest id. */
@Serializable
data class CreateLessonResponseDto(
    val job_id: String? = null,
    val status: String? = null,
    val id: String? = null,
)

@Serializable
data class JobStatusDto(
    val job_id: String,
    val status: String,
    val progress: Int = 0,
    val stage: String? = null,
    val lesson_id: String? = null,
    val error: String? = null,
)

@Serializable
data class AssetRefDto(
    val asset_id: String,
    val url: String,
)

@Serializable
data class SegmentDto(
    val index: Int,
    val text: String,
    val svg_element_ids: List<String>,
    val audio: AssetRefDto,
    val duration_ms: Int,
)

@Serializable
data class LessonManifestDto(
    val id: String,
    val topic: String,
    val title: String,
    val language: String,
    val voice: String,
    val total_duration_ms: Int,
    val created_at: String,
    val svg: AssetRefDto,
    val segments: List<SegmentDto>,
)
