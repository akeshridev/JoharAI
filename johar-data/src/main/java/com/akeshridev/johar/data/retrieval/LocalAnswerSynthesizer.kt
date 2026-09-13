package com.akeshridev.johar.data.retrieval

/**
 * Model/runtime-neutral contract for a future on-device LLM.
 *
 * The implementation receives only grounded retrieval context and must synthesize
 * from that evidence. This keeps the retrieval pipeline independent from the
 * eventual local inference runtime and model choice.
 */
interface LocalAnswerSynthesizer {
    suspend fun synthesize(context: OfflineRagContext): String
}
