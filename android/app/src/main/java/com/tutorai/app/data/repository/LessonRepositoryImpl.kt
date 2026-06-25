package com.tutorai.app.data.repository

import com.tutorai.app.data.remote.TutorApi
import com.tutorai.app.data.remote.dto.GenerateLessonRequestDto
import com.tutorai.app.data.remote.toLesson
import com.tutorai.app.domain.model.GenerationStatus
import com.tutorai.app.domain.model.Lesson
import com.tutorai.app.domain.repository.LessonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val POLL_INTERVAL_MS = 1500L

class LessonRepositoryImpl(
    private val api: TutorApi,
    private val baseUrl: String,
) : LessonRepository {

    override fun generateLesson(topic: String): Flow<GenerationStatus> = flow {
        emit(GenerationStatus.InProgress(stage = "submitting", progress = 0))

        val response = api.createLesson(GenerateLessonRequestDto(topic = topic))
        when (response.code()) {
            200 -> {
                // Cache hit: body carries the lesson id; fetch the manifest.
                val id = response.body()?.id ?: error("Cache-hit response missing lesson id")
                emit(GenerationStatus.Completed(api.getLesson(id).toLesson(baseUrl)))
            }

            202 -> {
                val jobId = response.body()?.job_id ?: error("Accepted response missing job id")
                while (true) {
                    val job = api.getJob(jobId)
                    if (job.status == "succeeded") {
                        val lessonId = job.lesson_id ?: error("Job succeeded without lesson id")
                        emit(GenerationStatus.Completed(api.getLesson(lessonId).toLesson(baseUrl)))
                        break
                    }
                    if (job.status == "failed") {
                        emit(GenerationStatus.Failed(job.error ?: "Generation failed"))
                        break
                    }
                    emit(GenerationStatus.InProgress(job.stage ?: job.status, job.progress))
                    delay(POLL_INTERVAL_MS)
                }
            }

            else -> emit(GenerationStatus.Failed("Unexpected status ${response.code()}"))
        }
    }.flowOn(Dispatchers.IO).catch { e ->
        emit(GenerationStatus.Failed(e.message ?: "Network error"))
    }

    override suspend fun getLesson(lessonId: String): Lesson = withContext(Dispatchers.IO) {
        api.getLesson(lessonId).toLesson(baseUrl)
    }

    override suspend fun getSvg(svgUrl: String): String = withContext(Dispatchers.IO) {
        api.getAsset(svgUrl).string()
    }
}
