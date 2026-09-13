package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.data.local.CrawlKeywordRow
import com.akeshridev.johar.data.local.CrawledSourceDao
import com.akeshridev.johar.data.local.CrawledSourceEntity
import com.akeshridev.johar.data.local.KnowledgeDao
import com.akeshridev.johar.data.local.toCrawlSeed
import com.akeshridev.johar.data.local.toDomain
import com.akeshridev.johar.data.local.toRow
import com.akeshridev.johar.data.source.DiscoveryResult
import com.akeshridev.johar.data.source.SourceResult
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity

internal class KnowledgeStore(
    private val knowledgeDao: KnowledgeDao,
    private val sourceDao: CrawledSourceDao,
) {
    fun ensureSeed(seed: CrawlSeed): CrawlSeed {
        val existingByName = seed.entityId?.let(knowledgeDao::getEntity)
            ?: knowledgeDao.findEntity(normalizeText(seed.name), seed.region)
        val entityId = existingByName?.id
            ?: seed.entityId
            ?: stableId("entity", normalizeText(seed.name), seed.region, seed.country)
        val existingRefs = existingByName?.toCrawlSeed()?.externalRefs.orEmpty()

        val entity = KnowledgeEntity(
            id = entityId,
            name = seed.name,
            type = seed.entityType ?: existingByName?.type
                ?.let { runCatching { EntityType.valueOf(it) }.getOrNull() }
                ?: EntityType.OTHER,
            description = existingByName?.description,
            latitude = seed.latitude ?: existingByName?.latitude,
            longitude = seed.longitude ?: existingByName?.longitude,
            region = seed.region,
            country = seed.country,
            aliases = emptyList(),
            externalRefs = existingRefs + seed.externalRefs,
        )
        val now = System.currentTimeMillis()
        knowledgeDao.upsertEntity(
            entity.toRow(
                nowEpochMillis = now,
                discoveryDepth = seed.depth,
                existing = existingByName,
            ),
        )

        return seed.copy(
            entityId = entityId,
            entityType = entity.type,
            latitude = entity.latitude,
            longitude = entity.longitude,
            externalRefs = entity.externalRefs,
        )
    }

    fun seed(entityId: String): CrawlSeed? = knowledgeDao.getEntity(entityId)?.toCrawlSeed()

    fun insertKeywords(keywords: List<CrawlKeyword>) {
        if (keywords.isNotEmpty()) knowledgeDao.insertKeywords(keywords.map { it.toRow() })
    }

    fun nextEntitiesToCrawl(
        staleBeforeEpochMillis: Long,
        limit: Int,
    ): List<CrawlSeed> = knowledgeDao
        .nextEntitiesToCrawl(staleBeforeEpochMillis, limit)
        .map { it.toCrawlSeed() }

    fun nextKeywordsToCrawl(
        staleBeforeEpochMillis: Long,
        limit: Int,
    ): List<CrawlKeyword> = knowledgeDao
        .nextKeywordsToCrawl(staleBeforeEpochMillis, limit)
        .map(CrawlKeywordRow::toDomain)

    fun persist(
        seed: CrawlSeed,
        result: SourceResult,
    ) {
        val ownerEntityId = requireNotNull(seed.entityId)
        val now = System.currentTimeMillis()
        persistSnapshot(ownerEntityId, result.sourceUrl, result.publisher, result.rawContent, now)

        result.entity?.let { enriched ->
            val canonical = if (enriched.id == ownerEntityId) enriched else enriched.copy(id = ownerEntityId)
            knowledgeDao.upsertEntity(
                canonical.toRow(
                    nowEpochMillis = now,
                    discoveryDepth = seed.depth,
                    existing = knowledgeDao.getEntity(ownerEntityId),
                ),
            )
        }

        result.discoveredEntities.forEach { entity ->
            val existing = knowledgeDao.getEntity(entity.id)
            knowledgeDao.upsertEntity(
                entity.toRow(
                    nowEpochMillis = now,
                    discoveryDepth = seed.depth + 1,
                    existing = existing,
                ),
            )
            insertKeywords(CrawlBootstrap.keywordsFor(entity))
        }

        replaceFacts(result)
        replaceRelationships(result)
        replaceMedia(result)
        insertKeywords(result.discoveredKeywords)
    }

    fun persistDiscovery(
        keyword: CrawlKeyword,
        result: DiscoveryResult,
    ) {
        val ownerEntityId = keyword.entityId ?: "global:johar"
        val now = System.currentTimeMillis()
        val parentDepth = keyword.entityId
            ?.let(knowledgeDao::getEntity)
            ?.discoveryDepth
            ?: 0
        persistSnapshot(ownerEntityId, result.sourceUrl, result.publisher, result.rawContent, now)

        result.entities.forEach { entity ->
            val existing = knowledgeDao.getEntity(entity.id)
            knowledgeDao.upsertEntity(
                entity.toRow(
                    nowEpochMillis = now,
                    discoveryDepth = minOf(existing?.discoveryDepth ?: parentDepth + 1, parentDepth + 1),
                    existing = existing,
                ),
            )
            insertKeywords(CrawlBootstrap.keywordsFor(entity))
        }
        insertKeywords(result.keywords)
    }

    fun markEntityCrawled(entityId: String, nowEpochMillis: Long = System.currentTimeMillis()) {
        knowledgeDao.markEntityCrawled(entityId, nowEpochMillis)
    }

    fun markKeywordCrawled(keywordId: String, nowEpochMillis: Long = System.currentTimeMillis()) {
        knowledgeDao.markKeywordCrawled(keywordId, nowEpochMillis)
    }

    fun markKeywordFailed(keywordId: String, nowEpochMillis: Long = System.currentTimeMillis()) {
        knowledgeDao.markKeywordFailed(
            keywordId = keywordId,
            crawledAtEpochMillis = nowEpochMillis,
            disableAfterFailures = MAX_KEYWORD_FAILURES,
        )
    }

    fun stats(): CrawlStats = CrawlStats(
        entities = knowledgeDao.entityCount(),
        facts = knowledgeDao.factCount(),
        relationships = knowledgeDao.relationshipCount(),
        media = knowledgeDao.mediaCount(),
        enabledKeywords = knowledgeDao.enabledKeywordCount(),
    )

    private fun replaceFacts(result: SourceResult) {
        val rows = result.facts.map { it.toRow() }
        rows.groupBy { it.entityId to it.sourceUrl }
            .forEach { (key, _) -> knowledgeDao.deleteFactsForSource(key.first, key.second) }
        if (rows.isNotEmpty()) knowledgeDao.upsertFacts(rows)
    }

    private fun replaceRelationships(result: SourceResult) {
        val rows = result.relationships.map { it.toRow() }
        rows.groupBy { it.fromEntityId to it.sourceUrl }
            .forEach { (key, _) -> knowledgeDao.deleteRelationshipsForSource(key.first, key.second) }
        if (rows.isNotEmpty()) knowledgeDao.upsertRelationships(rows)
    }

    private fun replaceMedia(result: SourceResult) {
        val rows = result.media.map { it.toRow() }
        rows.groupBy { it.entityId to it.sourceUrl }
            .forEach { (key, _) -> knowledgeDao.deleteMediaForSource(key.first, key.second) }
        if (rows.isNotEmpty()) knowledgeDao.upsertMedia(rows)
    }

    private fun persistSnapshot(
        entityId: String,
        sourceUrl: String,
        publisher: String,
        rawContent: String,
        fetchedAtEpochMillis: Long,
    ) {
        sourceDao.upsert(
            CrawledSourceEntity(
                sourceUrl = sourceUrl,
                entityId = entityId,
                publisher = publisher,
                fetchedAtEpochMillis = fetchedAtEpochMillis,
                content = rawContent.take(MAX_RAW_SNAPSHOT_CHARS),
            ),
        )
    }

    companion object {
        private const val MAX_RAW_SNAPSHOT_CHARS = 750_000
        private const val MAX_KEYWORD_FAILURES = 5
    }
}

internal data class CrawlStats(
    val entities: Int,
    val facts: Int,
    val relationships: Int,
    val media: Int,
    val enabledKeywords: Int,
)
