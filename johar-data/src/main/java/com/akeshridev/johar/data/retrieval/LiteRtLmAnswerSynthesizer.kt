package com.akeshridev.johar.data.retrieval

import android.os.SystemClock
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LiteRT-LM implementation of [LocalAnswerSynthesizer].
 *
 * The model file is intentionally supplied as an external path so large model
 * assets can be downloaded after install instead of being bundled in the APK.
 * The engine is initialized lazily on first synthesis and reused afterwards.
 */
class LiteRtLmAnswerSynthesizer(
    private val modelPath: String,
) : LocalAnswerSynthesizer, AutoCloseable {

    private val mutex = Mutex()
    private var engine: Engine? = null

    override suspend fun synthesize(context: OfflineRagContext): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val activeEngine = engine ?: createEngine().also { engine = it }

            Log.i(TAG, "CONVERSATION_CREATE_START")
            val conversationStartedAt = SystemClock.elapsedRealtime()
            activeEngine.createConversation().use { conversation ->
                Log.i(
                    TAG,
                    "CONVERSATION_CREATE_COMPLETE latencyMs=${SystemClock.elapsedRealtime() - conversationStartedAt}",
                )

                Log.i(TAG, "GENERATION_START promptChars=${context.prompt.length}")
                val generationStartedAt = SystemClock.elapsedRealtime()
                val result = conversation.sendMessage(context.prompt).toString().trim()
                Log.i(
                    TAG,
                    "GENERATION_COMPLETE latencyMs=${SystemClock.elapsedRealtime() - generationStartedAt} " +
                        "answerChars=${result.length}",
                )
                result
            }
        }
    }

    override fun close() {
        Log.i(TAG, "ENGINE_CLOSE")
        engine?.close()
        engine = null
    }

    private fun createEngine(): Engine {
        Log.i(TAG, "ENGINE_CREATE_START backend=CPU modelPath=$modelPath")
        val createStartedAt = SystemClock.elapsedRealtime()
        val created = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
            ),
        )
        Log.i(
            TAG,
            "ENGINE_CREATE_COMPLETE latencyMs=${SystemClock.elapsedRealtime() - createStartedAt}",
        )

        Log.i(TAG, "ENGINE_INITIALIZE_START")
        val initializeStartedAt = SystemClock.elapsedRealtime()
        created.initialize()
        Log.i(
            TAG,
            "ENGINE_INITIALIZE_COMPLETE latencyMs=${SystemClock.elapsedRealtime() - initializeStartedAt}",
        )
        return created
    }

    companion object {
        private const val TAG = "JoharLLM"
    }
}
