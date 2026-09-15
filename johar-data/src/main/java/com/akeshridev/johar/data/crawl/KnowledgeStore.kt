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

    fun ranchiOsmBackfillCandidates(
        staleBeforeEpochMillis: Long,
        limit: Int,
    ): List<CrawlSeed> = knowledgeDao
        .ranchiOsmBackfillCandidates(staleBeforeEpochMillis, limit)
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

        val canonicalIds = mutableMapOf<String, String>()
        result.discoveredEntities.forEach { discovered ->
            canonicalIds[discovered.id] = upsertDiscoveredEntity(discovered, seed.depth + 1, now)
        }

        val facts = result.facts.map { fact ->
            canonicalIds[fact.entityId]?.let { fact.copy(entityId = it) } ?: fact
        }
        val relationships = result.relationships.map { relationship ->
            relationship.copy(
                fromEntityId = canonicalIds[relationship.fromEntityId] ?: relationship.fromEntityId,
                toEntityId = canonicalIds[relationship.toEntityId] ?: relationship.toEntityId,
            )
        }
        val media = result.media.map { asset ->
            canonicalIds[asset.entityId]?.let { asset.copy(entityId = it) } ?: asset
        }
        val keywords = result.discoveredKeywords.map { keyword ->
            val remappedId = keyword.entityId?.let(canonicalIds::get)
            if (remappedId != null) keyword.copy(entityId = remappedId) else keyword
        }

        replaceFacts(ownerEntityId, result.sourceUrl, facts)
        replaceRelationships(ownerEntityId, result.sourceUrl, relationships)
        replaceMedia(ownerEntityId, result.sourceUrl, media)
        insertKeywords(keywords)
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

        val canonicalIds = mutableMapOf<String, String>()
        result.entities.forEach { discovered ->
            canonicalIds[discovered.id] = upsertDiscoveredEntity(discovered, parentDepth + 1, now)
        }
        val facts = result.facts.mapNotNull { fact ->
            val canonicalId = canonicalIds[fact.entityId] ?: return@mapNotNull null
            fact.copy(entityId = canonicalId)
        }
        replaceFacts(ownerEntityId, result.sourceUrl, facts)
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
        entityTypes = knowledgeDao.entityCountsByType().associate { it.type to it.count },
    )

    private fun upsertDiscoveredEntity(
        discovered: KnowledgeEntity,
        discoveryDepth: Int,
        nowEpochMillis: Long,
    ): String {
        val existing = knowledgeDao.getEntity(discovered.id)
            ?: CanonicalEntityResolver.resolve(discovered, knowledgeDao.allEnabledEntities())
        val canonicalId = existing?.id ?: stableId(
            "entity",
            normalizeText(discovered.name),
            discovered.region,
            discovered.country,
        )
        val canonical = if (existing == null) {
            discovered.copy(id = canonicalId)
        } else {
            discovered.copy(
                id = canonicalId,
                name = existing.name,
                aliases = (discovered.aliases + discovered.name)
                    .distinctBy(::normalizeText),
            )
        }
        knowledgeDao.upsertEntity(
            canonical.toRow(
                nowEpochMillis = nowEpochMillis,
                discoveryDepth = discoveryDepth,
                existing = existing,
            ),
        )
        insertKeywords(CrawlBootstrap.keywordsFor(canonical))
        return canonicalId
    }

    private fun replaceFacts(
        ownerEntityId: String,
        resultSourceUrl: String,
        facts: List<com.akeshridev.johar.domain.source.SourceFact>,
    ) {
        val rows = facts.map { it.toRow() }
        val scopes = rows.mapTo(linkedSetOf()) { it.entityId to it.sourceUrl }
        scopes += ownerEntityId to resultSourceUrl
        scopes.forEach { (entityId, sourceUrl) -> knowledgeDao.deleteFactsForSource(entityId, sourceUrl) }
        if (rows.isNotEmpty()) knowledgeDao.upsertFacts(rows)
    }

    private fun replaceRelationships(
        ownerEntityId: String,
        resultSourceUrl: String,
        relationships: List<com.akeshridev.johar.domain.entity.EntityRelationship>,
    ) {
        val rows = relationships.map { it.toRow() }
        val scopes = rows.mapTo(linkedSetOf()) { it.fromEntityId to it.sourceUrl }
        scopes += ownerEntityId to resultSourceUrl
        scopes.forEach { (entityId, sourceUrl) -> knowledgeDao.deleteRelationshipsForSource(entityId, sourceUrl) }
        if (rows.isNotEmpty()) knowledgeDao.upsertRelationships(rows)
    }

    private fun replaceMedia(
        ownerEntityId: String,
        resultSourceUrl: String,
        media: List<com.akeshridev.johar.domain.media.MediaAsset>,
    ) {
        val rows = media.map { it.toRow() }
        val scopes = rows.mapTo(linkedSetOf()) { it.entityId to it.sourceUrl }
        scopes += ownerEntityId to resultSourceUrl
        scopes.forEach { (entityId, sourceUrl) -> knowledgeDao.deleteMediaForSource(entityId, sourceUrl) }
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
    val entityTypes: Map<String, Int>,
)
