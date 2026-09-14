package com.akeshridev.johar.data.spatial

import android.content.Context
import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline spatial lookup over Johar's existing Room entities.
 *
 * Important: this engine is intentionally scoped to Ranchi district only. Any entity whose
 * coordinate falls outside [RanchiBoundary] is ignored even if it exists in the shared database.
 */
class RanchiSpatialEngine(context: Context) {
    private val dao = JoharDatabaseProvider.get(context.applicationContext).knowledgeDao()

    fun resolvePlace(query: String, limit: Int = 5): List<RanchiSpatialPlace> {
        val normalized = normalizeText(query)
        if (normalized.isBlank()) return emptyList()

        return dao.searchEntities(normalized, limit = maxOf(limit * 5, 20))
            .asSequence()
            .mapNotNull(::toRanchiPlace)
            .take(limit)
            .toList()
    }

    fun nearby(
        origin: RanchiCoordinate,
        radiusKm: Double = 5.0,
        types: Set<String> = emptySet(),
        limit: Int = 10,
    ): List<RanchiSpatialPlace> {
        if (!RanchiBoundary.contains(origin)) return emptyList()

        val normalizedTypes = types.map { it.uppercase() }.toSet()
        return dao.allEnabledEntities()
            .asSequence()
            .mapNotNull(::toRanchiPlace)
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
            .mapNotNull(::toRanchiPlace)
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

    private fun toRanchiPlace(entity: KnowledgeEntityRow): RanchiSpatialPlace? {
        val latitude = entity.latitude ?: return null
        val longitude = entity.longitude ?: return null
        val coordinate = RanchiCoordinate(latitude, longitude)
        if (!RanchiBoundary.contains(coordinate)) return null

        return RanchiSpatialPlace(
            id = entity.id,
            name = entity.name,
            type = entity.type,
            coordinate = coordinate,
            region = entity.region,
            description = entity.description,
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

/**
 * Simplified Ranchi district polygon used as a hard geofence for V1 spatial features.
 * Coordinates are [longitude, latitude] converted to [RanchiCoordinate].
 * Replace with a higher-resolution official/open boundary during the map-pack build step.
 */
object RanchiBoundary {
    private val polygon = listOf(
        RanchiCoordinate(23.430090336051986, 85.8670606977255),
        RanchiCoordinate(23.1943283976327, 85.84112360114803),
        RanchiCoordinate(22.996071877584942, 86.24020965780804),
        RanchiCoordinate(22.83784918087175, 86.16147955019308),
        RanchiCoordinate(22.837106325391293, 86.16097795117446),
        RanchiCoordinate(22.868670059545337, 85.90905744516185),
        RanchiCoordinate(22.868707125663594, 85.9081041904089),
        RanchiCoordinate(23.155920537195655, 85.75652950894481),
        RanchiCoordinate(23.28552211187081, 85.15653840194658),
        RanchiCoordinate(23.285124876712086, 85.1562554391505),
        RanchiCoordinate(23.458569580117096, 85.18042738952606),
        RanchiCoordinate(23.551778055750702, 85.05229205226979),
        RanchiCoordinate(23.642745155919503, 85.05827780566528),
        RanchiCoordinate(23.571319002690867, 84.97124386980819),
        RanchiCoordinate(23.580668299024087, 84.96061202872161),
        RanchiCoordinate(23.597924897580725, 84.94905900117112),
        RanchiCoordinate(23.5980433553132, 84.94539974704877),
        RanchiCoordinate(23.5992069000462, 84.94422281519118),
        RanchiCoordinate(23.59972270894508, 84.94370106198643),
        RanchiCoordinate(23.600541822649234, 84.94403226358595),
        RanchiCoordinate(23.71351010809952, 85.05375392933693),
    )

    fun contains(point: RanchiCoordinate): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val yi = polygon[i].latitude
            val xi = polygon[i].longitude
            val yj = polygon[j].latitude
            val xj = polygon[j].longitude
            val intersects = ((yi > point.latitude) != (yj > point.latitude)) &&
                (point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
}
