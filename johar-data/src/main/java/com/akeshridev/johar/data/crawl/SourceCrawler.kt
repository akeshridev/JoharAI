package com.akeshridev.johar.data.crawl

import android.util.Log
import com.akeshridev.johar.data.source.CrawlSourceAdapter
import com.akeshridev.johar.data.source.DiscoveryResult
import com.akeshridev.johar.data.source.KeywordDiscoveryAdapter
import com.akeshridev.johar.data.source.SourceResult
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.crawl.CrawlTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal class SourceCrawler(
    private val store: KnowledgeStore,
    private val sourceAdapters: List<CrawlSourceAdapter>,
    private val discoveryAdapters: List<KeywordDiscoveryAdapter>,
) {
    private val persistenceMutex = Mutex()
    private val overpassSemaphore = Semaphore(OVERPASS_CONCURRENCY)

    suspend fun crawl(target: CrawlTarget): CrawlStats {
        val root = persistenceMutex.withLock {
            store.ensureSeed(target.seed).also { seeded ->
                if (target == CrawlTarget.RANCHI || target == CrawlTarget.JHARKHAND) {
                    store.insertKeywords(CrawlBootstrap.rootKeywords(requireNotNull(seeded.entityId), target))
                }
            }
        }

        val rootSuccesses = crawlEntity(root)
        check(rootSuccesses > 0) { "No source succeeded for root ${root.name}" }

        crawlPendingKeywords(target)
        // Ranchi V1 is intentionally bounded. The packaged/base database still contains statewide
        // entities, and crawling the global pending queue here causes Ranchi runs to wander into
        // Palamu, Latehar, Bokaro, etc. Ranchi discovery already persists the entities it finds;
        // enrichment of arbitrary pending entities is reserved for explicit non-Ranchi targets.
        if (target != CrawlTarget.RANCHI) {
            crawlPendingEntities(target, excludingEntityId = root.entityId)
        }

        return persistenceMutex.withLock { store.stats() }.also { stats ->
            Log.i(
                TAG,
                "crawl_complete target=$target entities=${stats.entities} facts=${stats.facts} " +
                    "relationships=${stats.relationships} media=${stats.media} keywords=${stats.enabledKeywords}",
            )
        }
    }

    private suspend fun crawlPendingKeywords(target: CrawlTarget) {
        val staleBefore = System.currentTimeMillis() - KEYWORD_STALE_MILLIS
        val limit = if (target == CrawlTarget.RANCHI) RANCHI_KEYWORDS_PER_RUN else DEFAULT_KEYWORDS_PER_RUN
        val pending = persistenceMutex.withLock {
            store.nextKeywordsToCrawl(staleBefore, limit)
        }

        pending.chunked(CONCURRENT_BATCH_SIZE).forEach { batch ->
            coroutineScope {
                batch.map { keyword ->
                    async(Dispatchers.IO) { crawlKeyword(keyword) }
                }.awaitAll()
            }
        }
    }

    private suspend fun crawlKeyword(keyword: CrawlKeyword) {
        if (keyword.discoveredFrom != BOOTSTRAP_SOURCE) {
            persistenceMutex.withLock { store.markKeywordCrawled(keyword.id) }
            return
        }

        var supported = false
        var successes = 0
        discoveryAdapters.forEach { adapter ->
            if (!adapter.supports(keyword)) return@forEach
            if (shouldSkipDiscovery(adapter.id, keyword)) return@forEach
            supported = true

            try {
                val result = discover(adapter, keyword)
                persistenceMutex.withLock {
                    store.persistDiscovery(keyword, result)
                }
                successes += 1
            } catch (error: Exception) {
                Log.w(TAG, "keyword_source_failed adapter=${adapter.id} keyword=${keyword.term}", error)
            }
        }

        persistenceMutex.withLock {
            when {
                successes > 0 || !supported -> store.markKeywordCrawled(keyword.id)
                else -> store.markKeywordFailed(keyword.id)
            }
        }
    }

    private suspend fun discover(
        adapter: KeywordDiscoveryAdapter,
        keyword: CrawlKeyword,
    ): DiscoveryResult = if (isOverpass(adapter.id)) {
        overpassSemaphore.withPermit {
            withContext(Dispatchers.IO) { adapter.discover(keyword) }
        }
    } else {
        withContext(Dispatchers.IO) { adapter.discover(keyword) }
    }

    private fun shouldSkipDiscovery(adapterId: String, keyword: CrawlKeyword): Boolean {
        if (adapterId != OPENSTREETMAP_ADAPTER_ID) return false
        val term = normalizeText(keyword.term)
        return BROAD_OSM_TERMS.any(term::contains)
    }

    private suspend fun crawlPendingEntities(target: CrawlTarget, excludingEntityId: String?) {
        val staleBefore = System.currentTimeMillis() - ENTITY_STALE_MILLIS
        val limit = if (target == CrawlTarget.RANCHI) RANCHI_ENTITIES_PER_RUN else DEFAULT_ENTITIES_PER_RUN
        val pending = persistenceMutex.withLock {
            store.nextEntitiesToCrawl(staleBefore, limit)
        }.filterNot { it.entityId == excludingEntityId }

        pending.chunked(CONCURRENT_BATCH_SIZE).forEach { batch ->
            coroutineScope {
                batch.map { seed ->
                    async(Dispatchers.IO) { crawlEntity(seed) }
                }.awaitAll()
            }
        }
    }

    private suspend fun crawlEntity(initialSeed: CrawlSeed): Int {
        val entityId = requireNotNull(initialSeed.entityId)
        if (initialSeed.depth > MAX_DISCOVERY_DEPTH) {
            persistenceMutex.withLock { store.markEntityCrawled(entityId) }
            return 0
        }

        var seed = initialSeed
        var successes = 0
        sourceAdapters.forEach { adapter ->
            if (!adapter.supports(seed)) return@forEach

            try {
                val result = crawlSource(adapter, seed)
                if (result != null) {
                    persistenceMutex.withLock {
                        store.persist(seed, result)
                        seed = store.seed(entityId) ?: seed
                    }
                    successes += 1
                }
            } catch (error: Exception) {
                Log.w(TAG, "entity_source_failed adapter=${adapter.id} entity=${seed.name}", error)
            }
        }
        persistenceMutex.withLock { store.markEntityCrawled(entityId) }
        return successes
    }

    private suspend fun crawlSource(
        adapter: CrawlSourceAdapter,
        seed: CrawlSeed,
    ): SourceResult? = if (isOverpass(adapter.id)) {
        overpassSemaphore.withPermit {
            withContext(Dispatchers.IO) { adapter.crawl(seed) }
        }
    } else {
        withContext(Dispatchers.IO) { adapter.crawl(seed) }
    }

    private fun isOverpass(adapterId: String): Boolean =
        adapterId.startsWith(OPENSTREETMAP_ADAPTER_ID)

    companion object {
        private const val TAG = "JoharCrawl"
        private const val BOOTSTRAP_SOURCE = "bootstrap"
        private const val OPENSTREETMAP_ADAPTER_ID = "openstreetmap"

        // One WorkManager job may fetch up to ten independent crawl items in parallel. Keep
        // persistence serialized because canonical identity resolution is read-modify-write.
        private const val CONCURRENT_BATCH_SIZE = 10
        private const val OVERPASS_CONCURRENCY = 2

        // Ranchi V1 is a prototype breadth pass. Keep the budget bounded so public sources are
        // treated politely, but large enough that manual crawl runs produce useful coverage.
        private const val RANCHI_ENTITIES_PER_RUN = 12
        private const val RANCHI_KEYWORDS_PER_RUN = 10
        private const val DEFAULT_ENTITIES_PER_RUN = 6
        private const val DEFAULT_KEYWORDS_PER_RUN = 3

        private const val MAX_DISCOVERY_DEPTH = 2
        private const val ENTITY_STALE_MILLIS = 24L * 60L * 60L * 1_000L
        private const val KEYWORD_STALE_MILLIS = 30L * 24L * 60L * 60L * 1_000L

        private val BROAD_OSM_TERMS = listOf(
            "places in jharkhand",
            "picnic spots in jharkhand",
            "villages in jharkhand",
            "rivers in jharkhand",
        )
    }
}
