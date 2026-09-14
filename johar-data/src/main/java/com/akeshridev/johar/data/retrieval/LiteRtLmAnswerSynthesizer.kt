package com.akeshridev.johar.data.retrieval

import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.NoRepeatNgramConfig
import com.google.ai.edge.litertlm.RepetitionPenaltyConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** One owner per chat lifecycle. All native operations, including cleanup, share one thread. */
class LiteRtLmAnswerSynthesizer(
    private val modelPath: String,
    private val timeoutMs: Long = 90_000L,
) : LocalAnswerSynthesizer, AutoCloseable {
    private val worker = NativeInferenceWorker()
    private var engine: Engine? = null
    @Volatile private var backend = "NONE"
    @Volatile private var stage = "MODEL_VALIDATION"

    override suspend fun synthesize(context: OfflineRagContext): String {
        try {
            return withTimeout(timeoutMs) {
                worker.run {
                    try {
                        if (context.hits.isEmpty()) {
                            return@run "The offline knowledge pack does not have enough information."
                        }
                        stage = "DEVICE_CHECK"
                        check(!LiteRtLmRuntimePolicy.isEmulator(Build.FINGERPRINT, Build.MODEL, Build.HARDWARE, Build.PRODUCT)) {
                            "EMULATOR_UNSUPPORTED: real inference requires a physical Android device"
                        }
                        check(Build.SUPPORTED_ABIS.contains("arm64-v8a")) { "DEVICE_UNSUPPORTED: arm64-v8a required" }
                        Log.i(TAG, "DEVICE physical=true model=${Build.MODEL} hardware=${Build.HARDWARE}")
                        generate(engine ?: initializeEngine().also { engine = it }, context)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        failed(error)
                        releaseEngine()
                        throw error
                    } catch (error: LinkageError) {
                        failed(error)
                        releaseEngine()
                        throw error
                    }
                }
            }
        } catch (error: TimeoutCancellationException) {
            Log.e(TAG, "FAILED stage=$stage backend=$backend kind=TIMEOUT timeoutMs=$timeoutMs; worker retired, native cleanup pending", error)
            close()
            throw error
        }
    }

    private fun initializeEngine(): Engine {
        backend = "NONE"
        stage = "MODEL_VALIDATION"
        LocalModelValidator.validate(File(modelPath))
        stage("MODEL_READY", "path=$modelPath bytes=${File(modelPath).length()}")
        return createEngine(Backend.CPU())
    }

    private fun generate(active: Engine, context: OfflineRagContext): String {
        stage("CONVERSATION_CREATE_START")
        val config = ConversationConfig(
            samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.5, seed = 0),
        )
        return active.createConversation(config).use { conversation ->
            stage("GENERATION_START")
            val started = SystemClock.elapsedRealtime()
            val answer = conversation.sendMessage(
                context.prompt,
                repetitionPenaltyConfig = RepetitionPenaltyConfig(repetitionPenalty = 1.3f),
                noRepeatNgramConfig = NoRepeatNgramConfig(noRepeatNgramSize = 3),
                maxOutputToken = 512,
            ).contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }.trim()
            check(answer.isNotEmpty()) { "Empty inference response" }
            stage("GENERATION_COMPLETE", "latencyMs=${SystemClock.elapsedRealtime() - started} answerChars=${answer.length}")
            answer
        }
    }

    private fun createEngine(selected: Backend): Engine {
        backend = selected.name
        stage("ENGINE_CREATE_START")
        val created = Engine(EngineConfig(modelPath = modelPath, backend = selected, maxNumTokens = 2048))
        stage("ENGINE_CREATE_COMPLETE")
        try {
            stage("ENGINE_INITIALIZE_START")
            val started = SystemClock.elapsedRealtime()
            created.initialize()
            stage("ENGINE_INITIALIZE_COMPLETE", "latencyMs=${SystemClock.elapsedRealtime() - started}")
            return created
        } catch (error: Throwable) {
            // SDK close() requires an initialized handle. Failed JNI creation owns its partial cleanup.
            if (created.isInitialized()) {
                try { created.close() } catch (cleanup: Throwable) { error.addSuppressed(cleanup) }
            }
            throw error
        }
    }

    private fun stage(value: String, detail: String = "") {
        stage = value
        Log.i(TAG, "$value backend=$backend $detail")
    }

    private fun failed(error: Throwable) {
        val kind = when {
            error.message.orEmpty().startsWith("EMULATOR_UNSUPPORTED") -> "EMULATOR_UNSUPPORTED"
            error.message.orEmpty().startsWith("DEVICE_UNSUPPORTED") -> "DEVICE_UNSUPPORTED"
            LiteRtLmRuntimePolicy.failureKind(error) != "RUNTIME_FAILURE" -> LiteRtLmRuntimePolicy.failureKind(error)
            stage == "GENERATION_START" -> "INFERENCE_FAILURE"
            else -> LiteRtLmRuntimePolicy.failureKind(error)
        }
        Log.e(TAG, "FAILED stage=$stage backend=$backend kind=$kind reason=${error.message}", error)
    }

    private fun releaseEngine(): Boolean {
        val old = engine
        engine = null
        if (old != null) {
            try {
                old.close()
                stage("ENGINE_CLOSE_COMPLETE")
            } catch (error: Exception) {
                Log.e(TAG, "FAILED stage=ENGINE_CLOSE backend=$backend reason=${error.message}", error)
                worker.close {} // do not start another engine after uncertain native cleanup
                return false
            }
        }
        return true
    }

    /** Nonblocking: rejects new work, then closes after any in-flight JNI call has returned. */
    override fun close() {
        worker.close { releaseEngine() }
    }

    private companion object { const val TAG = "JoharLLM" }
}
