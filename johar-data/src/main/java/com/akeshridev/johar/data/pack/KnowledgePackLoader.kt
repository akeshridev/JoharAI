package com.akeshridev.johar.data.pack

import android.content.Context
import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.crawl.stableId
import com.akeshridev.johar.data.local.EntityRelationshipRow
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow
import org.json.JSONArray
import org.json.JSONObject

class KnowledgePackLoader(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database = JoharDatabaseProvider.get(appContext)
    private val dao = database.knowledgeDao()

    fun load(): KnowledgePackLoadResult {
        val root = appContext.assets.open(ASSET_PATH).bufferedReader().use { JSONObject(it.readText()) }
        val version = root.getJSONObject("manifest").getString("packVersion")
        val sources = parseSources(root.getJSONArray("sources"))
        val entities = root.getJSONArray("entities")
        val facts = root.getJSONArray("facts")
        val relationships = root.getJSONArray("relations")
        val now = System.currentTimeMillis()

        val canonicalIds = buildMap<String, String> {
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val identityName = entity.optString("qualifiedName").takeIf(String::isNotBlank)
                    ?: entity.getString("name")
                val region = entity.optString("region", "Jharkhand")
                val country = entity.optString("country", "India")
                put(
                    entity.getString("id"),
                    stableId("entity", normalizeText(identityName), region, country),
                )
            }
        }
        val categories = buildMap<String, String> {
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                put(entity.getString("id"), entity.optString("category"))
            }
        }

        var importedEntities = 0
        var importedFacts = 0
        var importedRelationships = 0

        database.runInTransaction {
            for (index in 0 until entities.length()) {
                val packed = entities.getJSONObject(index)
                val packId = packed.getString("id")
                val canonicalId = requireNotNull(canonicalIds[packId])
                val existing = dao.getEntity(canonicalId)
                val aliases = linkedSetOf<String>().apply {
                    existing?.aliasesJson?.let(::stringList)?.let(::addAll)
                    addAll(stringList(packed.optJSONArray("aliases") ?: JSONArray()))
                    packed.optString("qualifiedName").takeIf(String::isNotBlank)?.let(::add)
                }
                val refs = linkedMapOf<String, String>().apply {
                    existing?.externalRefsJson?.let(::stringMap)?.let(::putAll)
                    put("joharPackId", packId)
                    put("joharPackVersion", version)
                    put("joharPackType", packed.optString("type", "OTHER"))
                    put("joharPackCategory", packed.optString("category", ""))
                }
                val identityName = packed.optString("qualifiedName").takeIf(String::isNotBlank)
                    ?: packed.getString("name")

                dao.upsertEntity(
                    KnowledgeEntityRow(
                        id = canonicalId,
                        name = packed.getString("name"),
                        normalizedName = normalizeText(identityName),
                        type = domainTypeFor(packed.optString("type", "OTHER")),
                        description = packed.optString("summary").takeIf(String::isNotBlank)
                            ?: existing?.description,
                        latitude = existing?.latitude,
                        longitude = existing?.longitude,
                        region = packed.optString("region", "Jharkhand"),
                        country = packed.optString("country", "India"),
                        aliasesJson = JSONArray(aliases.toList()).toString(),
                        externalRefsJson = JSONObject(refs as Map<*, *>).toString(),
                        discoveredAtEpochMillis = existing?.discoveredAtEpochMillis ?: now,
                        updatedAtEpochMillis = now,
                        lastCrawledAtEpochMillis = existing?.lastCrawledAtEpochMillis,
                        discoveryDepth = existing?.discoveryDepth ?: 0,
                        enabled = existing?.enabled ?: true,
                    ),
                )
                importedEntities += 1
            }

            for (index in 0 until facts.length()) {
                val packed = facts.getJSONObject(index)
                val packedEntityId = packed.getString("entityId")
                val entityId = canonicalIds[packedEntityId] ?: continue
                val field = packed.getString("field")
                val domain = domainFor(categories[packedEntityId].orEmpty(), field)
                val value = valueParts(packed.get("value"))
                val freshness = freshnessFor(packed.optString("freshness", "EVERGREEN"))
                val sourceIds = stringList(packed.optJSONArray("sourceIds") ?: JSONArray())
                    .ifEmpty { listOf(PACK_SOURCE_ID) }

                sourceIds.forEach { sourceId ->
                    val source = sources[sourceId]
                    val sourceUrl = source?.url ?: PACK_SOURCE_URL
                    val publisher = source?.publisher ?: PACK_PUBLISHER
                    dao.upsertFacts(
                        listOf(
                            SourceFactRow(
                                id = stableId("fact", entityId, sourceUrl, domain, field, value.stable),
                                entityId = entityId,
                                sourceUrl = sourceUrl,
                                publisher = publisher,
                                retrievedAtEpochMillis = now,
                                domain = domain,
                                field = field,
                                valueType = value.type,
                                textValue = value.text,
                                numberValue = value.number,
                                unit = null,
                                booleanValue = value.boolean,
                                evidenceText = "Johar knowledge pack $version",
                                factType = "SOURCE_FACT",
                                state = "KNOWN",
                                freshness = freshness,
                            ),
                        ),
                    )
                    importedFacts += 1
                }
            }

            for (index in 0 until relationships.length()) {
                val packed = relationships.getJSONObject(index)
                val fromId = canonicalIds[packed.optString("fromEntityId")] ?: continue
                val toId = canonicalIds[packed.optString("toEntityId")] ?: continue
                val predicate = packed.optString("predicate").takeIf(String::isNotBlank) ?: continue
                val sourceIds = stringList(packed.optJSONArray("sourceIds") ?: JSONArray())
                    .ifEmpty { listOf(PACK_SOURCE_ID) }

                sourceIds.forEach { sourceId ->
                    val source = sources[sourceId]
                    val sourceUrl = source?.url ?: PACK_SOURCE_URL
                    val publisher = source?.publisher ?: PACK_PUBLISHER
                    dao.upsertRelationships(
                        listOf(
                            EntityRelationshipRow(
                                id = stableId("relationship", fromId, toId, predicate, sourceUrl),
                                fromEntityId = fromId,
                                toEntityId = toId,
                                predicate = predicate,
                                sourceUrl = sourceUrl,
                                publisher = publisher,
                                retrievedAtEpochMillis = now,
                                evidenceText = "Johar knowledge pack $version",
                            ),
                        ),
                    )
                    importedRelationships += 1
                }
            }
        }

        return KnowledgePackLoadResult(
            version = version,
            entities = importedEntities,
            facts = importedFacts,
            relationships = importedRelationships,
        )
    }

    private fun parseSources(array: JSONArray): Map<String, PackSource> = buildMap {
        for (index in 0 until array.length()) {
            val source = array.getJSONObject(index)
            put(
                source.getString("id"),
                PackSource(
                    url = source.optString("url").takeIf(String::isNotBlank) ?: PACK_SOURCE_URL,
                    publisher = source.optString("publisher").takeIf(String::isNotBlank) ?: PACK_PUBLISHER,
                ),
            )
        }
    }

    private fun domainTypeFor(type: String): String = when (type) {
        "FOOD", "INGREDIENT" -> "FOOD"
        "FESTIVAL" -> "FESTIVAL"
        "DANCE", "ART", "CRAFT", "MUSICAL_INSTRUMENT", "CULTURAL_PRACTICE", "LANGUAGE", "TRIBE", "HISTORICAL_PERSON" -> "CULTURAL_PRACTICE"
        "MARKET_COLLECTION", "MARKET_TYPE" -> "MARKET"
        "CITY" -> "CITY"
        "TOWN" -> "TOWN"
        "DISTRICT" -> "DISTRICT"
        "REGION", "SUBDIVISION", "GEOGRAPHIC_REGION" -> "REGION"
        "RIVER" -> "RIVER"
        "AIRPORT" -> "AIRPORT"
        "RAILWAY_STATION" -> "RAILWAY_STATION"
        "WATERFALL", "DAM", "LAKE", "HILL", "FOREST", "NATIONAL_PARK", "WILDLIFE_SANCTUARY", "TIGER_RESERVE", "HOT_SPRING" -> "NATURAL_FEATURE"
        "TEMPLE", "PILGRIMAGE", "MUSEUM", "HERITAGE_SITE", "ASHRAM", "GARDEN", "LANDMARK", "VIEWPOINT", "PARK", "ADVENTURE_SITE", "BIOLOGICAL_PARK" -> "TOURIST_ATTRACTION"
        else -> "OTHER"
    }

    private fun domainFor(category: String, field: String): String {
        if (field.contains("emergency", ignoreCase = true)) return "SAFETY_EMERGENCY"
        return when (category) {
            "FOOD" -> "FOOD"
            "FESTIVAL", "CULTURE", "PEOPLE", "LANGUAGE", "HISTORY" -> "HISTORY_CULTURE"
            "TRANSPORT" -> "TRAVEL_LOGISTICS"
            "MARKET" -> "FACILITIES"
            else -> "GEOGRAPHY"
        }
    }

    private fun freshnessFor(value: String): String = when (value) {
        "LIVE" -> "LIVE"
        "SEMI_DYNAMIC", "SEASONAL" -> "SEASONAL"
        else -> "EVERGREEN"
    }

    private fun valueParts(value: Any): ValueParts = when (value) {
        is Number -> ValueParts("NUMBER", null, value.toDouble(), null, value.toString())
        is Boolean -> ValueParts("BOOLEAN", null, null, value, value.toString())
        is JSONObject, is JSONArray -> ValueParts("TEXT", value.toString(), null, null, value.toString())
        else -> ValueParts("TEXT", value.toString(), null, null, value.toString())
    }

    private fun stringList(array: JSONArray): List<String> = buildList {
        for (index in 0 until array.length()) {
            array.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun stringList(raw: String): List<String> = runCatching { stringList(JSONArray(raw)) }
        .getOrDefault(emptyList())

    private fun stringMap(raw: String): Map<String, String> = runCatching {
        val json = JSONObject(raw)
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                json.optString(key).takeIf(String::isNotBlank)?.let { put(key, it) }
            }
        }
    }.getOrDefault(emptyMap())

    private data class PackSource(val url: String, val publisher: String)
    private data class ValueParts(
        val type: String,
        val text: String?,
        val number: Double?,
        val boolean: Boolean?,
        val stable: String,
    )

    companion object {
        private const val ASSET_PATH = "johar/johar-knowledge-2026.09-mega-v1.2.json"
        private const val PACK_SOURCE_ID = "johar-pack"
        private const val PACK_SOURCE_URL = "asset://johar/knowledge-pack"
        private const val PACK_PUBLISHER = "Johar Knowledge Pack"
    }
}

data class KnowledgePackLoadResult(
    val version: String,
    val entities: Int,
    val facts: Int,
    val relationships: Int,
)
