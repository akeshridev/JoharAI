package com.akeshridev.johar.data.spatial

import android.content.Context
import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.KnowledgeDao
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline spatial lookup over Johar's existing Room entities.
 *
 * Ranchi V1 is intentionally hard-scoped by provenance, not by a hand-drawn bounding box.
 * Only entities explicitly discovered for Ranchi (or sourced from District Ranchi) participate.
 * The exact district polygon is generated separately for the offline map pack.
 */
class RanchiSpatialEngine internal constructor(private val dao: KnowledgeDao) {
    constructor(context: Context) : this(JoharDatabaseProvider.get(context.applicationContext).knowledgeDao())

    fun resolvePlace(query: String, limit: Int = 5): List<RanchiSpatialPlace> {
        val normalized = normalizeText(query)
        if (normalized.isBlank()) return emptyList()

        val candidates = linkedMapOf<String, KnowledgeEntityRow>()
        searchInto(normalized, limit, candidates)

        COMMON_RANCHI_ALIASES[normalized].orEmpty().forEach { alias ->
            searchInto(alias, limit, candidates)
        }

        if (candidates.size < limit) {
            normalized
                .split(' ')
                .asSequence()
                .filter { it.length >= 3 && it !in SEARCH_STOP_WORDS }
                .forEach { token ->
                    dao.searchEntities(token, limit = 20).forEach { candidates.putIfAbsent(it.id, it) }
                }
        }

        return candidates.values
            .asSequence()
            .filter(::isRanchiScoped)
            .mapNotNull { entity -> toSpatialPlace(entity)?.let { it to relevanceScore(normalized, entity) } }
            .sortedByDescending { (_, score) -> score }
            .map { (place, _) -> place }
            .take(limit)
            .toList()
    }

    private fun searchInto(
        normalized: String,
        limit: Int,
        candidates: MutableMap<String, KnowledgeEntityRow>,
    ) {
        dao.searchEntities(normalized, limit = maxOf(limit * 5, 20))
            .forEach { candidates.putIfAbsent(it.id, it) }
    }

