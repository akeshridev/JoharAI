package com.akeshridev.johar.data.retrieval

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Owns the on-device LLM file location and model delivery.
 *
 * LiteRT-LM consumes a filesystem path, so the model is downloaded once into
 * app-private storage and reused on subsequent launches.
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

    fun downloadIfMissing(
        modelUrl: String = DEFAULT_MODEL_URL,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): LocalModelStatus {
        val existing = status()
        if (existing.isAvailable) return existing

        ensureDirectory()
        val target = modelFile
        val temporary = File(target.parentFile, "${target.name}.download")
        temporary.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(modelUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "JoharAI/0.1")
            }
            connection.connect()

            val responseCode = connection.responseCode
            check(responseCode in 200..299) {
                "Model download failed with HTTP $responseCode"
            }

            val totalBytes = connection.contentLengthLong.coerceAtLeast(-1L)
            var downloadedBytes = 0L
            connection.inputStream.buffered().use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            }

            check(temporary.length() > 0L) { "Downloaded model is empty" }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) {
                "Unable to move downloaded model into ${target.absolutePath}"
            }
        } catch (throwable: Throwable) {
            temporary.delete()
            throw throwable
        } finally {
            connection?.disconnect()
        }

        return status()
    }

    companion object {
        const val MODEL_DIRECTORY = "models"
        const val DEFAULT_MODEL_FILE_NAME = "johar-qwen2.5-1.5b.litertlm"
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/" +
                "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true"
    }
}

data class LocalModelStatus(
    val path: String,
    val isAvailable: Boolean,
    val sizeBytes: Long,
)
