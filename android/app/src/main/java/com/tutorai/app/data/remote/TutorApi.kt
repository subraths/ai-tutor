package com.tutorai.app.data.remote

import com.tutorai.app.data.remote.dto.CreateLessonResponseDto
import com.tutorai.app.data.remote.dto.GenerateLessonRequestDto
import com.tutorai.app.data.remote.dto.JobStatusDto
import com.tutorai.app.data.remote.dto.LessonManifestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface TutorApi {
    // Response<> so we can branch on 200 (cache hit) vs 202 (job started).
    @POST("api/v1/lessons")
    suspend fun createLesson(@Body body: GenerateLessonRequestDto): Response<CreateLessonResponseDto>

    @GET("api/v1/lessons/jobs/{jobId}")
    suspend fun getJob(@Path("jobId") jobId: String): JobStatusDto

    @GET("api/v1/lessons/{lessonId}")
    suspend fun getLesson(@Path("lessonId") lessonId: String): LessonManifestDto

    // Fetch an asset (e.g. the SVG) by absolute URL.
    @Streaming
    @GET
    suspend fun getAsset(@Url url: String): ResponseBody
}
