package com.tutorai.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val title: String,
    val totalDurationMs: Int,
    val svgPath: String,
    val createdAt: Long,
)

@Entity(
    tableName = "segments",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lessonId")],
)
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val lessonId: String,
    val idx: Int,
    val text: String,
    val svgIdsJson: String,
    val audioPath: String,
    val durationMs: Int,
)

data class LessonWithSegments(
    @Embedded val lesson: LessonEntity,
    @Relation(parentColumn = "id", entityColumn = "lessonId")
    val segments: List<SegmentEntity>,
)