    /**
     * Category discovery must not depend on a category word occurring in the place name.
     *
     * Discovery order is intentionally quality-ranked rather than alphabetical. Raw OSM/imported
     * datasets can contain technically valid but demo-hostile labels such as "89" or tiny generic
     * locality names. Those should never outrank a recognizable named hill, falls, park or museum.
     *
     * Some upstream sources classify named waterfalls as TOURIST_ATTRACTION rather than WATERFALL.
     * Treat those as waterfall candidates when their display name explicitly says falls/waterfall.
     */
    fun discoverPlaces(types: Set<String>, limit: Int = 20): List<RanchiSpatialPlace> {
        val normalizedTypes = types.map { it.uppercase() }.toSet()
        return dao.allEnabledEntities().asSequence()
            .filter(::isRanchiScoped)
            .filter { hasUsableDisplayName(it.name) }
            .mapNotNull { entity -> toSpatialPlace(entity)?.let { it to discoveryQualityScore(entity, it) } }
            .filter { (place, _) -> matchesDiscoveryTypes(place, normalizedTypes) }
            .sortedWith(
                compareByDescending<Pair<RanchiSpatialPlace, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() },
            )
            .map { (place, _) -> place }
            .take(limit)
            .toList()
    }

    fun nearby(
        origin: RanchiCoordinate,
        radiusKm: Double = 5.0,
        types: Set<String> = emptySet(),
        limit: Int = 10,
    ): List<RanchiSpatialPlace> {
        val normalizedTypes = types.map { it.uppercase() }.toSet()
        return dao.allEnabledEntities()
            .asSequence()
            .filter(::isRanchiScoped)
            .mapNotNull(::toSpatialPlace)
            .filter { place -> normalizedTypes.isEmpty() || place.type.uppercase() in normalizedTypes }
            .map { place -> place.copy(distanceKm = distanceKm(origin, place.coordinate)) }
            .filter { place -> (place.distanceKm ?: Double.MAX_VALUE) <= radiusKm }
            .sortedBy { it.distanceKm }
            .take(limit)
            .toList()
    }

    fun allRanchiPlaces(limit: Int = 500): List<RanchiSpatialPlace> =
        dao.allEnabledEntities()
            .asSequence()
            .filter(::isRanchiScoped)
            .mapNotNull(::toSpatialPlace)
            .take(limit)
            .toList()

    fun distanceKm(from: RanchiCoordinate, to: RanchiCoordinate): Double {
        val earthRadiusKm = 6371.0088
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun isRanchiScoped(entity: KnowledgeEntityRow): Boolean {
        if (entity.region.equals(RANCHI, ignoreCase = true)) return true

        val refs = entity.externalRefsJson.lowercase()
        return refs.contains("\"johardiscoveryscope\":\"ranchi\"") ||
            refs.contains("ranchi.nic.in")
    }

    private fun relevanceScore(query: String, entity: KnowledgeEntityRow): Int {
        val name = entity.normalizedName
        val tokens = query.split(' ').filter(String::isNotBlank)
        var score = when {
            name == query -> 500
            name.contains(query) || query.contains(name) -> 250
            else -> 0
        }
        score += tokens.count(name::contains) * 20

        val type = entity.type.uppercase()
        if (containsAny(query, "railway", "station", "junction") && containsAny(type, "RAIL", "STATION")) score += 120
        if (containsAny(query, "mandir", "temple") && containsAny(type, "TEMPLE", "PILGRIMAGE")) score += 120
        if (containsAny(query, "hospital", "clinic") && containsAny(type, "HOSPITAL", "HEALTH")) score += 120
        if (containsAny(query, "park", "garden") && containsAny(type, "PARK", "GARDEN")) score += 120
        if (containsAny(query, "airport") && containsAny(type, "AIRPORT")) score += 120
        return score
    }

    private fun discoveryQualityScore(entity: KnowledgeEntityRow, place: RanchiSpatialPlace): Int {
        val name = place.name.lowercase()
        val type = place.type.uppercase()
        var score = when (type) {
            "WATERFALL" -> 120
            "HILL", "VIEWPOINT" -> 115
            "MUSEUM", "HERITAGE_SITE" -> 110
            "TEMPLE", "PILGRIMAGE" -> 105
            "GARDEN" -> 100
            "PARK" -> 95
            "TOURIST_ATTRACTION" -> 90
            else -> 70
        }

        if (!place.description.isNullOrBlank()) score += 35
        if (name.split(Regex("\\s+")).size >= 2) score += 12
        if (RECOGNIZABLE_PLACE_WORDS.any(name::contains)) score += 25
        if (LOW_VALUE_PLACE_WORDS.any(name::contains)) score -= 35
        if (name.length < 4) score -= 50
        return score
    }

    private fun matchesDiscoveryTypes(place: RanchiSpatialPlace, requestedTypes: Set<String>): Boolean {
        val type = place.type.uppercase()
        if (type in requestedTypes) return true

        // Generic waterfall queries should still find records sourced as tourist attractions.
        if ("WATERFALL" in requestedTypes && type == "TOURIST_ATTRACTION") {
            val normalizedName = normalizeText(place.name)
            return WATERFALL_NAME_WORDS.any(normalizedName::contains)
        }
        return false
    }

    private fun hasUsableDisplayName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length < 3) return false
        if (trimmed.all(Char::isDigit)) return false
        return trimmed.any(Char::isLetter)
    }

    private fun toSpatialPlace(entity: KnowledgeEntityRow): RanchiSpatialPlace? {
        val latitude = entity.latitude ?: return null
        val longitude = entity.longitude ?: return null
        val resolvedType = runCatching { JSONObject(entity.externalRefsJson).optString("joharPackType") }
            .getOrNull()?.takeIf(String::isNotBlank) ?: entity.type
        return RanchiSpatialPlace(
            id = entity.id,
            name = entity.name,
            type = resolvedType,
            coordinate = RanchiCoordinate(latitude, longitude),
            region = entity.region,
            description = safeDescription(entity.name, resolvedType, entity.description),
        )
    }

    /**
     * Defensive presentation guard for imported/canonicalized place descriptions.
     *
     * For named waterfalls, a description that talks about another named falls without mentioning
     * the card's own distinguishing name is worse than showing no description at all. This prevents
     * cases such as a Jonha card rendering a Hundru paragraph while the underlying data is repaired.
     */
    private fun safeDescription(name: String, type: String, description: String?): String? {
        val text = description?.trim()?.takeIf(String::isNotBlank) ?: return null
        val normalizedType = type.uppercase()
        val normalizedName = normalizeText(name)
        val normalizedDescription = normalizeText(text)
        val isWaterfall = normalizedType in setOf("WATERFALL", "NATURAL_FEATURE", "TOURIST_ATTRACTION") &&
            WATERFALL_NAME_WORDS.any(normalizedName::contains)
        if (!isWaterfall) return text

        val identityTokens = normalizedName
            .split(' ')
            .filter { token -> token.length >= 4 && token !in GENERIC_WATERFALL_WORDS }
        if (identityTokens.isEmpty()) return text

        val mentionsOwnIdentity = identityTokens.any(normalizedDescription::contains)
        val namesAnotherFalls = WATERFALL_NAME_WORDS.any(normalizedDescription::contains) && !mentionsOwnIdentity
        return if (namesAnotherFalls) null else text
    }

    private fun containsAny(text: String, vararg values: String): Boolean = values.any(text::contains)

    private companion object {
        const val RANCHI = "Ranchi"
        val SEARCH_STOP_WORDS = setOf(
            "ranchi", "mein", "me", "ke", "ka", "ki", "ko", "se", "paas", "near", "kahan", "hai",
        )
        val COMMON_RANCHI_ALIASES = mapOf(
            "main road" to listOf("mahatma gandhi main road", "mahatma gandhi road", "mg road"),
            "ranchi main road" to listOf("mahatma gandhi main road", "mahatma gandhi road", "mg road"),
        )
        val RECOGNIZABLE_PLACE_WORDS = setOf(
            "hill", "falls", "waterfall", "dam", "lake", "garden", "park", "mandir", "temple", "museum", "rock",
        )
        val LOW_VALUE_PLACE_WORDS = setOf(
            "colony ground", "play ground", "playground", "community ground", "sector ground",
        )
        val WATERFALL_NAME_WORDS = setOf("falls", "fall", "waterfall")
        val GENERIC_WATERFALL_WORDS = WATERFALL_NAME_WORDS + setOf("ranchi", "jharkhand")
    }
}

data class RanchiSpatialPlace(
    val id: String,
    val name: String,
    val type: String,
    val coordinate: RanchiCoordinate,
    val region: String?,
    val description: String?,
    val distanceKm: Double? = null,
)

data class RanchiCoordinate(
    val latitude: Double,
    val longitude: Double,
)
