package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Narrow OSM discovery for Ranchi education entities.
 *
 * Education is kept separate from generic place discovery because a query such as
 * "schools in Ranchi" must never degrade into a tourism scan. Results remain canonical
 * KnowledgeEntity rows and are later enriched by the normal entity/source pipeline.
 */
internal class RanchiEducationDiscoveryAdapter(
    private val fetcher: HttpTextFetcher,
    private val endpoint: String = DEFAULT_ENDPOINT,
) : KeywordDiscoveryAdapter {
    override val id: String = "openstreetmap_ranchi_education"

    override fun supports(keyword: CrawlKeyword): Boolean {
        if (keyword.discoveredFrom != "bootstrap") return false
        if (keyword.category != DiscoveryCategory.PLACES) return false
        val term = normalizeText(keyword.term)
        return "ranchi" in term && EDUCATION_TERMS.any(term::contains)
    }

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val term = normalizeText(keyword.term)
        val amenity = when {
            "school" in term -> "school"
            "college" in term -> "college"
            "universit" in term -> "university"
            "librar" in term -> "library"
            else -> error("Unsupported education seed: ${keyword.term}")
        }
        val query = """
            [out:json][timeout:30];
            area["boundary"="administrative"]["name"="Ranchi"]->.searchArea;
            nwr(area.searchArea)["amenity"="$amenity"]["name"];
            out center tags $DISCOVERY_LIMIT;
        """.trimIndent()
        val raw = fetcher.postForm(endpoint, mapOf("data" to query))
        val elements = JSONObject(raw).optJSONArray("elements") ?: JSONArray()
        val entities = buildList {
            for (index in 0 until elements.length()) {
                val element = elements.optJSONObject(index) ?: continue
                val tags = element.optJSONObject("tags") ?: continue
                val name = tags.optString("name").takeIf(String::isNotBlank) ?: continue
                val point = coordinates(element)
                add(
                    KnowledgeEntity(
                        id = "osm:${element.optString("type")}:${element.optLong("id")}",
                        name = name,
                        type = EntityType.FACILITY,
                        description = description(tags),
                        latitude = point?.first,
                        longitude = point?.second,
                        region = "Jharkhand",
                        country = "India",
                        aliases = aliases(tags),
                        externalRefs = mapOf(
                            "osm" to "${element.optString("type")}:${element.optLong("id")}",
                            "joharDiscoveryScope" to "Ranchi",
                            "joharPackType" to packType(amenity),
                        ),
                    ),
                )
            }
        }
        return DiscoveryResult(
            sourceUrl = "overpass:${normalizeText(keyword.term)}",
            publisher = PUBLISHER,
            rawContent = raw,
            entities = entities.distinctBy { it.id },
        )
    }

    private fun packType(amenity: String): String = when (amenity) {
        "school" -> "SCHOOL"
        "college" -> "COLLEGE"
        "university" -> "UNIVERSITY"
        "library" -> "LIBRARY"
        else -> "EDUCATION"
    }

    private fun coordinates(element: JSONObject): Pair<Double, Double>? {
        val lat = if (element.has("lat")) element.optDouble("lat", Double.NaN) else Double.NaN
        val lon = if (element.has("lon")) element.optDouble("lon", Double.NaN) else Double.NaN
        if (!lat.isNaN() && !lon.isNaN()) return lat to lon
        val center = element.optJSONObject("center") ?: return null
        val centerLat = center.optDouble("lat", Double.NaN)
        val centerLon = center.optDouble("lon", Double.NaN)
        return if (!centerLat.isNaN() && !centerLon.isNaN()) centerLat to centerLon else null
    }

    private fun description(tags: JSONObject): String {
        val address = listOf(
            tags.optString("addr:housenumber"),
            tags.optString("addr:street"),
            tags.optString("addr:suburb"),
            tags.optString("addr:city"),
        ).filter(String::isNotBlank).joinToString(", ")
        return sequenceOf(
            tags.optString("description"),
            tags.optString("description:en"),
            address,
            tags.optString("operator"),
            "Education facility in Ranchi",
        ).first(String::isNotBlank)
    }

    private fun aliases(tags: JSONObject): List<String> = buildList {
        listOf("alt_name", "old_name", "short_name", "name:hi").forEach { key ->
            tags.optString(key)
                .split(';')
                .map(String::trim)
                .filter(String::isNotBlank)
                .forEach(::add)
        }
    }.distinctBy(::normalizeText)

    companion object {
        private const val PUBLISHER = "OpenStreetMap contributors"
        private const val DEFAULT_ENDPOINT = "https://overpass-api.de/api/interpreter"
        private const val DISCOVERY_LIMIT = 150
        private val EDUCATION_TERMS = listOf("school", "college", "universit", "librar")
    }
}
