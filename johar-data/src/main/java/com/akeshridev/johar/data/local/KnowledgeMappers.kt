package com.akeshridev.johar.data.local

import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.crawl.stableId
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityRelationship
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import com.akeshridev.johar.domain.media.MediaAsset
import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.SourceFact
import org.json.JSONArray
import org.json.JSONObject

internal fun KnowledgeEntity.toRow(
    nowEpochMillis: Long,
    discoveryDepth: Int,
    existing: KnowledgeEntityRow? = null,
): KnowledgeEntityRow {
    val mergedAliases = (existing?.aliasesJson?.toStringList().orEmpty() + aliases)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy(::normalizeText)

    val mergedRefs = existing?.externalRefsJson?.toStringMap().orEmpty() + externalRefs

    return KnowledgeEntityRow(
        id = id,
        name = name,
        normalizedName = normalizeText(name),
        type = type.name,
        description = description ?: existing?.description,
        latitude = latitude ?: existing?.latitude,
        longitude = longitude ?: existing?.longitude,
        region = region ?: existing?.region,
        country = country ?: existing?.country,
        aliasesJson = mergedAliases.toJsonArray(),
        externalRefsJson = mergedRefs.toJsonObject(),
        discoveredAtEpochMillis = existing?.discoveredAtEpochMillis ?: nowEpochMillis,
        updatedAtEpochMillis = nowEpochMillis,
        lastCrawledAtEpochMillis = existing?.lastCrawledAtEpochMillis,
        discoveryDepth = minOf(existing?.discoveryDepth ?: discoveryDepth, discoveryDepth),
        enabled = existing?.enabled ?: true,
    )
}

internal fun KnowledgeEntityRow.toCrawlSeed(): CrawlSeed = CrawlSeed(
    name = name,
    region = region ?: "Jharkhand",
    country = country ?: "India",
    entityId = id,
    entityType = runCatching { EntityType.valueOf(type) }.getOrDefault(EntityType.OTHER),
    latitude = latitude,
    longitude = longitude,
    externalRefs = externalRefsJson.toStringMap(),
    depth = discoveryDepth,
)

internal fun SourceFact.toRow(): SourceFactRow {
    val (valueType, textValue, numberValue, unit, booleanValue, stableValue) = when (val factValue = value) {
        is FactValue.Text -> ValueParts("TEXT", factValue.value, null, null, null, factValue.value)
        is FactValue.Number -> ValueParts(
            "NUMBER",
            null,
            factValue.value,
            factValue.unit,
            null,
            "${factValue.value}|${factValue.unit.orEmpty()}",
        )
        is FactValue.BooleanValue -> ValueParts(
            "BOOLEAN",
            null,
            null,
            null,
            factValue.value,
            factValue.value.toString(),
        )
    }

    return SourceFactRow(
        id = stableId("fact", entityId, sourceUrl, domain.name, field, stableValue),
        entityId = entityId,
        sourceUrl = sourceUrl,
        publisher = publisher,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        domain = domain.name,
        field = field,
        valueType = valueType,
        textValue = textValue,
        numberValue = numberValue,
        unit = unit,
        booleanValue = booleanValue,
        evidenceText = evidenceText,
        factType = factType.name,
        state = state.name,
        freshness = freshness.name,
    )
}

internal fun EntityRelationship.toRow(): EntityRelationshipRow = EntityRelationshipRow(
    id = id,
    fromEntityId = fromEntityId,
    toEntityId = toEntityId,
    predicate = predicate,
    sourceUrl = sourceUrl,
    publisher = publisher,
    retrievedAtEpochMillis = retrievedAtEpochMillis,
    evidenceText = evidenceText,
)

internal fun MediaAsset.toRow(): MediaAssetRow = MediaAssetRow(
    id = id,
    entityId = entityId,
    type = type.name,
    sourceUrl = sourceUrl,
    mediaUrl = mediaUrl,
    previewUrl = previewUrl,
    title = title,
    description = description,
    creator = creator,
    attributionText = attributionText,
    license = license,
    licenseUrl = licenseUrl,
    mimeType = mimeType,
    width = width,
    height = height,
    durationMillis = durationMillis,
)

internal fun CrawlKeyword.toRow(): CrawlKeywordRow = CrawlKeywordRow(
    id = id,
    entityId = entityId,
    category = category.name,
    term = term,
    normalizedTerm = normalizeText(term),
    discoveredFrom = discoveredFrom,
    lastCrawledAtEpochMillis = lastCrawledAtEpochMillis,
    enabled = enabled,
    failureCount = 0,
)

internal fun CrawlKeywordRow.toDomain(): CrawlKeyword = CrawlKeyword(
    id = id,
    entityId = entityId,
    category = runCatching { DiscoveryCategory.valueOf(category) }
        .getOrDefault(DiscoveryCategory.PLACES),
    term = term,
    discoveredFrom = discoveredFrom,
    lastCrawledAtEpochMillis = lastCrawledAtEpochMillis,
    enabled = enabled,
)

private data class ValueParts(
    val type: String,
    val text: String?,
    val number: Double?,
    val unit: String?,
    val boolean: Boolean?,
    val stableValue: String,
)

private fun List<String>.toJsonArray(): String = JSONArray().apply {
    forEach { put(it) }
}.toString()

private fun Map<String, String>.toJsonObject(): String = JSONObject().apply {
    forEach { (key, value) -> put(key, value) }
}.toString()

private fun String.toStringList(): List<String> = runCatching {
    val json = JSONArray(this)
    buildList {
        for (index in 0 until json.length()) {
            json.optString(index)
                .takeIf(String::isNotBlank)
                ?.let(::add)
        }
    }
}.getOrDefault(emptyList())

private fun String.toStringMap(): Map<String, String> = runCatching {
    val json = JSONObject(this)
    buildMap {
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            json.optString(key)
                .takeIf(String::isNotBlank)
                ?.let { put(key, it) }
        }
    }
}.getOrDefault(emptyMap())
