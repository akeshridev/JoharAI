package com.akeshridev.johar.data.retrieval

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** JNI calls are blocking. Shutdown must queue behind them, never race or interrupt them. */
internal class NativeInferenceWorker {
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "JoharLLM") }
    private val closed = AtomicBoolean(false)

    suspend fun <T> run(block: () -> T): T {
        check(!closed.get()) { "Synthesizer is closed" }
        return suspendCancellableCoroutine { continuation ->
            try {
                executor.execute {
                    if (!continuation.isActive) return@execute
                    try {
                        check(!closed.get()) { "Synthesizer is closed" }
                        continuation.resume(block())
                    } catch (error: Throwable) {
                        continuation.resumeWithException(error)
                    }
                }
            } catch (error: java.util.concurrent.RejectedExecutionException) {
                continuation.resumeWithException(IllegalStateException("Synthesizer is closed", error))
            }
        }
    }

    fun close(cleanup: () -> Unit) {
        if (closed.compareAndSet(false, true)) {
            executor.execute(cleanup)
            executor.shutdown() // orderly shutdown drains queued cleanup
        }
    }
}
