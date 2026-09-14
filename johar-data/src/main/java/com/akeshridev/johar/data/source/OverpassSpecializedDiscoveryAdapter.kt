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
 * Precise OSM discovery for Ranchi V1 concepts. Bootstrap terms are mapped to an explicit
 * OSM tag query instead of falling back to generic tourism/marketplace searches.
 *
 * This discovers static place/service metadata only. It must never be interpreted as proof
 * of current inventory, availability, price, queue length, safety or opening state.
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
        val scope = discoveryScope(keyword)
        val query = """
            [out:json][timeout:30];
            ${areaStatement(scope)}
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
                val refs = buildMap {
                    put("osm", "${element.optString("type")}:${element.optLong("id")}")
                    put("joharDiscoveryScope", scope)
                    packTypeFor(keyword, tags)?.let { put("joharPackType", it) }
                }
                add(
                    KnowledgeEntity(
                        id = osmEntityId(element),
                        name = name,
                        type = typeFor(keyword, tags),
                        description = description(tags, scope),
                        latitude = point?.first,
                        longitude = point?.second,
                        region = "Jharkhand",
                        country = "India",
                        aliases = aliases(tags),
                        externalRefs = refs,
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

    private fun discoveryScope(keyword: CrawlKeyword): String =
        if ("ranchi" in normalizeText(keyword.term)) "Ranchi" else "Jharkhand"

    private fun areaStatement(scope: String): String =
        "area[\"boundary\"=\"administrative\"][\"name\"=\"$scope\"]->.searchArea;"

    private fun selectorFor(keyword: CrawlKeyword): String? {
        val term = normalizeText(keyword.term)
        return when (keyword.category) {
            DiscoveryCategory.PLACES -> placesSelector(term)
            DiscoveryCategory.EMERGENCY -> emergencySelector(term)
            DiscoveryCategory.LOCAL_BAZAR -> localServiceSelector(term)
            else -> null
        }
    }

    private fun placesSelector(term: String): String? = when {
        "neighborhood" in term || "localit" in term ->
            "nwr(area.searchArea)[\"place\"~\"suburb|neighbourhood|quarter|locality\"][\"name\"];"
        "village" in term -> "nwr(area.searchArea)[\"place\"=\"village\"][\"name\"];"
        "waterfall" in term -> "nwr(area.searchArea)[\"natural\"=\"waterfall\"][\"name\"];"
        "picnic" in term -> "nwr(area.searchArea)[\"tourism\"=\"picnic_site\"][\"name\"];"
        "children park" in term || "playground" in term ->
            "nwr(area.searchArea)[\"leisure\"=\"playground\"][\"name\"];"
        "park" in term -> "nwr(area.searchArea)[\"leisure\"=\"park\"][\"name\"];"
        "viewpoint" in term -> "nwr(area.searchArea)[\"tourism\"=\"viewpoint\"][\"name\"];"
        "hill" in term -> "nwr(area.searchArea)[\"natural\"~\"peak|ridge|hill\"][\"name\"];"
        "lake" in term -> "nwr(area.searchArea)[\"natural\"=\"water\"][\"water\"~\"lake|reservoir\"][\"name\"];"
        "dam" in term -> "nwr(area.searchArea)[\"waterway\"=\"dam\"][\"name\"];"
        "temple" in term ->
            "nwr(area.searchArea)[\"amenity\"=\"place_of_worship\"][\"religion\"=\"hindu\"][\"name\"];"
        "church" in term ->
            "nwr(area.searchArea)[\"amenity\"=\"place_of_worship\"][\"religion\"=\"christian\"][\"name\"];"
        "mosque" in term ->
            "nwr(area.searchArea)[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"][\"name\"];"
        "gurudwara" in term || "gurdwara" in term ->
            "nwr(area.searchArea)[\"amenity\"=\"place_of_worship\"][\"religion\"=\"sikh\"][\"name\"];"
        "museum" in term -> "nwr(area.searchArea)[\"tourism\"=\"museum\"][\"name\"];"
        "heritage" in term -> "nwr(area.searchArea)[\"heritage\"][\"name\"];"
        "hotel" in term -> "nwr(area.searchArea)[\"tourism\"~\"hotel|guest_house|hostel\"][\"name\"];"
        "convention" in term -> "nwr(area.searchArea)[\"amenity\"=\"conference_centre\"][\"name\"];"
        "event venue" in term -> "nwr(area.searchArea)[\"amenity\"=\"events_venue\"][\"name\"];"
        "railway" in term || "train station" in term ->
            "nwr(area.searchArea)[\"railway\"~\"station|halt\"][\"name\"];"
        "bus terminal" in term || "bus stand" in term ->
            "nwr(area.searchArea)[\"amenity\"=\"bus_station\"][\"name\"];"
        "airport" in term -> "nwr(area.searchArea)[\"aeroway\"=\"aerodrome\"][\"name\"];"
        "parking" in term -> "nwr(area.searchArea)[\"amenity\"=\"parking\"][\"name\"];"
        "college" in term -> "nwr(area.searchArea)[\"amenity\"=\"college\"][\"name\"];"
        "universit" in term -> "nwr(area.searchArea)[\"amenity\"=\"university\"][\"name\"];"
        "librar" in term -> "nwr(area.searchArea)[\"amenity\"=\"library\"][\"name\"];"
        "stadium" in term -> "nwr(area.searchArea)[\"leisure\"=\"stadium\"][\"name\"];"
        "gym" in term -> "nwr(area.searchArea)[\"leisure\"=\"fitness_centre\"][\"name\"];"
        "cinema" in term -> "nwr(area.searchArea)[\"amenity\"=\"cinema\"][\"name\"];"
        "government office" in term -> "nwr(area.searchArea)[\"office\"=\"government\"][\"name\"];"
        "industrial" in term -> "nwr(area.searchArea)[\"landuse\"=\"industrial\"][\"name\"];"
        "business area" in term -> "nwr(area.searchArea)[\"landuse\"=\"commercial\"][\"name\"];"
        "cowork" in term -> "nwr(area.searchArea)[\"office\"=\"coworking\"][\"name\"];"
        "tourist attraction" in term || "places in" in term ->
            "nwr(area.searchArea)[\"tourism\"~\"attraction|viewpoint|museum|zoo|gallery\"][\"name\"];"
        else -> null
    }

    private fun emergencySelector(term: String): String? = when {
        "pharmac" in term -> "nwr(area.searchArea)[\"amenity\"=\"pharmacy\"][\"name\"];"
        "police" in term -> "nwr(area.searchArea)[\"amenity\"=\"police\"][\"name\"];"
        "fire" in term -> "nwr(area.searchArea)[\"amenity\"=\"fire_station\"][\"name\"];"
        "ambulance" in term -> "nwr(area.searchArea)[\"emergency\"=\"ambulance_station\"][\"name\"];"
        "clinic" in term -> "nwr(area.searchArea)[\"amenity\"=\"clinic\"][\"name\"];"
        "hospital" in term -> "nwr(area.searchArea)[\"amenity\"=\"hospital\"][\"name\"];"
        else -> null
    }

    private fun localServiceSelector(term: String): String? = when {
        "vegetarian restaurant" in term -> """
            nwr(area.searchArea)["amenity"="restaurant"]["diet:vegetarian"="yes"]["name"];
            nwr(area.searchArea)["amenity"="restaurant"]["diet:vegan"="yes"]["name"];
        """.trimIndent()
        "restaurant" in term -> "nwr(area.searchArea)[\"amenity\"=\"restaurant\"][\"name\"];"
        "cafe" in term -> "nwr(area.searchArea)[\"amenity\"=\"cafe\"][\"name\"];"
        "street food" in term -> "nwr(area.searchArea)[\"amenity\"=\"fast_food\"][\"name\"];"
        "pork" in term -> """
            nwr(area.searchArea)["shop"="butcher"]["butcher"~"pork|pig",i]["name"];
            nwr(area.searchArea)["shop"="butcher"]["name"~"pork|pig",i];
        """.trimIndent()
        "mutton" in term -> """
            nwr(area.searchArea)["shop"="butcher"]["butcher"~"mutton|goat",i]["name"];
            nwr(area.searchArea)["shop"="butcher"]["name"~"mutton|goat",i];
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
        "haat" in term || "bazar" in term || "market" in term ->
            "nwr(area.searchArea)[\"amenity\"=\"marketplace\"][\"name\"];"
        "handicraft" in term -> "nwr(area.searchArea)[\"shop\"~\"craft|gift|art\"][\"name\"];"
        "shopping mall" in term || "malls" in term -> "nwr(area.searchArea)[\"shop\"=\"mall\"][\"name\"];"
        "supermarket" in term -> "nwr(area.searchArea)[\"shop\"=\"supermarket\"][\"name\"];"
        "bank" in term || "atm" in term -> "nwr(area.searchArea)[\"amenity\"~\"bank|atm\"][\"name\"];"
        "petrol" in term || "fuel" in term -> "nwr(area.searchArea)[\"amenity\"=\"fuel\"][\"name\"];"
        "charging" in term || "ev " in "$term " ->
            "nwr(area.searchArea)[\"amenity\"=\"charging_station\"][\"name\"];"
        "toilet" in term -> "nwr(area.searchArea)[\"amenity\"=\"toilets\"][\"name\"];"
        "mobile repair" in term -> """
            nwr(area.searchArea)["shop"="mobile_phone"]["repair"="yes"]["name"];
            nwr(area.searchArea)["service:mobile_phone:repair"="yes"]["name"];
        """.trimIndent()
        "bike repair" in term -> """
            nwr(area.searchArea)["shop"="motorcycle"]["service:motorcycle:repair"="yes"]["name"];
            nwr(area.searchArea)["shop"="bicycle"]["service:bicycle:repair"="yes"]["name"];
        """.trimIndent()
        "car repair" in term -> "nwr(area.searchArea)[\"shop\"=\"car_repair\"][\"name\"];"
        "courier" in term -> "nwr(area.searchArea)[\"office\"~\"courier|logistics\"][\"name\"];"
        else -> null
    }

    private fun typeFor(keyword: CrawlKeyword, tags: JSONObject): EntityType {
        val amenity = tags.optString("amenity")
        val shop = tags.optString("shop")
        val place = tags.optString("place")
        val tourism = tags.optString("tourism")
        return when {
            amenity in setOf("hospital", "clinic") -> EntityType.HOSPITAL
            amenity == "police" -> EntityType.POLICE_STATION
            amenity == "fire_station" || tags.optString("emergency") == "ambulance_station" ->
                EntityType.EMERGENCY_SERVICE
            amenity == "marketplace" -> EntityType.MARKET
            amenity in setOf("restaurant", "cafe", "fast_food") -> EntityType.RESTAURANT
            amenity == "bus_station" -> EntityType.BUS_STAND
            amenity == "pharmacy" -> EntityType.SHOP
            amenity in setOf(
                "bank", "atm", "library", "college", "university", "place_of_worship", "fuel",
                "charging_station", "toilets", "parking", "conference_centre", "events_venue", "cinema",
            ) -> EntityType.FACILITY
            tags.optString("railway") in setOf("station", "halt") -> EntityType.RAILWAY_STATION
            tags.optString("aeroway") == "aerodrome" -> EntityType.AIRPORT
            tourism in setOf("hotel", "guest_house", "hostel") -> EntityType.FACILITY
            shop.isNotBlank() -> EntityType.SHOP
            place == "village" -> EntityType.VILLAGE
            place in setOf("suburb", "neighbourhood", "quarter", "locality") -> EntityType.PLACE
            tags.optString("natural").isNotBlank() || tags.optString("waterway") == "dam" -> EntityType.NATURAL_FEATURE
            tourism.isNotBlank() || tags.optString("leisure") in setOf("park", "playground", "stadium", "fitness_centre") ->
                EntityType.TOURIST_ATTRACTION
            tags.has("office") -> EntityType.ORGANIZATION
            else -> when (keyword.category) {
                DiscoveryCategory.EMERGENCY -> EntityType.EMERGENCY_SERVICE
                DiscoveryCategory.LOCAL_BAZAR -> EntityType.SHOP
                else -> EntityType.PLACE
            }
        }
    }

    private fun packTypeFor(keyword: CrawlKeyword, tags: JSONObject): String? {
        val term = normalizeText(keyword.term)
        return when {
            "village" in term -> "VILLAGE"
            "waterfall" in term -> "WATERFALL"
            "dam" in term -> "DAM"
            "lake" in term -> "LAKE"
            "hill" in term -> "HILL"
            "viewpoint" in term -> "VIEWPOINT"
            "children park" in term || "playground" in term || "park" in term -> "PARK"
            "temple" in term -> "TEMPLE"
            "church" in term -> "CHURCH"
            "mosque" in term -> "MOSQUE"
            "gurudwara" in term || "gurdwara" in term -> "GURUDWARA"
            "museum" in term -> "MUSEUM"
            "heritage" in term -> "HERITAGE_SITE"
            "hotel" in term -> "HOTEL"
            "convention" in term -> "CONVENTION_VENUE"
            "event venue" in term -> "EVENT_VENUE"
            "railway" in term || "train station" in term -> "RAILWAY_STATION"
            "bus terminal" in term || "bus stand" in term -> "BUS_STAND"
            "airport" in term -> "AIRPORT"
            "parking" in term -> "PARKING"
            "college" in term -> "COLLEGE"
            "universit" in term -> "UNIVERSITY"
            "librar" in term -> "LIBRARY"
            "stadium" in term -> "STADIUM"
            "gym" in term -> "GYM"
            "cinema" in term -> "CINEMA"
            "cowork" in term -> "COWORKING"
            "industrial" in term -> "INDUSTRIAL_AREA"
            "business area" in term -> "BUSINESS_AREA"
            "pharmac" in term -> "PHARMACY"
            "hospital" in term || "clinic" in term -> "HOSPITAL"
            "police" in term -> "POLICE_STATION"
            "fire" in term -> "FIRE_STATION"
            "ambulance" in term -> "AMBULANCE"
            "vegetarian restaurant" in term || "restaurant" in term -> "RESTAURANT"
            "cafe" in term -> "CAFE"
            "street food" in term -> "STREET_FOOD"
            "pork" in term || "mutton" in term -> "BUTCHER"
            "fish" in term -> "FISH_SHOP"
            "vegetable" in term -> "VEGETABLE_SHOP"
            "haat" in term || "bazar" in term || "market" in term || "mandi" in term -> "MARKET_COLLECTION"
            "handicraft" in term -> "HANDICRAFT_SHOP"
            "mall" in term -> "MALL"
            "supermarket" in term -> "SUPERMARKET"
            "bank" in term || "atm" in term -> if (tags.optString("amenity") == "atm") "ATM" else "BANK"
            "petrol" in term || "fuel" in term -> "FUEL"
            "charging" in term || "ev " in "$term " -> "EV_CHARGING"
            "toilet" in term -> "TOILET"
            "repair" in term -> "REPAIR"
            "courier" in term -> "COURIER"
            "neighborhood" in term || "localit" in term -> "LOCALITY"
            else -> null
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

    private fun description(tags: JSONObject, scope: String): String {
        val address = listOf(
            tags.optString("addr:housenumber"),
            tags.optString("addr:street"),
            tags.optString("addr:suburb"),
            tags.optString("addr:city"),
        ).filter(String::isNotBlank).joinToString(", ")
        val sourceDescription = sequenceOf(
            tags.optString("description"),
            tags.optString("description:en"),
            address,
            tags.optString("operator"),
        ).firstOrNull(String::isNotBlank)
        return listOfNotNull(sourceDescription, "Located in $scope discovery area").joinToString(". ")
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
        private const val DISCOVERY_LIMIT = 100
    }
}
