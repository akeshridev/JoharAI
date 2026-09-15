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
        dao.searchEntities(normalized, limit = maxOf(limit * 5, 20)).forEach { candidates[it.id] = it }

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

    /** Category discovery must not depend on a category word occurring in the place name. */
    fun discoverPlaces(types: Set<String>, limit: Int = 20): List<RanchiSpatialPlace> =
        dao.allEnabledEntities().asSequence()
            .filter(::isRanchiScoped)
            .mapNotNull(::toSpatialPlace)
            .filter { it.type in types }
            .sortedBy { it.name }
            .take(limit)
            .toList()

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

    private fun toSpatialPlace(entity: KnowledgeEntityRow): RanchiSpatialPlace? {
        val latitude = entity.latitude ?: return null
        val longitude = entity.longitude ?: return null
        return RanchiSpatialPlace(
            id = entity.id,
            name = entity.name,
            type = runCatching { JSONObject(entity.externalRefsJson).optString("joharPackType") }
                .getOrNull()?.takeIf(String::isNotBlank) ?: entity.type,
            coordinate = RanchiCoordinate(latitude, longitude),
            region = entity.region,
            description = entity.description,
        )
    }

    private fun containsAny(text: String, vararg values: String): Boolean = values.any(text::contains)

    private companion object {
        const val RANCHI = "Ranchi"
        val SEARCH_STOP_WORDS = setOf(
            "ranchi", "mein", "me", "ke", "ka", "ki", "ko", "se", "paas", "near", "kahan", "hai",
        )
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
