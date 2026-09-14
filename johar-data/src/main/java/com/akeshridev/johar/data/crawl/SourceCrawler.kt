package com.akeshridev.johar.data.crawl

import android.util.Log
import com.akeshridev.johar.data.source.CrawlSourceAdapter
import com.akeshridev.johar.data.source.KeywordDiscoveryAdapter
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.crawl.CrawlTarget

internal class SourceCrawler(
    private val store: KnowledgeStore,
    private val sourceAdapters: List<CrawlSourceAdapter>,
    private val discoveryAdapters: List<KeywordDiscoveryAdapter>,
) {
    fun crawl(target: CrawlTarget): CrawlStats {
        val root = store.ensureSeed(target.seed)
        if (target == CrawlTarget.RANCHI || target == CrawlTarget.JHARKHAND) {
            store.insertKeywords(CrawlBootstrap.rootKeywords(requireNotNull(root.entityId), target))
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

        return store.stats().also { stats ->
            Log.i(
                TAG,
                "crawl_complete target=$target entities=${stats.entities} facts=${stats.facts} " +
                    "relationships=${stats.relationships} media=${stats.media} keywords=${stats.enabledKeywords}",
            )
        }
    }

    private fun crawlPendingKeywords(target: CrawlTarget) {
        val staleBefore = System.currentTimeMillis() - KEYWORD_STALE_MILLIS
        val limit = if (target == CrawlTarget.RANCHI) RANCHI_KEYWORDS_PER_RUN else DEFAULT_KEYWORDS_PER_RUN
        store.nextKeywordsToCrawl(staleBefore, limit).forEach { keyword ->
            if (keyword.discoveredFrom != BOOTSTRAP_SOURCE) {
                store.markKeywordCrawled(keyword.id)
                return@forEach
            }

            var supported = false
            var successes = 0
            discoveryAdapters.forEach { adapter ->
                if (!adapter.supports(keyword)) return@forEach
                if (shouldSkipDiscovery(adapter.id, keyword)) return@forEach
                supported = true
                runCatching { adapter.discover(keyword) }
                    .onSuccess { result ->
                        store.persistDiscovery(keyword, result)
                        successes += 1
                    }
                    .onFailure { error ->
                        Log.w(TAG, "keyword_source_failed adapter=${adapter.id} keyword=${keyword.term}", error)
                    }
            }

            when {
                successes > 0 || !supported -> store.markKeywordCrawled(keyword.id)
                else -> store.markKeywordFailed(keyword.id)
            }
        }
    }

    private fun shouldSkipDiscovery(adapterId: String, keyword: CrawlKeyword): Boolean {
        if (adapterId != OPENSTREETMAP_ADAPTER_ID) return false
        val term = normalizeText(keyword.term)
        return BROAD_OSM_TERMS.any(term::contains)
    }

    private fun crawlPendingEntities(target: CrawlTarget, excludingEntityId: String?) {
        val staleBefore = System.currentTimeMillis() - ENTITY_STALE_MILLIS
        val limit = if (target == CrawlTarget.RANCHI) RANCHI_ENTITIES_PER_RUN else DEFAULT_ENTITIES_PER_RUN
        store.nextEntitiesToCrawl(staleBefore, limit)
            .filterNot { it.entityId == excludingEntityId }
            .forEach(::crawlEntity)
    }

    private fun crawlEntity(initialSeed: CrawlSeed): Int {
        val entityId = requireNotNull(initialSeed.entityId)
        if (initialSeed.depth > MAX_DISCOVERY_DEPTH) {
            store.markEntityCrawled(entityId)
            return 0
        }

        var seed = initialSeed
        var successes = 0
        sourceAdapters.forEach { adapter ->
            if (!adapter.supports(seed)) return@forEach
            runCatching { adapter.crawl(seed) }
                .onSuccess { result ->
                    if (result != null) {
                        store.persist(seed, result)
                        successes += 1
                        seed = store.seed(entityId) ?: seed
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "entity_source_failed adapter=${adapter.id} entity=${seed.name}", error)
                }
        }
        store.markEntityCrawled(entityId)
        return successes
    }

    companion object {
        private const val TAG = "JoharCrawl"
        private const val BOOTSTRAP_SOURCE = "bootstrap"
        private const val OPENSTREETMAP_ADAPTER_ID = "openstreetmap"

        // Ranchi V1 is a prototype breadth pass. Keep the budget bounded so public sources are
        // treated politely, but large enough that manual crawl runs produce useful coverage.
        private const val RANCHI_ENTITIES_PER_RUN = 12
        private const val RANCHI_KEYWORDS_PER_RUN = 8
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
