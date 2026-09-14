package com.akeshridev.johar.data.retrieval

import java.io.File

/** Delivery-independent container checks; delivery must verify artifact size/checksum. */
internal object LocalModelValidator {
    fun validate(file: File, expectedBytes: Long? = null) {
        check(file.isFile && file.canRead()) { "MODEL_MISSING_OR_UNREADABLE path=${file.absolutePath}" }
        check(file.length() >= 8) { "MODEL_INCOMPLETE_OR_WRONG_ARTIFACT bytes=${file.length()}" }
        check(expectedBytes == null || file.length() == expectedBytes) {
            "MODEL_INCOMPLETE_OR_WRONG_ARTIFACT bytes=${file.length()} expected=$expectedBytes"
        }
        val header = ByteArray(8)
        java.io.DataInputStream(file.inputStream()).use { it.readFully(header) }
        check(header.contentEquals("LITERTLM".toByteArray(Charsets.US_ASCII))) {
            "MODEL_INVALID_HEADER"
        }
    }
}

internal object LiteRtLmRuntimePolicy {
    fun isEmulator(fingerprint: String, model: String, hardware: String, product: String): Boolean {
        val values = listOf(fingerprint, model, hardware, product).joinToString(" ").lowercase()
        return listOf("generic", "emulator", "sdk_gphone", "google_sdk", "goldfish", "ranchu", "vbox", "genymotion")
            .any(values::contains)
    }

    fun failureKind(error: Throwable): String {
        val messages = generateSequence(error) { it.cause }.take(16)
            .joinToString(" | ") { it.message.orEmpty() }.lowercase()
        return when {
            listOf("model_", "tf_lite_prefill_decode", "flatbuffer", "invalid model", "unexpected eof")
                .any(messages::contains) -> "MODEL_INVALID_OR_INCOMPATIBLE"
            listOf("failed to modify graph with delegate", "opencl", "gpu delegate", "gpu is not supported", "unsupported gpu")
                .any(messages::contains) -> "GPU_UNSUPPORTED_OR_DELEGATE_FAILURE"
            error is LinkageError -> "RUNTIME_UNAVAILABLE"
            else -> "RUNTIME_FAILURE"
        }
    }
}
