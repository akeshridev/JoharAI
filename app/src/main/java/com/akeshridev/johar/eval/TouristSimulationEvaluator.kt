package com.akeshridev.johar.eval

import android.content.Context
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeHit
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import org.json.JSONObject

class TouristSimulationEvaluator(
    context: Context,
    private val retriever: OfflineKnowledgeRetriever,
) {
    private val appContext = context.applicationContext

    fun run(assetName: String = "tourist-eval.jsonl"): TouristEvalReport {
        val results = loadCases(assetName).map(::evaluate)
        val gapCounts = results
            .flatMap(TouristEvalResult::gaps)
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
        return TouristEvalReport(
            total = results.size,
            pass = results.count { it.status == TouristEvalStatus.PASS },
            partial = results.count { it.status == TouristEvalStatus.PARTIAL },
            gap = results.count { it.status == TouristEvalStatus.GAP },
            gapCounts = gapCounts,
            results = results,
        )
    }

    private fun evaluate(case: TouristEvalCase): TouristEvalResult {
        val effectiveQuery = buildEffectiveQuery(case)
        val hits = retriever.retrieve(effectiveQuery, limit = 5)
        val gaps = linkedSetOf<String>()

        if (case.requiresLocationContext && case.locationContext.isNullOrBlank()) {
            gaps += GAP_LOCATION_CONTEXT_REQUIRED
        }

        if (hits.isEmpty()) {
            gaps += GAP_NO_OFFLINE_EVIDENCE
        } else {
            if (hits.size < case.minHits) {
                gaps += GAP_THIN_OFFLINE_COVERAGE
            }

            if (case.expectedPackTypes.isNotEmpty() && hits.none { it.packType in case.expectedPackTypes }) {
                gaps += GAP_WRONG_RESULT_TYPE
            }

            case.requiredFactGroups.forEach { group ->
                if (!hasFactGroup(group, hits)) {
                    gaps += "MISSING_$group"
                }
            }
        }

        if (case.requiresCurrentData) {
            gaps += GAP_LIVE_FRESHNESS_REQUIRED
        }

        val status = when {
            hits.isEmpty() -> TouristEvalStatus.GAP
            gaps.isEmpty() -> TouristEvalStatus.PASS
            else -> TouristEvalStatus.PARTIAL
        }

        return TouristEvalResult(
            case = case,
            effectiveQuery = effectiveQuery,
            status = status,
            gaps = gaps.toList(),
            actual = hits.map { it.name },
            actualPackTypes = hits.mapNotNull { it.packType }.distinct(),
        )
    }

    private fun buildEffectiveQuery(case: TouristEvalCase): String {
        val location = case.locationContext?.trim().orEmpty()
        return if (location.isBlank()) case.query else "${case.query} $location"
    }

    private fun hasFactGroup(group: String, hits: List<OfflineKnowledgeHit>): Boolean {
        val aliases = FACT_GROUP_ALIASES[group].orEmpty()
        if (aliases.isEmpty()) return true

        val searchable = buildString {
            hits.forEach { hit ->
                append(' ')
                append(hit.description.orEmpty())
                hit.facts.forEach { fact ->
                    append(' ')
                    append(fact.field)
                    append(' ')
                    append(fact.value)
                }
            }
        }.lowercase()

        return aliases.any(searchable::contains)
    }

    private fun loadCases(assetName: String): List<TouristEvalCase> =
        appContext.assets.open(assetName).bufferedReader().useLines { lines ->
            lines
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(::parseCase)
                .toList()
        }

    private fun parseCase(line: String): TouristEvalCase {
        val json = JSONObject(line)
        return TouristEvalCase(
            id = json.getString("id"),
            query = json.getString("query"),
            category = json.getString("category"),
            locationContext = json.optString("locationContext").takeIf(String::isNotBlank),
            requiresLocationContext = json.optBoolean("requiresLocationContext", false),
            expectedPackTypes = json.stringList("expectedPackTypes").toSet(),
            minHits = json.optInt("minHits", 1).coerceAtLeast(1),
            requiredFactGroups = json.stringList("requiredFactGroups"),
            requiresCurrentData = json.optBoolean("requiresCurrentData", false),
        )
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) add(array.getString(index))
        }
    }

    companion object {
        const val GAP_NO_OFFLINE_EVIDENCE = "NO_OFFLINE_EVIDENCE"
        const val GAP_THIN_OFFLINE_COVERAGE = "THIN_OFFLINE_COVERAGE"
        const val GAP_WRONG_RESULT_TYPE = "WRONG_RESULT_TYPE"
        const val GAP_LIVE_FRESHNESS_REQUIRED = "LIVE_FRESHNESS_REQUIRED"
        const val GAP_LOCATION_CONTEXT_REQUIRED = "LOCATION_CONTEXT_REQUIRED"

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
            "SAFETY" to listOf("safe", "safety", "danger", "risk", "slippery", "guard", "rail"),
        )
    }
}

data class TouristEvalCase(
    val id: String,
    val query: String,
    val category: String,
    val locationContext: String?,
    val requiresLocationContext: Boolean,
    val expectedPackTypes: Set<String>,
    val minHits: Int,
    val requiredFactGroups: List<String>,
    val requiresCurrentData: Boolean,
)

data class TouristEvalResult(
    val case: TouristEvalCase,
    val effectiveQuery: String,
    val status: TouristEvalStatus,
    val gaps: List<String>,
    val actual: List<String>,
    val actualPackTypes: List<String>,
)

data class TouristEvalReport(
    val total: Int,
    val pass: Int,
    val partial: Int,
    val gap: Int,
    val gapCounts: Map<String, Int>,
    val results: List<TouristEvalResult>,
)

enum class TouristEvalStatus {
    PASS,
    PARTIAL,
    GAP,
}
