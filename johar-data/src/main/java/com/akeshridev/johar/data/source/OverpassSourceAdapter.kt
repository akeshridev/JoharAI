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

internal class OverpassSourceAdapter(
    private val fetcher: HttpTextFetcher,
    private val endpoint: String = DEFAULT_ENDPOINT,
) : CrawlSourceAdapter, KeywordDiscoveryAdapter {
    override val id: String = "openstreetmap"

    override fun supports(seed: CrawlSeed): Boolean = seed.entityType != EntityType.REGION

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val ownerId = requireNotNull(seed.entityId)
        val resolutionQuery = resolutionQuery(seed)
        val resolutionRaw = fetch(resolutionQuery)
        val resolutionElements = JSONObject(resolutionRaw).optJSONArray("elements") ?: JSONArray()
        val resolved = bestEntityElement(seed, resolutionElements)

        val resolvedCoordinates = resolved?.let(::coordinates)
        val latitude = resolvedCoordinates?.first ?: seed.latitude
        val longitude = resolvedCoordinates?.second ?: seed.longitude
        val resolvedType = resolved?.let(::entityType) ?: seed.entityType ?: EntityType.OTHER
        val rootSourceUrl = resolved?.let(::canonicalUrl) ?: snapshotUrl(resolutionQuery)
        val retrievedAt = System.currentTimeMillis()

        val rootTags = resolved?.optJSONObject("tags") ?: JSONObject()
        val rootName = rootTags.optString("name").takeIf(String::isNotBlank) ?: seed.name
        val rootEntity = KnowledgeEntity(
            id = ownerId,
            name = rootName,
            type = resolvedType,
            description = osmDescription(rootTags),
            latitude = latitude,
            longitude = longitude,
            region = seed.region,
            country = seed.country,
            aliases = aliases(rootTags),
            externalRefs = seed.externalRefs + resolvedRef(resolved),
        )
        val facts = mutableListOf<SourceFact>()
        resolved?.let { element ->
            facts += factsFromElement(ownerId, element, rootSourceUrl, retrievedAt)
        }

        val discovered = mutableListOf<KnowledgeEntity>()
        val relationships = mutableListOf<EntityRelationship>()
        val discoveredFacts = mutableListOf<SourceFact>()
        val media = mutableListOf<MediaAsset>()
        var nearbyRaw = ""

        if (latitude != null && longitude != null) {
            val nearbyQuery = nearbyQuery(latitude, longitude)
            nearbyRaw = fetch(nearbyQuery)
            val nearbyElements = JSONObject(nearbyRaw).optJSONArray("elements") ?: JSONArray()
            for (index in 0 until nearbyElements.length()) {
                val element = nearbyElements.optJSONObject(index) ?: continue
                val tags = element.optJSONObject("tags") ?: continue
                val name = tags.optString("name").takeIf(String::isNotBlank) ?: continue
                val type = entityType(element)
                val targetId = osmEntityId(element)
                if (targetId == ownerId) continue
                val point = coordinates(element)
                val sourceUrl = canonicalUrl(element)

                discovered += KnowledgeEntity(
                    id = targetId,
                    name = name,
                    type = type,
                    description = osmDescription(tags),
                    latitude = point?.first,
                    longitude = point?.second,
                    region = seed.region,
                    country = seed.country,
                    aliases = aliases(tags),
                    externalRefs = resolvedRef(element),
                )
                discoveredFacts += factsFromElement(targetId, element, sourceUrl, retrievedAt)
                relationships += EntityRelationship(
                    id = stableId("rel", ownerId, targetId, "osm.nearby", sourceUrl),
                    fromEntityId = ownerId,
                    toEntityId = targetId,
                    predicate = "osm.nearby",
                    sourceUrl = sourceUrl,
                    publisher = PUBLISHER,
                    retrievedAtEpochMillis = retrievedAt,
                    evidenceText = "OpenStreetMap element returned within ${NEARBY_RADIUS_METERS}m of $rootName",
                )
                media += mediaFromTags(targetId, sourceUrl, tags)
            }
        }

        val raw = JSONObject()
            .put("resolution", JSONObject(resolutionRaw))
            .apply {
                if (nearbyRaw.isNotBlank()) put("nearby", JSONObject(nearbyRaw))
            }
            .toString()

        return SourceResult(
            sourceUrl = snapshotUrl(resolutionQuery),
            publisher = PUBLISHER,
            rawContent = raw,
            entity = rootEntity,
            discoveredEntities = discovered.distinctBy { it.id },
            facts = facts + discoveredFacts,
            relationships = relationships,
            media = media,
        )
    }

    override fun supports(keyword: CrawlKeyword): Boolean =
        keyword.discoveredFrom == "bootstrap" && keyword.category in setOf(
            DiscoveryCategory.PLACES,
            DiscoveryCategory.EMERGENCY,
            DiscoveryCategory.LOCAL_BAZAR,
        )

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val query = discoveryQuery(keyword)
        val raw = fetch(query)
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
                        type = entityType(element),
                        description = osmDescription(tags),
                        latitude = point?.first,
                        longitude = point?.second,
                        region = "Jharkhand",
                        country = "India",
                        aliases = aliases(tags),
                        externalRefs = resolvedRef(element),
                    ),
                )
            }
        }
        return DiscoveryResult(
            sourceUrl = snapshotUrl(query),
            publisher = PUBLISHER,
            rawContent = raw,
            entities = entities.distinctBy { it.id },
        )
    }

    private fun resolutionQuery(seed: CrawlSeed): String {
        val osmRef = seed.externalRefs[REF_OSM]
        if (!osmRef.isNullOrBlank()) {
            val parts = osmRef.split(':', limit = 2)
            if (parts.size == 2 && parts[1].toLongOrNull() != null) {
                val type = parts[0].takeIf { it in setOf("node", "way", "relation") }
                if (type != null) {
                    return "[out:json][timeout:20];$type(id:${parts[1]});out center tags;"
                }
            }
        }

        val escapedName = regexEscape(seed.name)
        val escapedRegion = stringEscape(seed.region)
        return """
            [out:json][timeout:25];
            area["boundary"="administrative"]["name"="$escapedRegion"]->.searchArea;
            nwr(area.searchArea)["name"~"^$escapedName$",i];
            out center tags $RESOLUTION_LIMIT;
        """.trimIndent()
    }

    private fun nearbyQuery(latitude: Double, longitude: Double): String = """
        [out:json][timeout:25];
        (
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["amenity"~"hospital|clinic|police|marketplace|bus_station|toilets|parking|restaurant|fast_food"]["name"];
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["shop"]["name"];
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["tourism"]["name"];
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["place"~"village|town|city"]["name"];
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["natural"]["name"];
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["railway"="station"]["name"];
          nwr(around:$NEARBY_RADIUS_METERS,$latitude,$longitude)["aeroway"="aerodrome"]["name"];
        );
        out center tags $NEARBY_LIMIT;
    """.trimIndent()

    private fun discoveryQuery(keyword: CrawlKeyword): String {
        val term = normalizeText(keyword.term)
        val selector = when (keyword.category) {
            DiscoveryCategory.EMERGENCY -> when {
                "police" in term -> "nwr(area.searchArea)[\"amenity\"=\"police\"][\"name\"]"
                else -> "nwr(area.searchArea)[\"amenity\"~\"hospital|clinic\"][\"name\"]"
            }
            DiscoveryCategory.LOCAL_BAZAR -> when {
                "butcher" in term || "meat" in term -> "nwr(area.searchArea)[\"shop\"=\"butcher\"][\"name\"]"
                else -> "nwr(area.searchArea)[\"amenity\"=\"marketplace\"][\"name\"]"
            }
            else -> when {
                "waterfall" in term -> "nwr(area.searchArea)[\"natural\"=\"waterfall\"][\"name\"]"
                "village" in term -> "nwr(area.searchArea)[\"place\"=\"village\"][\"name\"]"
                "river" in term -> "nwr(area.searchArea)[\"waterway\"=\"river\"][\"name\"]"
                else -> "nwr(area.searchArea)[\"tourism\"][\"name\"]"
            }
        }
        return """
            [out:json][timeout:30];
            area["boundary"="administrative"]["name"="Jharkhand"]->.searchArea;
            $selector;
            out center tags $DISCOVERY_LIMIT;
        """.trimIndent()
    }

    private fun fetch(query: String): String = fetcher.postForm(
        url = endpoint,
        form = mapOf("data" to query),
    )

    private fun bestEntityElement(seed: CrawlSeed, elements: JSONArray): JSONObject? {
        val normalizedSeed = normalizeText(seed.name)
        var best: JSONObject? = null
        var score = Int.MIN_VALUE
        for (index in 0 until elements.length()) {
            val element = elements.optJSONObject(index) ?: continue
            val tags = element.optJSONObject("tags") ?: continue
            val name = tags.optString("name")
            var candidateScore = 0
            if (normalizeText(name) == normalizedSeed) candidateScore += 100
            if (entityType(element) == seed.entityType) candidateScore += 20
            if (coordinates(element) != null) candidateScore += 5
            if (candidateScore > score) {
                score = candidateScore
                best = element
            }
        }
        return best
    }

    private fun factsFromElement(
        entityId: String,
        element: JSONObject,
        sourceUrl: String,
        retrievedAt: Long,
    ): List<SourceFact> {
        val tags = element.optJSONObject("tags") ?: return emptyList()
        val facts = mutableListOf<SourceFact>()
        val keys = tags.keys()
        while (keys.hasNext() && facts.size < MAX_TAG_FACTS) {
            val key = keys.next()
            if (key in IGNORED_TAGS) continue
            val value = tags.optString(key)
            if (value.isBlank()) continue
            facts += SourceFact(
                entityId = entityId,
                sourceUrl = sourceUrl,
                publisher = PUBLISHER,
                retrievedAtEpochMillis = retrievedAt,
                domain = domainForTag(key, tags),
                field = "osm.$key",
                value = when (value.lowercase()) {
                    "yes" -> FactValue.BooleanValue(true)
                    "no" -> FactValue.BooleanValue(false)
                    else -> FactValue.Text(value)
                },
                evidenceText = "OSM tag $key=$value",
                freshness = freshnessForTag(key),
            )
        }

        coordinates(element)?.let { (lat, lon) ->
            facts += SourceFact(
                entityId = entityId,
                sourceUrl = sourceUrl,
                publisher = PUBLISHER,
                retrievedAtEpochMillis = retrievedAt,
                domain = KnowledgeDomain.GEOGRAPHY,
                field = "latitude",
                value = FactValue.Number(lat, "degrees"),
                evidenceText = "OpenStreetMap geometry",
                freshness = Freshness.EVERGREEN,
            )
            facts += SourceFact(
                entityId = entityId,
                sourceUrl = sourceUrl,
                publisher = PUBLISHER,
                retrievedAtEpochMillis = retrievedAt,
                domain = KnowledgeDomain.GEOGRAPHY,
                field = "longitude",
                value = FactValue.Number(lon, "degrees"),
                evidenceText = "OpenStreetMap geometry",
                freshness = Freshness.EVERGREEN,
            )
        }
        return facts
    }

    private fun mediaFromTags(
        entityId: String,
        sourceUrl: String,
        tags: JSONObject,
    ): List<MediaAsset> {
        val image = tags.optString("image").takeIf { it.startsWith("http") } ?: return emptyList()
        return listOf(
            MediaAsset(
                id = stableId("media", entityId, sourceUrl, image),
                entityId = entityId,
                type = MediaType.IMAGE,
                sourceUrl = sourceUrl,
                mediaUrl = image,
            ),
        )
    }

    private fun entityType(element: JSONObject): EntityType {
        val tags = element.optJSONObject("tags") ?: return EntityType.OTHER
        val amenity = tags.optString("amenity")
        val place = tags.optString("place")
        val natural = tags.optString("natural")
        return when {
            amenity in setOf("hospital", "clinic") -> EntityType.HOSPITAL
            amenity == "police" -> EntityType.POLICE_STATION
            amenity == "marketplace" -> EntityType.MARKET
            amenity in setOf("restaurant", "fast_food", "cafe") -> EntityType.RESTAURANT
            amenity == "bus_station" -> EntityType.BUS_STAND
            amenity in setOf("toilets", "parking") -> EntityType.FACILITY
            tags.has("shop") -> EntityType.SHOP
            tags.optString("railway") == "station" -> EntityType.RAILWAY_STATION
            tags.optString("aeroway") == "aerodrome" -> EntityType.AIRPORT
            place == "village" -> EntityType.VILLAGE
            place == "town" -> EntityType.TOWN
            place == "city" -> EntityType.CITY
            tags.has("tourism") -> EntityType.TOURIST_ATTRACTION
            natural == "waterfall" -> EntityType.TOURIST_ATTRACTION
            tags.optString("waterway") == "river" -> EntityType.RIVER
            natural.isNotBlank() -> EntityType.NATURAL_FEATURE
            else -> EntityType.PLACE
        }
    }

    private fun domainForTag(key: String, tags: JSONObject): KnowledgeDomain = when {
        key == "cuisine" || key.startsWith("diet:") -> KnowledgeDomain.FOOD
        key in setOf("wheelchair", "step_count", "ramp", "elevator") -> KnowledgeDomain.FAMILY_ACCESSIBILITY
        key in setOf("opening_hours", "fee", "toilets", "parking", "drinking_water", "internet_access") ->
            KnowledgeDomain.FACILITIES
        key in setOf("phone", "emergency", "healthcare") || tags.optString("amenity") == "police" ->
            KnowledgeDomain.SAFETY_EMERGENCY
        key.startsWith("addr:") || key in setOf("place", "natural", "waterway", "ele") -> KnowledgeDomain.GEOGRAPHY
        key in setOf("website", "tourism", "attraction", "description") -> KnowledgeDomain.TOURISM
        else -> KnowledgeDomain.TRAVEL_LOGISTICS
    }

    private fun freshnessForTag(key: String): Freshness = when (key) {
        "opening_hours", "phone", "website" -> Freshness.SEASONAL
        else -> Freshness.EVERGREEN
    }

    private fun coordinates(element: JSONObject): Pair<Double, Double>? {
        if (element.has("lat") && element.has("lon")) {
            return element.optDouble("lat") to element.optDouble("lon")
        }
        val center = element.optJSONObject("center") ?: return null
        if (!center.has("lat") || !center.has("lon")) return null
        return center.optDouble("lat") to center.optDouble("lon")
    }

    private fun osmEntityId(element: JSONObject): String =
        "osm:${element.optString("type")}:${element.optLong("id")}"

    private fun canonicalUrl(element: JSONObject): String =
        "https://www.openstreetmap.org/${element.optString("type")}/${element.optLong("id")}"

    private fun resolvedRef(element: JSONObject?): Map<String, String> {
        if (element == null) return emptyMap()
        val type = element.optString("type")
        val id = element.optLong("id")
        if (type.isBlank() || id == 0L) return emptyMap()
        return mapOf(REF_OSM to "$type:$id")
    }

    private fun aliases(tags: JSONObject): List<String> = buildList {
        listOf("alt_name", "loc_name", "official_name", "short_name", "name:hi").forEach { key ->
            tags.optString(key)
                .split(';')
                .map(String::trim)
                .filter(String::isNotBlank)
                .forEach(::add)
        }
    }.distinctBy(::normalizeText)

    private fun osmDescription(tags: JSONObject): String? = tags.optString("description")
        .takeIf(String::isNotBlank)
        ?: listOf("tourism", "amenity", "shop", "natural", "place", "waterway")
            .firstNotNullOfOrNull { key ->
                tags.optString(key).takeIf(String::isNotBlank)?.let { "$key=$it" }
            }

    private fun snapshotUrl(query: String): String =
        "$endpoint#${stableId("query", query)}"

    private fun regexEscape(value: String): String = Regex.escape(value)
        .removePrefix("\\Q")
        .removeSuffix("\\E")
        .replace("\\E\\Q", "")
        .replace("\"", "\\\"")

    private fun stringEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    companion object {
        private const val PUBLISHER = "OpenStreetMap contributors"
        private const val DEFAULT_ENDPOINT = "https://overpass-api.de/api/interpreter"
        private const val REF_OSM = "osm"
        private const val NEARBY_RADIUS_METERS = 8_000
        private const val RESOLUTION_LIMIT = 8
        private const val NEARBY_LIMIT = 160
        private const val DISCOVERY_LIMIT = 250
        private const val MAX_TAG_FACTS = 60

        private val IGNORED_TAGS = setOf(
            "name",
            "name:en",
            "name:hi",
            "alt_name",
            "loc_name",
            "official_name",
            "short_name",
            "source",
            "source:date",
            "created_by",
        )
    }
}
