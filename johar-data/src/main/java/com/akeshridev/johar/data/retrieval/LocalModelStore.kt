package com.akeshridev.johar.data.retrieval

import android.content.Context
import java.io.File

/**
 * Owns the on-device LLM file location and can materialize a model bundled in APK assets.
 *
 * LiteRT-LM consumes a filesystem path, so a bundled model is copied once from assets to
 * app-private storage on first use and reused afterwards.
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

    fun ensureBundledModel(): LocalModelStatus {
        val existing = status()
        if (existing.isAvailable) return existing

        ensureDirectory()
        val assetPath = "$BUNDLED_ASSET_DIRECTORY/$modelFileName"
        val target = modelFile
        val temporary = File(target.parentFile, "${target.name}.tmp")

        try {
            appContext.assets.open(assetPath).use { input ->
                temporary.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
            if (temporary.length() <= 0L) {
                temporary.delete()
                return status()
            }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) {
                "Unable to move bundled model into ${target.absolutePath}"
            }
        } catch (_: java.io.FileNotFoundException) {
            temporary.delete()
        } catch (throwable: Throwable) {
            temporary.delete()
            throw throwable
        }

        return status()
    }

    companion object {
        const val MODEL_DIRECTORY = "models"
        const val BUNDLED_ASSET_DIRECTORY = "models"
        const val DEFAULT_MODEL_FILE_NAME = "johar-qwen2.5-1.5b.litertlm"
    }
}

data class LocalModelStatus(
    val path: String,
    val isAvailable: Boolean,
    val sizeBytes: Long,
)
