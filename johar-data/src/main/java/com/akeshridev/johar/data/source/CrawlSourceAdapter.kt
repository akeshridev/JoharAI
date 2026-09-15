package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.entity.EntityRelationship
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import com.akeshridev.johar.domain.media.MediaAsset
import com.akeshridev.johar.domain.source.SourceFact

internal interface CrawlSourceAdapter {
    val id: String

    fun supports(seed: CrawlSeed): Boolean = true

    fun crawl(seed: CrawlSeed): SourceResult?
}

internal interface KeywordDiscoveryAdapter {
    val id: String

    fun supports(keyword: CrawlKeyword): Boolean = true

    fun discover(keyword: CrawlKeyword): DiscoveryResult
}

internal data class SourceResult(
    val sourceUrl: String,
    val publisher: String,
    val rawContent: String,
    val entity: KnowledgeEntity? = null,
    val discoveredEntities: List<KnowledgeEntity> = emptyList(),
    val facts: List<SourceFact> = emptyList(),
    val relationships: List<EntityRelationship> = emptyList(),
    val media: List<MediaAsset> = emptyList(),
    val discoveredKeywords: List<CrawlKeyword> = emptyList(),
)

internal data class DiscoveryResult(
    val sourceUrl: String,
    val publisher: String,
    val rawContent: String,
    val entities: List<KnowledgeEntity> = emptyList(),
    val facts: List<SourceFact> = emptyList(),
    val keywords: List<CrawlKeyword> = emptyList(),
)
