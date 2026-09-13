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
 *
 * The configured model is large enough that a network connection can sometimes
 * end early without throwing. A file is therefore considered ready only when its
 * size matches the pinned model artifact, never merely because it is non-empty.
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
        val sizeBytes = file.takeIf(File::isFile)?.length() ?: 0L
        return LocalModelStatus(
            path = file.absolutePath,
            isAvailable = file.isFile && sizeBytes == EXPECTED_MODEL_SIZE_BYTES,
            sizeBytes = sizeBytes,
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
                setRequestProperty("Accept-Encoding", "identity")
            }
            connection.connect()

            val responseCode = connection.responseCode
            check(responseCode in 200..299) {
                "Model download failed with HTTP $responseCode"
            }

            val contentLength = connection.contentLengthLong
            if (contentLength > 0L) {
                check(contentLength == EXPECTED_MODEL_SIZE_BYTES) {
                    "Unexpected model Content-Length: $contentLength; expected $EXPECTED_MODEL_SIZE_BYTES"
                }
            }

            var downloadedBytes = 0L
            connection.inputStream.buffered().use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        onProgress(downloadedBytes, EXPECTED_MODEL_SIZE_BYTES)
                    }
                }
            }

            check(downloadedBytes == EXPECTED_MODEL_SIZE_BYTES) {
                "Incomplete model download: $downloadedBytes bytes; expected $EXPECTED_MODEL_SIZE_BYTES"
            }
            check(temporary.length() == EXPECTED_MODEL_SIZE_BYTES) {
                "Downloaded model file size mismatch: ${temporary.length()} bytes; expected $EXPECTED_MODEL_SIZE_BYTES"
            }

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

        // Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm
        const val EXPECTED_MODEL_SIZE_BYTES = 1_597_931_520L

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
