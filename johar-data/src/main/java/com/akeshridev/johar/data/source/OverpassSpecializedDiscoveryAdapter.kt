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
 * Adds precise OSM discovery for V1 concepts that should not fall back to a generic
 * tourism/marketplace query. It only returns named OSM elements that explicitly match
 * the requested concept; it does not infer stock/availability from nearby shops.
 */
internal class OverpassSpecializedDiscoveryAdapter(
    private val fetcher: HttpTextFetcher,
    private val endpoint: String = DEFAULT_ENDPOINT,
) : KeywordDiscoveryAdapter {
    override val id: String = "openstreetmap_specialized"

    override fun supports(keyword: CrawlKeyword): Boolean =
        keyword.discoveredFrom == "bootstrap" && selectorFor(keyword) != null

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val selector = requireNotNull(selectorFor(keyword))
        val query = """
            [out:json][timeout:30];
            area["boundary"="administrative"]["name"="Jharkhand"]->.searchArea;
            (
              $selector
            );
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
                        id = osmEntityId(element),
                        name = name,
                        type = typeFor(keyword, tags),
                        description = description(tags),
                        latitude = point?.first,
                        longitude = point?.second,
                        region = "Jharkhand",
                        country = "India",
                        aliases = aliases(tags),
                        externalRefs = mapOf("osm" to "${element.optString("type")}:${element.optLong("id")}"),
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

    private fun selectorFor(keyword: CrawlKeyword): String? {
        val term = normalizeText(keyword.term)
        return when (keyword.category) {
            DiscoveryCategory.PLACES -> when {
                "hill" in term -> "nwr(area.searchArea)[\"natural\"~\"peak|ridge|hill\"][\"name\"];"
                "lake" in term -> "nwr(area.searchArea)[\"natural\"=\"water\"][\"water\"~\"lake|reservoir\"][\"name\"];"
                "dam" in term -> "nwr(area.searchArea)[\"waterway\"=\"dam\"][\"name\"];"
                "temple" in term -> "nwr(area.searchArea)[\"amenity\"=\"place_of_worship\"][\"religion\"=\"hindu\"][\"name\"];"
                "sanctuar" in term -> "nwr(area.searchArea)[\"boundary\"=\"protected_area\"][\"name\"];"
                "heritage" in term -> "nwr(area.searchArea)[\"heritage\"][\"name\"];"
                else -> null
            }

            DiscoveryCategory.EMERGENCY -> when {
                "fire" in term -> "nwr(area.searchArea)[\"amenity\"=\"fire_station\"][\"name\"];"
                "ambulance" in term -> "nwr(area.searchArea)[\"emergency\"=\"ambulance_station\"][\"name\"];"
                else -> null
            }

            DiscoveryCategory.LOCAL_BAZAR -> when {
                "pork" in term -> """
                    nwr(area.searchArea)["shop"="butcher"]["butcher"~"pork|pig",i]["name"];
                    nwr(area.searchArea)["shop"="butcher"]["name"~"pork|pig",i];
                """.trimIndent()
                "fish" in term -> """
                    nwr(area.searchArea)["shop"="seafood"]["name"];
                    nwr(area.searchArea)["shop"="fishmonger"]["name"];
                    nwr(area.searchArea)["amenity"="marketplace"]["name"~"fish",i];
                """.trimIndent()
                "vegetable" in term -> """
                    nwr(area.searchArea)["shop"="greengrocer"]["name"];
                    nwr(area.searchArea)["amenity"="marketplace"]["name"~"vegetable|sabzi",i];
                """.trimIndent()
                "mandi" in term -> """
                    nwr(area.searchArea)["amenity"="marketplace"]["name"~"mandi",i];
                    nwr(area.searchArea)["name"~"mandi",i]["name"];
                """.trimIndent()
                else -> null
            }

            else -> null
        }
    }

    private fun typeFor(keyword: CrawlKeyword, tags: JSONObject): EntityType = when (keyword.category) {
        DiscoveryCategory.EMERGENCY -> when {
            tags.optString("amenity") == "fire_station" -> EntityType.EMERGENCY_SERVICE
            tags.optString("emergency") == "ambulance_station" -> EntityType.EMERGENCY_SERVICE
            else -> EntityType.EMERGENCY_SERVICE
        }
        DiscoveryCategory.LOCAL_BAZAR -> when {
            tags.optString("amenity") == "marketplace" -> EntityType.MARKET
            tags.has("shop") -> EntityType.SHOP
            else -> EntityType.MARKET
        }
        else -> when {
            tags.optString("amenity") == "place_of_worship" -> EntityType.PLACE
            tags.optString("waterway") == "dam" -> EntityType.NATURAL_FEATURE
            tags.has("natural") -> EntityType.NATURAL_FEATURE
            else -> EntityType.PLACE
        }
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

    private fun osmEntityId(element: JSONObject): String =
        "osm:${element.optString("type")}:${element.optLong("id")}"

    private fun description(tags: JSONObject): String? = sequenceOf(
        tags.optString("description"),
        tags.optString("description:en"),
        tags.optString("operator"),
    ).firstOrNull(String::isNotBlank)

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
        private const val DISCOVERY_LIMIT = 100
    }
}
