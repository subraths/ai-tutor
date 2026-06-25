package com.tutorai.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<SegmentEntity>)

    @Transaction
    @Query("SELECT * FROM lessons ORDER BY createdAt DESC")
    fun history(): Flow<List<LessonWithSegments>>

    @Transaction
    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLesson(id: String): LessonWithSegments?

    @Query("SELECT COUNT(*) FROM lessons WHERE id = :id")
    suspend fun count(id: String): Int

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteLesson(id: String)
}
