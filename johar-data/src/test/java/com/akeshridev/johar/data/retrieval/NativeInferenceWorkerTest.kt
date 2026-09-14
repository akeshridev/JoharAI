package com.akeshridev.johar.data.retrieval

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class NativeInferenceWorkerTest {
    @Test fun concurrentRequestsUseOneThreadAndNeverOverlap() = runBlocking {
        val worker = NativeInferenceWorker()
        val active = AtomicInteger()
        val closed = CountDownLatch(1)
        try {
            val threads = (1..20).map {
                async(Dispatchers.Default) {
                    worker.run {
                        assertEquals(1, active.incrementAndGet())
                        try { Thread.currentThread().id } finally { active.decrementAndGet() }
                    }
                }
            }.awaitAll()
            assertEquals(1, threads.toSet().size)
        } finally { worker.close { closed.countDown() } }
        assertTrue(closed.await(5, TimeUnit.SECONDS))
    }

    @Test fun timeoutReturnsBeforeBlockedNativeCallAndCleanupWaits() = runBlocking {
        val worker = NativeInferenceWorker()
        val started = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val closed = CountDownLatch(1)
        try {
            val call = async(Dispatchers.Default) {
                assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
                    kotlinx.coroutines.withTimeout(500) {
                        worker.run {
                            started.countDown()
                            check(finish.await(5, TimeUnit.SECONDS))
                        }
                    }
                }
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            kotlinx.coroutines.withTimeout(2_000) { call.await() }
            worker.close { closed.countDown() }
            assertEquals(1L, closed.count)
            assertFailsWith<IllegalStateException> { worker.run { error("must not run") } }
        } finally {
            finish.countDown()
            worker.close { closed.countDown() }
        }
        assertTrue(closed.await(5, TimeUnit.SECONDS))
    }

    @Test fun closeWaitsForNativeCallAndRejectsFurtherWork() = runBlocking {
        val worker = NativeInferenceWorker()
        val started = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val cleanups = AtomicInteger()
        val call = async(Dispatchers.Default) {
            worker.run {
                started.countDown()
                check(finish.await(5, TimeUnit.SECONDS))
            }
        }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            worker.close { cleanups.incrementAndGet(); closed.countDown() }
            worker.close { cleanups.incrementAndGet() }
            assertEquals(1L, closed.count)
            assertFailsWith<IllegalStateException> { worker.run { error("must not run") } }
        } finally { finish.countDown() }
        call.await()
        assertTrue(closed.await(5, TimeUnit.SECONDS))
        assertEquals(1, cleanups.get())
    }
}
