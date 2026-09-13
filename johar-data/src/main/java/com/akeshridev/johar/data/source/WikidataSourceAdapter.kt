package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.crawl.stableId
import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityRelationship
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import com.akeshridev.johar.domain.media.MediaAsset
import com.akeshridev.johar.domain.media.MediaType
import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.Freshness
import com.akeshridev.johar.domain.source.KnowledgeDomain
import com.akeshridev.johar.domain.source.SourceFact
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class WikidataSourceAdapter(
    private val fetcher: HttpTextFetcher,
) : CrawlSourceAdapter, KeywordDiscoveryAdapter {
    override val id: String = "wikidata"

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val ownerId = requireNotNull(seed.entityId)
        val qid = seed.externalRefs[REF_WIKIDATA] ?: resolveQid(seed) ?: return null
        val sourceUrl = "$ENTITY_DATA_BASE/$qid.json"
        val raw = fetcher.get(sourceUrl)
        val root = JSONObject(raw)
            .optJSONObject("entities")
            ?.optJSONObject(qid)
            ?: return null

        val label = localizedValue(root.optJSONObject("labels"), "en")
            ?: localizedValue(root.optJSONObject("labels"), "hi")
            ?: seed.name
        val description = localizedValue(root.optJSONObject("descriptions"), "en")
            ?: localizedValue(root.optJSONObject("descriptions"), "hi")
        val aliases = buildList {
            addAll(localizedAliases(root.optJSONObject("aliases"), "en"))
            addAll(localizedAliases(root.optJSONObject("aliases"), "hi"))
        }.distinctBy(::normalizeText)

        val claims = root.optJSONObject("claims") ?: JSONObject()
        val coordinate = firstCoordinate(claims.optJSONArray(P_COORDINATE))
        val inferredType = inferEntityType(
            description = description,
            fallback = seed.entityType ?: EntityType.OTHER,
        )
        val entity = KnowledgeEntity(
            id = ownerId,
            name = label,
            type = inferredType,
            description = description,
            latitude = coordinate?.first ?: seed.latitude,
            longitude = coordinate?.second ?: seed.longitude,
            region = seed.region,
            country = seed.country,
            aliases = aliases,
            externalRefs = seed.externalRefs + (REF_WIKIDATA to qid),
        )

        val retrievedAt = System.currentTimeMillis()
        val facts = mutableListOf<SourceFact>()
        val relationships = mutableListOf<EntityRelationship>()
        val media = mutableListOf<MediaAsset>()

        val propertyKeys = claims.keys()
        while (propertyKeys.hasNext()) {
            val property = propertyKeys.next()
            val propertyClaims = claims.optJSONArray(property) ?: continue
            for (index in 0 until minOf(propertyClaims.length(), MAX_CLAIMS_PER_PROPERTY)) {
                val claim = propertyClaims.optJSONObject(index) ?: continue
                val snak = claim.optJSONObject("mainsnak") ?: continue
                val dataValue = snak.optJSONObject("datavalue") ?: continue
                val value = dataValue.opt("value") ?: continue
                val evidence = claim.toString().take(MAX_EVIDENCE_CHARS)

                if (property == P_IMAGE && value is String) {
                    val fileUrl = "$COMMONS_FILE_PATH/${encodePath(value)}"
                    media += MediaAsset(
                        id = stableId("media", ownerId, sourceUrl, value),
                        entityId = ownerId,
                        type = MediaType.IMAGE,
                        sourceUrl = sourceUrl,
                        mediaUrl = fileUrl,
                        title = value,
                    )
                    continue
                }

                if (value is JSONObject && value.has("entity-type") && value.has("id")) {
                    val targetQid = value.optString("id")
                    if (targetQid.isBlank()) continue

                    if (property in RECURSIVE_RELATION_PROPERTIES) {
                        // Preserve the source-backed relationship without manufacturing a user-facing
                        // Qxxxx entity. The target can be materialized later only after label/type
                        // resolution succeeds.
                        val targetId = "wikidata:$targetQid"
                        relationships += EntityRelationship(
                            id = stableId("rel", ownerId, targetId, "wikidata.$property", sourceUrl),
                            fromEntityId = ownerId,
                            toEntityId = targetId,
                            predicate = "wikidata.$property",
                            sourceUrl = sourceUrl,
                            publisher = PUBLISHER,
                            retrievedAtEpochMillis = retrievedAt,
                            evidenceText = evidence,
                        )
                    } else {
                        facts += fact(
                            ownerId = ownerId,
                            sourceUrl = sourceUrl,
                            retrievedAt = retrievedAt,
                            property = property,
                            value = FactValue.Text(targetQid),
                            evidence = evidence,
                        )
                    }
                    continue
                }

                when (value) {
                    is String -> facts += fact(
                        ownerId,
                        sourceUrl,
                        retrievedAt,
                        property,
                        FactValue.Text(value),
                        evidence,
                    )
                    is Number -> facts += fact(
                        ownerId,
                        sourceUrl,
                        retrievedAt,
                        property,
                        FactValue.Number(value.toDouble()),
                        evidence,
                    )
                    is JSONObject -> parseStructuredValue(value)?.let { parsed ->
                        facts += fact(
                            ownerId,
                            sourceUrl,
                            retrievedAt,
                            property,
                            parsed,
                            evidence,
                        )
                    }
                }
            }
        }

        coordinate?.let { (latitude, longitude) ->
            facts += SourceFact(
                entityId = ownerId,
                sourceUrl = sourceUrl,
                publisher = PUBLISHER,
                retrievedAtEpochMillis = retrievedAt,
                domain = KnowledgeDomain.GEOGRAPHY,
                field = "latitude",
                value = FactValue.Number(latitude, "degrees"),
                evidenceText = "Wikidata $P_COORDINATE coordinate",
                freshness = Freshness.EVERGREEN,
            )
            facts += SourceFact(
                entityId = ownerId,
                sourceUrl = sourceUrl,
                publisher = PUBLISHER,
                retrievedAtEpochMillis = retrievedAt,
                domain = KnowledgeDomain.GEOGRAPHY,
                field = "longitude",
                value = FactValue.Number(longitude, "degrees"),
                evidenceText = "Wikidata $P_COORDINATE coordinate",
                freshness = Freshness.EVERGREEN,
            )
        }

        val keywords = aliases.take(MAX_ALIAS_KEYWORDS).map { alias ->
            CrawlKeyword(
                id = stableId("keyword", ownerId, DiscoveryCategory.PLACES.name, normalizeText(alias)),
                entityId = ownerId,
                category = categoryFor(inferredType),
                term = alias,
                discoveredFrom = sourceUrl,
            )
        }

        return SourceResult(
            sourceUrl = sourceUrl,
            publisher = PUBLISHER,
            rawContent = raw,
            entity = entity,
            facts = facts,
            relationships = relationships,
            media = media,
            discoveredKeywords = keywords,
        )
    }

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val sourceUrl = searchUrl(keyword.term, SEARCH_LIMIT)
        val raw = fetcher.get(sourceUrl)
        val results = JSONObject(raw).optJSONArray("search") ?: JSONArray()
        val entities = buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val qid = item.optString("id")
                val label = item.optString("label")
                if (qid.isBlank() || label.isBlank()) continue
                val description = item.optString("description").takeIf(String::isNotBlank)
                val inferredType = inferEntityType(description, EntityType.OTHER)
                if (!DiscoveryQualityGate.accept(keyword, label, description, inferredType)) continue

                add(
                    KnowledgeEntity(
                        id = "wikidata:$qid",
                        name = label,
                        type = inferredType.takeUnless { it == EntityType.OTHER }
                            ?: typeFor(keyword.category),
                        description = description,
                        region = "Jharkhand",
                        country = "India",
                        externalRefs = mapOf(REF_WIKIDATA to qid),
                    ),
                )
            }
        }
        return DiscoveryResult(
            sourceUrl = sourceUrl,
            publisher = PUBLISHER,
            rawContent = raw,
            entities = entities.distinctBy { it.id },
        )
    }

    private fun resolveQid(seed: CrawlSeed): String? {
        val raw = fetcher.get(searchUrl(seed.name, 8))
        val results = JSONObject(raw).optJSONArray("search") ?: return null
        if (results.length() == 0) return null

        val normalizedSeed = normalizeText(seed.name)
        var bestId: String? = null
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val qid = item.optString("id")
            if (qid.isBlank()) continue
            val label = item.optString("label")
            val description = item.optString("description")
            var score = 0
            if (normalizeText(label) == normalizedSeed) score += 100
            if (description.contains(seed.region, ignoreCase = true)) score += 20
            if (description.contains(seed.country, ignoreCase = true)) score += 10
            if (score > bestScore) {
                bestScore = score
                bestId = qid
            }
        }
        return bestId
    }

    private fun fact(
        ownerId: String,
        sourceUrl: String,
        retrievedAt: Long,
        property: String,
        value: FactValue,
        evidence: String,
    ): SourceFact = SourceFact(
        entityId = ownerId,
        sourceUrl = sourceUrl,
        publisher = PUBLISHER,
        retrievedAtEpochMillis = retrievedAt,
        domain = domainFor(property),
        field = "wikidata.$property",
        value = value,
        evidenceText = evidence,
        freshness = Freshness.EVERGREEN,
    )

    private fun parseStructuredValue(value: JSONObject): FactValue? = when {
        value.has("text") -> value.optString("text")
            .takeIf(String::isNotBlank)
            ?.let(FactValue::Text)
        value.has("amount") -> value.optString("amount")
            .removePrefix("+")
            .toDoubleOrNull()
            ?.let { amount ->
                FactValue.Number(
                    value = amount,
                    unit = value.optString("unit")
                        .takeIf { it.isNotBlank() && it != "1" }
                        ?.substringAfterLast('/'),
                )
            }
        value.has("time") -> value.optString("time")
            .takeIf(String::isNotBlank)
            ?.let(FactValue::Text)
        value.has("latitude") && value.has("longitude") -> FactValue.Text(
            "${value.optDouble("latitude")},${value.optDouble("longitude")}",
        )
        else -> value.toString().takeIf(String::isNotBlank)?.let(FactValue::Text)
    }

    private fun firstCoordinate(claims: JSONArray?): Pair<Double, Double>? {
        if (claims == null) return null
        for (index in 0 until claims.length()) {
            val value = claims.optJSONObject(index)
                ?.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optJSONObject("value")
                ?: continue
            if (value.has("latitude") && value.has("longitude")) {
                return value.optDouble("latitude") to value.optDouble("longitude")
            }
        }
        return null
    }

    private fun localizedValue(container: JSONObject?, language: String): String? = container
        ?.optJSONObject(language)
        ?.optString("value")
        ?.takeIf(String::isNotBlank)

    private fun localizedAliases(container: JSONObject?, language: String): List<String> {
        val aliases = container?.optJSONArray(language) ?: return emptyList()
        return buildList {
            for (index in 0 until aliases.length()) {
                aliases.optJSONObject(index)
                    ?.optString("value")
                    ?.takeIf(String::isNotBlank)
                    ?.let(::add)
            }
        }
    }

    private fun searchUrl(query: String, limit: Int): String =
        "$API?action=wbsearchentities&format=json&language=en&uselang=en&type=item" +
            "&limit=$limit&search=${encodeQuery(query)}"

    private fun domainFor(property: String): KnowledgeDomain = when (property) {
        P_COORDINATE, "P131", "P17", "P2044", "P206", "P403", "P706", "P1082" ->
            KnowledgeDomain.GEOGRAPHY
        "P856", P_IMAGE, "P373" -> KnowledgeDomain.TOURISM
        "P571", "P580", "P582", "P1619", "P577" -> KnowledgeDomain.HISTORY_CULTURE
        else -> KnowledgeDomain.HISTORY_CULTURE
    }

    private fun categoryFor(type: EntityType): DiscoveryCategory = when (type) {
        EntityType.FOOD -> DiscoveryCategory.FOOD
        EntityType.FESTIVAL -> DiscoveryCategory.FESTIVALS
        EntityType.CULTURAL_PRACTICE -> DiscoveryCategory.CULTURE
        EntityType.MARKET, EntityType.SHOP, EntityType.RESTAURANT -> DiscoveryCategory.LOCAL_BAZAR
        EntityType.HOSPITAL, EntityType.POLICE_STATION, EntityType.EMERGENCY_SERVICE -> DiscoveryCategory.EMERGENCY
        else -> DiscoveryCategory.PLACES
    }

    private fun typeFor(category: DiscoveryCategory): EntityType = when (category) {
        DiscoveryCategory.FOOD -> EntityType.FOOD
        DiscoveryCategory.FESTIVALS -> EntityType.FESTIVAL
        DiscoveryCategory.CULTURE -> EntityType.CULTURAL_PRACTICE
        DiscoveryCategory.LOCAL_BAZAR -> EntityType.MARKET
        DiscoveryCategory.EMERGENCY -> EntityType.EMERGENCY_SERVICE
        else -> EntityType.PLACE
    }

    private fun inferEntityType(description: String?, fallback: EntityType): EntityType {
        val text = normalizeText(description.orEmpty())
        return when {
            "waterfall" in text -> EntityType.TOURIST_ATTRACTION
            "festival" in text -> EntityType.FESTIVAL
            "food" in text || "dish" in text || "cuisine" in text -> EntityType.FOOD
            "market" in text || "bazaar" in text || "bazar" in text || "haat" in text -> EntityType.MARKET
            "hospital" in text -> EntityType.HOSPITAL
            "police" in text -> EntityType.POLICE_STATION
            "river" in text -> EntityType.RIVER
            "village" in text -> EntityType.VILLAGE
            "town" in text -> EntityType.TOWN
            "city" in text -> EntityType.CITY
            "district" in text -> EntityType.DISTRICT
            "airport" in text -> EntityType.AIRPORT
            "railway" in text || "railroad station" in text -> EntityType.RAILWAY_STATION
            listOf("dance", "music", "language", "tribe", "tribal", "folk", "art", "craft", "painting", "culture").any(text::contains) ->
                EntityType.CULTURAL_PRACTICE
            else -> fallback
        }
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun encodePath(value: String): String = value
        .split('/')
        .joinToString("/") { encodeQuery(it).replace("+", "%20") }

    companion object {
        private const val PUBLISHER = "Wikidata"
        private const val API = "https://www.wikidata.org/w/api.php"
        private const val ENTITY_DATA_BASE = "https://www.wikidata.org/wiki/Special:EntityData"
        private const val COMMONS_FILE_PATH = "https://commons.wikimedia.org/wiki/Special:FilePath"
        private const val REF_WIKIDATA = "wikidata"
        private const val P_COORDINATE = "P625"
        private const val P_IMAGE = "P18"
        private const val SEARCH_LIMIT = 20
        private const val MAX_CLAIMS_PER_PROPERTY = 20
        private const val MAX_EVIDENCE_CHARS = 2_000
        private const val MAX_ALIAS_KEYWORDS = 8

        private val RECURSIVE_RELATION_PROPERTIES = setOf(
            "P131",
            "P361",
            "P403",
            "P706",
        )
    }
}
