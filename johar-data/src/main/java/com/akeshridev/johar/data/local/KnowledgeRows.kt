package com.akeshridev.johar.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_entities",
    indices = [
        Index(value = ["normalizedName"]),
        Index(value = ["lastCrawledAtEpochMillis"]),
        Index(value = ["type"]),
    ],
)
data class KnowledgeEntityRow(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val type: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val region: String?,
    val country: String?,
    val aliasesJson: String,
    val externalRefsJson: String,
    val discoveredAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val lastCrawledAtEpochMillis: Long?,
    val discoveryDepth: Int,
    val enabled: Boolean,
)

@Entity(
    tableName = "source_facts",
    indices = [
        Index(value = ["entityId"]),
        Index(value = ["domain"]),
        Index(value = ["sourceUrl"]),
    ],
)
data class SourceFactRow(
    @PrimaryKey val id: String,
    val entityId: String,
    val sourceUrl: String,
    val publisher: String,
    val retrievedAtEpochMillis: Long,
    val domain: String,
    val field: String,
    val valueType: String,
    val textValue: String?,
    val numberValue: Double?,
    val unit: String?,
    val booleanValue: Boolean?,
    val evidenceText: String,
    val factType: String,
    val state: String,
    val freshness: String,
)

@Entity(
    tableName = "entity_relationships",
    indices = [
        Index(value = ["fromEntityId"]),
        Index(value = ["toEntityId"]),
        Index(value = ["predicate"]),
    ],
)
data class EntityRelationshipRow(
    @PrimaryKey val id: String,
    val fromEntityId: String,
    val toEntityId: String,
    val predicate: String,
    val sourceUrl: String,
    val publisher: String,
    val retrievedAtEpochMillis: Long,
    val evidenceText: String,
)

@Entity(
    tableName = "media_assets",
    indices = [
        Index(value = ["entityId"]),
        Index(value = ["type"]),
    ],
)
data class MediaAssetRow(
    @PrimaryKey val id: String,
    val entityId: String,
    val type: String,
    val sourceUrl: String,
    val mediaUrl: String,
    val previewUrl: String?,
    val title: String?,
    val description: String?,
    val creator: String?,
    val attributionText: String?,
    val license: String?,
    val licenseUrl: String?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val durationMillis: Long?,
)

@Entity(
    tableName = "crawl_keywords",
    indices = [
        Index(value = ["entityId"]),
        Index(value = ["category"]),
        Index(value = ["lastCrawledAtEpochMillis"]),
    ],
)
data class CrawlKeywordRow(
    @PrimaryKey val id: String,
    val entityId: String?,
    val category: String,
    val term: String,
    val normalizedTerm: String,
    val discoveredFrom: String,
    val lastCrawledAtEpochMillis: Long?,
    val enabled: Boolean,
    val failureCount: Int,
)
