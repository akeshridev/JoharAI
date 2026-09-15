package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Conservative identity resolver for crawler/booster candidates.
 *
 * A merge needs strong identity evidence. Shared external IDs are authoritative. Exact name/alias
 * matches can merge when entity types are compatible. Fuzzier name variants require geographic
 * support so common names such as "Central School" do not collapse unrelated places.
 */
internal object CanonicalEntityResolver {
    fun resolve(
        discovered: KnowledgeEntity,
        candidates: List<KnowledgeEntityRow>,
    ): KnowledgeEntityRow? {
        val scored = candidates.mapNotNull { candidate ->
            score(discovered, candidate)?.let { candidate to it }
        }
        val best = scored.maxByOrNull { it.second } ?: return null
        val second = scored.asSequence()
            .filterNot { it.first.id == best.first.id }
            .maxOfOrNull { it.second }

        if (best.second < MATCH_THRESHOLD) return null
        if (second != null && best.second - second < MIN_WIN_MARGIN) return null
        return best.first
    }

    private fun score(
        discovered: KnowledgeEntity,
        candidate: KnowledgeEntityRow,
    ): Int? {
        if (!sameRegion(discovered, candidate)) return null

        val discoveredRefs = discovered.externalRefs.filterKeys(::isIdentityReference)
        val candidateRefs = candidate.externalRefsJson.toStringMap().filterKeys(::isIdentityReference)
        if (discoveredRefs.any { (key, value) -> candidateRefs[key] == value }) {
            return EXTERNAL_ID_SCORE
        }

        val discoveredNames = buildSet {
            add(normalizeText(discovered.name))
            discovered.aliases.mapTo(this, ::normalizeText)
        }.filter(String::isNotBlank)
        val candidateNames = buildSet {
            add(candidate.normalizedName)
            candidate.aliasesJson.toStringList().mapTo(this, ::normalizeText)
        }.filter(String::isNotBlank)

        val exactName = discoveredNames.any(candidateNames::contains)
        val nameSimilarity = discoveredNames.maxOfOrNull { left ->
            candidateNames.maxOfOrNull { right -> tokenSimilarity(left, right) } ?: 0.0
        } ?: 0.0
        val typeCompatible = typeCompatible(discovered.type.name, candidate.type)
        val distanceKm = distanceKm(
            discovered.latitude,
            discovered.longitude,
            candidate.latitude,
            candidate.longitude,
        )

        if (!typeCompatible && !exactName) return null
        if (!exactName && nameSimilarity < MIN_NAME_SIMILARITY) return null
        if (!exactName && distanceKm == null) return null
        if (distanceKm != null && distanceKm > MAX_MERGE_DISTANCE_KM) return null

        var score = 0
        score += when {
            exactName -> 55
            nameSimilarity >= 0.80 -> 35
            nameSimilarity >= 0.60 -> 25
            else -> 15
        }
        if (typeCompatible) score += 20
        score += when {
            distanceKm == null -> 0
            distanceKm <= 0.25 -> 35
            distanceKm <= 1.0 -> 30
            distanceKm <= 3.0 -> 20
            else -> 0
        }
        if (normalizeText(discovered.region.orEmpty()) == normalizeText(candidate.region.orEmpty())) score += 5
        return score
    }

    private fun sameRegion(discovered: KnowledgeEntity, candidate: KnowledgeEntityRow): Boolean {
        val left = normalizeText(discovered.region.orEmpty())
        val right = normalizeText(candidate.region.orEmpty())
        return left.isBlank() || right.isBlank() || left == right
    }

    private fun typeCompatible(left: String, right: String): Boolean {
        if (left == right) return true
        val broad = setOf("PLACE", "TOURIST_ATTRACTION", "NATURAL_FEATURE", "FACILITY", "ORGANIZATION", "OTHER")
        return left in broad && right in broad
    }

    private fun tokenSimilarity(left: String, right: String): Double {
        val leftTokens = identityTokens(left)
        val rightTokens = identityTokens(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val union = leftTokens.union(rightTokens).size.toDouble()
        return intersection / union
    }

    private fun identityTokens(value: String): Set<String> = normalizeText(value)
        .split(' ')
        .map(String::trim)
        .filter { it.isNotBlank() && it !in GENERIC_IDENTITY_TOKENS }
        .map { TOKEN_ALIASES[it] ?: it }
        .toSet()

    private fun isIdentityReference(key: String): Boolean =
        key !in NON_IDENTITY_REFERENCE_KEYS

    private fun distanceKm(
        lat1: Double?,
        lon1: Double?,
        lat2: Double?,
        lon2: Double?,
    ): Double? {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private fun String.toStringList(): List<String> = runCatching {
        val json = JSONArray(this)
        buildList {
            for (index in 0 until json.length()) {
                json.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun String.toStringMap(): Map<String, String> = runCatching {
        val json = JSONObject(this)
        buildMap {
            json.keys().forEach { key ->
                json.optString(key).takeIf(String::isNotBlank)?.let { put(key, it) }
            }
        }
    }.getOrDefault(emptyMap())

    private const val MATCH_THRESHOLD = 75
    private const val MIN_WIN_MARGIN = 10
    private const val EXTERNAL_ID_SCORE = 200
    private const val MIN_NAME_SIMILARITY = 0.34
    private const val MAX_MERGE_DISTANCE_KM = 5.0
    private const val EARTH_RADIUS_KM = 6371.0

    private val NON_IDENTITY_REFERENCE_KEYS = setOf(
        "joharDiscoveryScope",
        "joharPackType",
    )
    private val GENERIC_IDENTITY_TOKENS = setOf(
        "railway", "station", "junction", "jn", "airport", "hospital", "college", "university",
        "school", "market", "bazar", "bazaar", "park", "temple", "the",
    )
    private val TOKEN_ALIASES = mapOf(
        "rd" to "road",
        "st" to "street",
    )
}
