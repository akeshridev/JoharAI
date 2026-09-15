package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.Freshness
import com.akeshridev.johar.domain.source.KnowledgeDomain
import com.akeshridev.johar.domain.source.SourceFact
import org.json.JSONArray
import org.json.JSONObject

/**
 * Extracts a conservative set of static/practical OSM tags into canonical source facts.
 *
 * These facts describe published metadata only. In particular, `opening_hours` must not be
 * interpreted as proof that a place is open right now.
 */
internal object OsmPracticalFactExtractor {
    fun fromOverpassRaw(
        rawContent: String,
        publisher: String,
        retrievedAtEpochMillis: Long = System.currentTimeMillis(),
    ): List<SourceFact> {
        val elements = runCatching { JSONObject(rawContent).optJSONArray("elements") }
            .getOrNull()
            ?: JSONArray()

        return buildList {
            for (index in 0 until elements.length()) {
                val element = elements.optJSONObject(index) ?: continue
                val type = element.optString("type").takeIf(String::isNotBlank) ?: continue
                val osmId = element.optLong("id").takeIf { it > 0L } ?: continue
                val tags = element.optJSONObject("tags") ?: continue
                val entityId = "osm:$type:$osmId"
                val sourceUrl = "https://www.openstreetmap.org/$type/$osmId"

                val keys = tags.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (!isUseful(key)) continue
                    val value = tags.optString(key).trim()
                    if (value.isBlank()) continue

                    add(
                        SourceFact(
                            entityId = entityId,
                            sourceUrl = sourceUrl,
                            publisher = publisher,
                            retrievedAtEpochMillis = retrievedAtEpochMillis,
                            domain = domainFor(key),
                            field = "osm.$key",
                            value = value.toFactValue(),
                            evidenceText = "OpenStreetMap tag $key=$value",
                            freshness = freshnessFor(key),
                        ),
                    )
                }
            }
        }
    }

    private fun isUseful(key: String): Boolean =
        key in USEFUL_KEYS ||
            key.startsWith("addr:") ||
            key.startsWith("diet:") ||
            key.startsWith("fuel:") ||
            key.startsWith("contact:")

    private fun domainFor(key: String): KnowledgeDomain = when {
        key == "cuisine" || key.startsWith("diet:") -> KnowledgeDomain.FOOD
        key in setOf("wheelchair", "toilets", "parking", "fee", "internet_access") ->
            KnowledgeDomain.FACILITIES
        key in setOf("phone", "contact:phone", "emergency", "healthcare") ->
            KnowledgeDomain.SAFETY_EMERGENCY
        key.startsWith("addr:") -> KnowledgeDomain.GEOGRAPHY
        key in setOf("opening_hours", "operator", "website", "contact:website") ->
            KnowledgeDomain.TRAVEL_LOGISTICS
        key.startsWith("fuel:") -> KnowledgeDomain.TRAVEL_LOGISTICS
        else -> KnowledgeDomain.FACILITIES
    }

    private fun freshnessFor(key: String): Freshness = when (key) {
        "opening_hours" -> Freshness.SEASONAL
        else -> Freshness.EVERGREEN
    }

    private fun String.toFactValue(): FactValue = when (lowercase()) {
        "yes" -> FactValue.BooleanValue(true)
        "no" -> FactValue.BooleanValue(false)
        else -> FactValue.Text(this)
    }

    private val USEFUL_KEYS = setOf(
        "amenity",
        "shop",
        "office",
        "leisure",
        "tourism",
        "healthcare",
        "emergency",
        "phone",
        "website",
        "opening_hours",
        "operator",
        "cuisine",
        "wheelchair",
        "toilets",
        "parking",
        "fee",
        "internet_access",
        "brand",
        "network",
    )
}
