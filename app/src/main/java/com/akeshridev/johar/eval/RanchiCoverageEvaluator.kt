package com.akeshridev.johar.eval

import android.content.Context
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeHit
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import org.json.JSONObject

class RanchiCoverageEvaluator(
    context: Context,
    private val retriever: OfflineKnowledgeRetriever,
) {
    private val appContext = context.applicationContext

    fun run(assetName: String = "ranchi-coverage-eval.jsonl"): RanchiCoverageReport {
        val results = loadCases(assetName).map(::evaluate)
        return RanchiCoverageReport(
            total = results.size,
            pass = results.count { it.status == RanchiCoverageStatus.PASS },
            partial = results.count { it.status == RanchiCoverageStatus.PARTIAL },
            gap = results.count { it.status == RanchiCoverageStatus.GAP },
            gapCounts = results.flatMap(RanchiCoverageResult::gaps).groupingBy { it }.eachCount().toSortedMap(),
            categoryCounts = summarize(results) { it.case.category },
            personaCounts = summarize(results) { it.case.persona },
            results = results,
        )
    }

    private fun evaluate(case: RanchiCoverageCase): RanchiCoverageResult {
        val effectiveQuery = buildEffectiveQuery(case)
        val hits = retriever.retrieve(effectiveQuery, limit = 5)

        if (case.expectNoAnswer) {
            val gaps = if (hits.isEmpty()) emptyList() else listOf(GAP_OUT_OF_DOMAIN_LEAK)
            return RanchiCoverageResult(
                case = case,
                effectiveQuery = effectiveQuery,
                status = if (hits.isEmpty()) RanchiCoverageStatus.PASS else RanchiCoverageStatus.GAP,
                gaps = gaps,
                actual = hits.map { it.name },
                actualPackTypes = hits.mapNotNull { it.packType }.distinct(),
            )
        }

        val gaps = linkedSetOf<String>()
        if (case.requiresLocationContext && case.locationContext.isNullOrBlank()) {
            gaps += GAP_LOCATION_CONTEXT_REQUIRED
        }

        val scoringHits = hits.filterNot { hit -> isGenericContextOnly(case, hit) }
        if (hits.isEmpty()) {
            gaps += GAP_NO_OFFLINE_EVIDENCE
        } else if (scoringHits.isEmpty()) {
            gaps += GAP_GENERIC_ONLY_RESULT
        } else {
            if (scoringHits.size < case.minHits) gaps += GAP_THIN_OFFLINE_COVERAGE
            if (
                case.expectedPackTypes.isNotEmpty() &&
                scoringHits.none { hit -> hit.packType?.let(case.expectedPackTypes::contains) == true }
            ) {
                gaps += GAP_WRONG_RESULT_TYPE
            }
            case.requiredFactGroups.forEach { group ->
                if (!hasFactGroup(group, scoringHits)) gaps += "MISSING_$group"
            }
        }

        if (case.requiresCurrentData) gaps += GAP_LIVE_FRESHNESS_REQUIRED

        val fatalEvidenceGap =
            hits.isEmpty() ||
                scoringHits.isEmpty() ||
                GAP_WRONG_RESULT_TYPE in gaps ||
                GAP_LOCATION_CONTEXT_REQUIRED in gaps

        val status = when {
            fatalEvidenceGap -> RanchiCoverageStatus.GAP
            gaps.isEmpty() -> RanchiCoverageStatus.PASS
            else -> RanchiCoverageStatus.PARTIAL
        }

        return RanchiCoverageResult(
            case = case,
            effectiveQuery = effectiveQuery,
            status = status,
            gaps = gaps.toList(),
            actual = hits.map { it.name },
            actualPackTypes = hits.mapNotNull { it.packType }.distinct(),
        )
    }

    private fun isGenericContextOnly(
        case: RanchiCoverageCase,
        hit: OfflineKnowledgeHit,
    ): Boolean {
        if (case.category in GENERIC_GEOGRAPHY_ALLOWED_CATEGORIES) return false
        return hit.packType in GENERIC_GEOGRAPHY_PACK_TYPES
    }

    private fun buildEffectiveQuery(case: RanchiCoverageCase): String {
        val location = case.locationContext?.trim().orEmpty()
        return if (location.isBlank()) case.query else "${case.query} $location"
    }

    private fun hasFactGroup(group: String, hits: List<OfflineKnowledgeHit>): Boolean {
        val aliases = FACT_GROUP_ALIASES[group].orEmpty()
        if (aliases.isEmpty()) return true
        val searchable = buildString {
            hits.forEach { hit ->
                append(' ').append(hit.description.orEmpty())
                hit.facts.forEach { fact ->
                    append(' ').append(fact.field).append(' ').append(fact.value)
                }
            }
        }.lowercase()
        return aliases.any(searchable::contains)
    }

    private fun loadCases(assetName: String): List<RanchiCoverageCase> =
        appContext.assets.open(assetName).bufferedReader().useLines { lines ->
            lines.map(String::trim)
                .filter(String::isNotEmpty)
                .map(::parseCase)
                .toList()
        }

    private fun parseCase(line: String): RanchiCoverageCase {
        val json = JSONObject(line)
        val category = json.getString("category")
        return RanchiCoverageCase(
            id = json.getString("id"),
            persona = json.optString("persona", "general"),
            query = json.getString("query"),
            category = category,
            locationContext = json.optString("locationContext").takeIf(String::isNotBlank),
            requiresLocationContext = json.optBoolean("requiresLocationContext", false),
            expectedPackTypes = json.stringList("expectedPackTypes").toSet(),
            minHits = json.optInt("minHits", 1).coerceAtLeast(1),
            requiredFactGroups = json.stringList("requiredFactGroups"),
            requiresCurrentData = json.optBoolean("requiresCurrentData", false),
            expectNoAnswer = json.optBoolean("expectNoAnswer", category == "out-of-domain"),
        )
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) add(array.getString(index))
        }
    }

    private fun summarize(
        results: List<RanchiCoverageResult>,
        key: (RanchiCoverageResult) -> String,
    ): Map<String, CoverageBucket> = results
        .groupBy(key)
        .mapValues { (_, values) ->
            CoverageBucket(
                total = values.size,
                pass = values.count { it.status == RanchiCoverageStatus.PASS },
                partial = values.count { it.status == RanchiCoverageStatus.PARTIAL },
                gap = values.count { it.status == RanchiCoverageStatus.GAP },
            )
        }
        .toSortedMap()

    companion object {
        const val GAP_NO_OFFLINE_EVIDENCE = "NO_OFFLINE_EVIDENCE"
        const val GAP_GENERIC_ONLY_RESULT = "GENERIC_ONLY_RESULT"
        const val GAP_THIN_OFFLINE_COVERAGE = "THIN_OFFLINE_COVERAGE"
        const val GAP_WRONG_RESULT_TYPE = "WRONG_RESULT_TYPE"
        const val GAP_LIVE_FRESHNESS_REQUIRED = "LIVE_FRESHNESS_REQUIRED"
        const val GAP_LOCATION_CONTEXT_REQUIRED = "LOCATION_CONTEXT_REQUIRED"
        const val GAP_OUT_OF_DOMAIN_LEAK = "OUT_OF_DOMAIN_LEAK"

        private val GENERIC_GEOGRAPHY_PACK_TYPES = setOf(
            "CITY",
            "DISTRICT",
            "REGION",
            "SUBDIVISION",
        )

        // These questions can legitimately be answered by facts attached to Ranchi itself.
        // Discovery/recommendation/service questions are deliberately excluded so a generic
        // Ranchi row cannot masquerade as a restaurant, route, attraction or local service.
        private val GENERIC_GEOGRAPHY_ALLOWED_CATEGORIES = setOf(
            "climate",
            "weather-live",
            "traffic-live",
            "government-live",
            "event-live",
        )

        private val FACT_GROUP_ALIASES = mapOf(
            "ADDRESS" to listOf("address", "locality", "located", "location", "village", "road"),
            "COORDINATES" to listOf("latitude", "longitude", "coordinate", "coordinates", " lat ", " lon "),
            "ACCESSIBILITY" to listOf("accessibility", "accessible", "wheelchair", "stairs", "steps", "walking", "walk"),
            "PARKING" to listOf("parking", "park vehicle"),
            "TIMINGS" to listOf("timing", "timings", "opening", "closing", "open hours", "hours"),
            "OFFERINGS" to listOf("sells", "sold", "available", "availability", "market for", "food", "meat", "vegetable", "produce"),
            "CUISINE" to listOf("cuisine", "dish", "food type", "menu"),
            "PRICE" to listOf("price", "cost", "fare", "rate", "₹", "rs."),
            "FAMILY" to listOf("family", "children", "kids", "child friendly"),
            "KIDS" to listOf("kids", "children", "child", "playground", "play area"),
            "SEASON" to listOf("season", "seasonal", "monsoon", "month", "summer", "winter"),
            "SAFETY" to listOf("safe", "safety", "danger", "risk", "slippery", "guard", "rail", "lighting"),
            "CONTACT" to listOf("phone", "contact", "telephone", "helpline", "mobile"),
            "TOILET" to listOf("toilet", "washroom", "restroom"),
            "TRANSPORT" to listOf("bus", "auto", "taxi", "rail", "station", "airport", "transport", "route"),
            "PAYMENT" to listOf("cash", "card", "upi", "payment"),
            "FACILITY" to listOf("facility", "facilities", "service", "services", "amenity", "amenities"),
        )
    }
}

data class RanchiCoverageCase(
    val id: String,
    val persona: String,
    val query: String,
    val category: String,
    val locationContext: String?,
    val requiresLocationContext: Boolean,
    val expectedPackTypes: Set<String>,
    val minHits: Int,
    val requiredFactGroups: List<String>,
    val requiresCurrentData: Boolean,
    val expectNoAnswer: Boolean,
)

data class RanchiCoverageResult(
    val case: RanchiCoverageCase,
    val effectiveQuery: String,
    val status: RanchiCoverageStatus,
    val gaps: List<String>,
    val actual: List<String>,
    val actualPackTypes: List<String>,
)

data class RanchiCoverageReport(
    val total: Int,
    val pass: Int,
    val partial: Int,
    val gap: Int,
    val gapCounts: Map<String, Int>,
    val categoryCounts: Map<String, CoverageBucket>,
    val personaCounts: Map<String, CoverageBucket>,
    val results: List<RanchiCoverageResult>,
)

data class CoverageBucket(
    val total: Int,
    val pass: Int,
    val partial: Int,
    val gap: Int,
)

enum class RanchiCoverageStatus {
    PASS,
    PARTIAL,
    GAP,
}
