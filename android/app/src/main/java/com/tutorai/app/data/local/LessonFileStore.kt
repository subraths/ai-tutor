package com.tutorai.app.data.local

import java.io.File

/** Stores lesson assets (svg + audio clips) under <filesDir>/lessons/<lessonId>/. */
class LessonFileStore(private val root: File) {

    private fun dir(lessonId: String): File =
        File(root, "lessons/$lessonId").apply { mkdirs() }

    fun saveSvg(lessonId: String, bytes: ByteArray): String {
        val file = File(dir(lessonId), "diagram.svg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun saveAudio(lessonId: String, index: Int, bytes: ByteArray, ext: String): String {
        val file = File(dir(lessonId), "seg$index.$ext")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun readText(path: String): String = File(path).readText()

    fun deleteLesson(lessonId: String) {
        File(root, "lessons/$lessonId").deleteRecursively()
    }
}
