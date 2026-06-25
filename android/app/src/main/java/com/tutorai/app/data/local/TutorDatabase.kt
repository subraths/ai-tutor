package com.tutorai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LessonEntity::class, SegmentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TutorDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
}
