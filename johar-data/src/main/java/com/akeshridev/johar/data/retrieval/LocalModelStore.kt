package com.akeshridev.johar.data.retrieval

import android.content.Context
import java.io.File

/**
 * Owns the local on-device LLM file location without coupling model delivery to inference.
 *
 * Large models are intentionally stored in app-private files instead of APK assets.
 * A future downloader can write atomically to [modelFile] and the inference layer does
 * not need to change.
 */
class LocalModelStore(
    context: Context,
    private val modelFileName: String = DEFAULT_MODEL_FILE_NAME,
) {
    private val appContext = context.applicationContext

    val modelFile: File
        get() = File(File(appContext.filesDir, MODEL_DIRECTORY), modelFileName)

    fun status(): LocalModelStatus {
        val file = modelFile
        return LocalModelStatus(
            path = file.absolutePath,
            isAvailable = file.isFile && file.length() > 0L,
            sizeBytes = file.takeIf(File::isFile)?.length() ?: 0L,
        )
    }

    fun ensureDirectory(): File = modelFile.parentFile!!.also(File::mkdirs)

    companion object {
        const val MODEL_DIRECTORY = "models"
        const val DEFAULT_MODEL_FILE_NAME = "johar-qwen2.5-1.5b.litertlm"
    }
}

data class LocalModelStatus(
    val path: String,
    val isAvailable: Boolean,
    val sizeBytes: Long,
)
