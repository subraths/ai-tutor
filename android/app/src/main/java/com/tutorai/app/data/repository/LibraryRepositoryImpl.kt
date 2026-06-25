package com.tutorai.app.data.repository

import com.tutorai.app.data.local.LessonDao
import com.tutorai.app.data.local.LessonEntity
import com.tutorai.app.data.local.LessonFileStore
import com.tutorai.app.data.local.LessonWithSegments
import com.tutorai.app.data.local.SegmentEntity
import com.tutorai.app.data.remote.TutorApi
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.model.Segment
import com.tutorai.app.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class LibraryRepositoryImpl(
    private val api: TutorApi,
    private val dao: LessonDao,
    private val fileStore: LessonFileStore,
    private val json: Json,
) : LibraryRepository {

    private val idsSerializer = ListSerializer(String.serializer())

    override suspend fun save(lesson: Lesson): Unit = withContext(Dispatchers.IO) {
        val svgPath = fileStore.saveSvg(lesson.id, api.getAsset(lesson.svgUrl).bytes())

        val segmentEntities = lesson.segments.map { seg ->
            val body = api.getAsset(seg.audioUrl)
            val ext = extensionFor(body.contentType()?.subtype)
            val audioPath = fileStore.saveAudio(lesson.id, seg.index, body.bytes(), ext)
            SegmentEntity(
                lessonId = lesson.id,
                idx = seg.index,
                text = seg.text,
                svgIdsJson = json.encodeToString(idsSerializer, seg.svgElementIds),
                audioPath = audioPath,
                durationMs = seg.durationMs,
            )
        }

        // Replace cleanly (cascade clears old segments before re-insert).
        dao.deleteLesson(lesson.id)
        dao.insertLesson(
            LessonEntity(
                id = lesson.id,
                topic = lesson.topic,
                title = lesson.title,
                totalDurationMs = lesson.totalDurationMs,
                svgPath = svgPath,
                createdAt = System.currentTimeMillis(),
            ),
        )
        dao.insertSegments(segmentEntities)
    }

    override fun history(): Flow<List<Lesson>> =
        dao.history().map { rows -> rows.map { it.toLesson() } }

    override suspend fun load(lessonId: String): Pair<Lesson, String> =
        withContext(Dispatchers.IO) {
            val row = dao.getLesson(lessonId) ?: error("Lesson not saved")
            row.toLesson() to fileStore.readText(row.lesson.svgPath)
        }

    override suspend fun delete(lessonId: String): Unit = withContext(Dispatchers.IO) {
        dao.deleteLesson(lessonId)
        fileStore.deleteLesson(lessonId)
    }

    override suspend fun isSaved(lessonId: String): Boolean =
        withContext(Dispatchers.IO) { dao.count(lessonId) > 0 }

    private fun extensionFor(subtype: String?): String = when (subtype?.lowercase()) {
        "wav", "x-wav" -> "wav"
        "mpeg", "mp3" -> "mp3"
        "ogg" -> "ogg"
        else -> "bin"
    }

    private fun LessonWithSegments.toLesson(): Lesson = Lesson(
        id = lesson.id,
        topic = lesson.topic,
        title = lesson.title,
        totalDurationMs = lesson.totalDurationMs,
        svgUrl = "file://${lesson.svgPath}",
        segments = segments.sortedBy { it.idx }.map { s ->
            Segment(
                index = s.idx,
                text = s.text,
                svgElementIds = json.decodeFromString(idsSerializer, s.svgIdsJson),
                audioUrl = "file://${s.audioPath}",
                durationMs = s.durationMs,
            )
        },
    )
}
