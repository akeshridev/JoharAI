package com.akeshridev.johar.data.retrieval

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
            activeEngine.createConversation().use { conversation ->
                conversation.sendMessage(context.prompt).toString().trim()
            }
        }
    }

    override fun close() {
        engine?.close()
        engine = null
    }

    private fun createEngine(): Engine {
        val created = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
            ),
        )
        created.initialize()
        return created
    }
}
