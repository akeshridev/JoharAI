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

        val nonGenericHits = hits.filterNot { hit -> isGenericContextOnly(case, hit) }
        val semanticRule = semanticRuleFor(case)
        val scoringHits = if (semanticRule == null) {
            nonGenericHits
        } else {
            nonGenericHits.filter { hit -> semanticRule.matches(hit) }
        }

        when {
            hits.isEmpty() -> gaps += GAP_NO_OFFLINE_EVIDENCE
            nonGenericHits.isEmpty() -> gaps += GAP_GENERIC_ONLY_RESULT
            semanticRule != null && scoringHits.isEmpty() -> gaps += GAP_WRONG_RESULT_TYPE
            else -> {
                if (scoringHits.size < case.minHits) gaps += GAP_THIN_OFFLINE_COVERAGE
                case.requiredFactGroups.forEach { group ->
                    if (!hasFactGroup(group, scoringHits)) gaps += "MISSING_$group"
                }
            }
        }

        if (case.requiresCurrentData) gaps += GAP_LIVE_FRESHNESS_REQUIRED

        val fatalEvidenceGap =
            hits.isEmpty() ||
                nonGenericHits.isEmpty() ||
                (semanticRule != null && scoringHits.isEmpty()) ||
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

    private fun semanticRuleFor(case: RanchiCoverageCase): SemanticRule? {
        if (case.expectedPackTypes.isNotEmpty()) {
            return SemanticRule(packTypes = case.expectedPackTypes)
        }
        inferredRuleFromQuery(case.query)?.let { return it }
        return CATEGORY_RULES[case.category]
    }

    private fun inferredRuleFromQuery(query: String): SemanticRule? {
        val text = query.lowercase()
        return when {
            containsAny(text, "mutton", "pork", "fish market", "meat shop", "butcher") -> MEAT_MARKET_RULE
            containsAny(text, "restaurant", "resturant", "restaurnt", "cafe", "dining", "khana", "food place") -> FOOD_SERVICE_RULE
            containsAny(text, "mandir", "temple", "church", "mosque", "masjid", "gurudwara", "gurdwara") -> RELIGION_RULE
            containsAny(text, "market", "bazar", "bazaar", "haat", "mandi") -> MARKET_RULE
            containsAny(text, "pharmacy", "medical shop", "chemist") -> PHARMACY_RULE
            containsAny(text, "hospital", "clinic") -> HOSPITAL_RULE
            containsAny(text, "police", "fire station", "ambulance") -> EMERGENCY_RULE
            containsAny(text, "atm", "bank branch") -> BANK_RULE
            containsAny(text, "repair", "puncture") -> REPAIR_RULE
            containsAny(text, "mall", "handicraft", "souvenir") -> SHOPPING_RULE
            containsAny(text, "public toilet", "washroom", "restroom") -> TOILET_RULE
            containsAny(text, "ev charging", "charging station", "petrol", "fuel") -> MOBILITY_RULE
            containsAny(text, "stadium") -> STADIUM_RULE
            containsAny(text, "college", "university") -> EDUCATION_RULE
            containsAny(text, "library") -> LIBRARY_RULE
            containsAny(text, "cowork") -> COWORK_RULE
            containsAny(text, "it compan", "business area", "commercial area", "industrial area") -> BUSINESS_RULE
            containsAny(text, "convention", "meeting venue", "event venue") -> BUSINESS_VENUE_RULE
            containsAny(text, "hotel", "guest house") -> HOTEL_RULE
            containsAny(text, "railway", "train station", "bus stand", "bus terminal", "airport", "transport option", "route") -> TRANSPORT_RULE
            containsAny(text, "waterfall", "waterfal", "falls", "jharna") -> WATERFALL_RULE
            containsAny(text, "child park", "children park", "playground", "park") -> PARK_RULE
            containsAny(text, "trade license", "municipal office", "collector office", "dc office") -> GOVERNMENT_RULE
            else -> null
        }
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
        if (group == "COORDINATES" && hits.any { it.latitude != null && it.longitude != null }) return true

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

    private data class SemanticRule(
        val packTypes: Set<String> = emptySet(),
        val entityTypes: Set<String> = emptySet(),
    ) {
        fun matches(hit: OfflineKnowledgeHit): Boolean =
            hit.packType?.let(packTypes::contains) == true || hit.type in entityTypes
    }

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

        private val GENERIC_GEOGRAPHY_ALLOWED_CATEGORIES = setOf(
            "climate",
            "weather-live",
            "traffic-live",
            "government-live",
            "event-live",
        )

        private val PLACE_RECOMMENDATION_RULE = SemanticRule(
            packTypes = setOf(
                "WATERFALL", "DAM", "LAKE", "HILL", "VIEWPOINT", "PARK", "FOREST",
                "WILDLIFE_SANCTUARY", "TIGER_RESERVE", "NATIONAL_PARK", "TEMPLE", "PILGRIMAGE",
                "CHURCH", "MOSQUE", "GURUDWARA", "MUSEUM", "HERITAGE_SITE", "ZOO", "AQUARIUM",
                "TOURIST_ATTRACTION",
            ),
            entityTypes = setOf("TOURIST_ATTRACTION", "NATURAL_FEATURE"),
        )
        private val FOOD_SERVICE_RULE = SemanticRule(
            packTypes = setOf("RESTAURANT", "CAFE", "STREET_FOOD", "FOOD"),
            entityTypes = setOf("RESTAURANT", "FOOD"),
        )
        private val FOOD_KNOWLEDGE_RULE = SemanticRule(
            packTypes = setOf("FOOD"),
            entityTypes = setOf("FOOD"),
        )
        private val MEAT_MARKET_RULE = SemanticRule(
            packTypes = setOf("BUTCHER", "FISH_SHOP", "MARKET_COLLECTION", "MARKET_TYPE"),
            entityTypes = setOf("SHOP", "MARKET"),
        )
        private val MARKET_RULE = SemanticRule(
            packTypes = setOf("MARKET_COLLECTION", "MARKET_TYPE", "VEGETABLE_SHOP", "FISH_SHOP", "BUTCHER"),
            entityTypes = setOf("MARKET"),
        )
        private val RELIGION_RULE = SemanticRule(
            packTypes = setOf("TEMPLE", "PILGRIMAGE", "CHURCH", "MOSQUE", "GURUDWARA"),
            entityTypes = setOf("TOURIST_ATTRACTION", "FACILITY"),
        )
        private val EDUCATION_RULE = SemanticRule(
            packTypes = setOf("COLLEGE", "UNIVERSITY", "COLLEGE_UNIVERSITY", "SCHOOL"),
            entityTypes = setOf("ORGANIZATION"),
        )
        private val LIBRARY_RULE = SemanticRule(packTypes = setOf("LIBRARY"))
        private val BUSINESS_RULE = SemanticRule(
            packTypes = setOf("BUSINESS_AREA", "INDUSTRIAL_AREA", "COWORKING"),
            entityTypes = setOf("ORGANIZATION"),
        )
        private val COWORK_RULE = SemanticRule(packTypes = setOf("COWORKING"))
        private val BUSINESS_VENUE_RULE = SemanticRule(packTypes = setOf("CONVENTION_VENUE", "EVENT_VENUE", "HOTEL"))
        private val HOTEL_RULE = SemanticRule(packTypes = setOf("HOTEL"))
        private val TRANSPORT_RULE = SemanticRule(
            packTypes = setOf("AIRPORT", "RAILWAY_STATION", "BUS_STAND"),
            entityTypes = setOf("AIRPORT", "RAILWAY_STATION", "BUS_STAND"),
        )
        private val HOSPITAL_RULE = SemanticRule(packTypes = setOf("HOSPITAL"), entityTypes = setOf("HOSPITAL"))
        private val PHARMACY_RULE = SemanticRule(packTypes = setOf("PHARMACY"), entityTypes = setOf("SHOP"))
        private val EMERGENCY_RULE = SemanticRule(
            packTypes = setOf("POLICE_STATION", "FIRE_STATION", "AMBULANCE"),
            entityTypes = setOf("POLICE_STATION", "EMERGENCY_SERVICE"),
        )
        private val BANK_RULE = SemanticRule(packTypes = setOf("BANK", "ATM"))
        private val REPAIR_RULE = SemanticRule(packTypes = setOf("REPAIR"), entityTypes = setOf("SHOP"))
        private val SHOPPING_RULE = SemanticRule(packTypes = setOf("MALL", "HANDICRAFT_SHOP", "SUPERMARKET", "ART", "CRAFT"))
        private val TOILET_RULE = SemanticRule(packTypes = setOf("TOILET"))
        private val MOBILITY_RULE = SemanticRule(packTypes = setOf("FUEL", "EV_CHARGING"))
        private val STADIUM_RULE = SemanticRule(packTypes = setOf("STADIUM"))
        private val PARK_RULE = SemanticRule(packTypes = setOf("PARK"), entityTypes = setOf("TOURIST_ATTRACTION"))
        private val WATERFALL_RULE = SemanticRule(packTypes = setOf("WATERFALL"), entityTypes = setOf("NATURAL_FEATURE"))
        private val GOVERNMENT_RULE = SemanticRule(packTypes = setOf("MUNICIPALITY", "GOVERNMENT_OFFICE"), entityTypes = setOf("ORGANIZATION"))

        private val CATEGORY_RULES = mapOf(
            "itinerary" to PLACE_RECOMMENDATION_RULE,
            "day-trip" to PLACE_RECOMMENDATION_RULE,
            "budget-places" to PLACE_RECOMMENDATION_RULE,
            "family-places" to PLACE_RECOMMENDATION_RULE,
            "family-safety" to PLACE_RECOMMENDATION_RULE,
            "evening-place" to PLACE_RECOMMENDATION_RULE,
            "accessibility" to PLACE_RECOMMENDATION_RULE,
            "photography" to PLACE_RECOMMENDATION_RULE,
            "leisure" to PLACE_RECOMMENDATION_RULE,
            "multi-constraint" to PLACE_RECOMMENDATION_RULE,
            "family-facility" to PARK_RULE,
            "food-locality" to FOOD_SERVICE_RULE,
            "student-food" to FOOD_SERVICE_RULE,
            "near-me-food" to FOOD_SERVICE_RULE,
            "food-culture" to FOOD_KNOWLEDGE_RULE,
            "food-availability" to FOOD_SERVICE_RULE,
            "seasonal-food" to FOOD_KNOWLEDGE_RULE,
            "restaurant" to FOOD_SERVICE_RULE,
            "restaurant-ranking" to FOOD_SERVICE_RULE,
            "restaurant-local" to FOOD_SERVICE_RULE,
            "restaurant-live" to FOOD_SERVICE_RULE,
            "restaurant-premium" to FOOD_SERVICE_RULE,
            "cafe" to FOOD_SERVICE_RULE,
            "market" to MARKET_RULE,
            "market-day" to MARKET_RULE,
            "market-food" to MARKET_RULE,
            "market-locality" to MARKET_RULE,
            "business-market" to MARKET_RULE,
            "meat-market" to MEAT_MARKET_RULE,
            "inventory-live" to MEAT_MARKET_RULE,
            "religion" to RELIGION_RULE,
            "religion-nearby" to RELIGION_RULE,
            "religion-live" to RELIGION_RULE,
            "religion-accessibility" to RELIGION_RULE,
            "religion-practical" to RELIGION_RULE,
            "near-me" to RELIGION_RULE,
            "education" to EDUCATION_RULE,
            "education-locality" to LIBRARY_RULE,
            "work" to BUSINESS_RULE,
            "work-space" to COWORK_RULE,
            "business-area" to BUSINESS_RULE,
            "business-venue" to BUSINESS_VENUE_RULE,
            "business-travel" to BUSINESS_VENUE_RULE,
            "transport" to TRANSPORT_RULE,
            "transport-live" to TRANSPORT_RULE,
            "student-transport" to TRANSPORT_RULE,
            "business-transport" to TRANSPORT_RULE,
            "mobility-service" to MOBILITY_RULE,
            "health-emergency" to HOSPITAL_RULE,
            "health-nearby" to HOSPITAL_RULE,
            "pharmacy" to PHARMACY_RULE,
            "pharmacy-live" to PHARMACY_RULE,
            "emergency" to EMERGENCY_RULE,
            "emergency-nearby" to EMERGENCY_RULE,
            "banking-utility" to BANK_RULE,
            "repair-service" to REPAIR_RULE,
            "shopping" to SHOPPING_RULE,
            "shopping-culture" to SHOPPING_RULE,
            "sports" to STADIUM_RULE,
            "public-utility" to TOILET_RULE,
            "civic-business" to GOVERNMENT_RULE,
            "government" to GOVERNMENT_RULE,
        )

        private val FACT_GROUP_ALIASES = mapOf(
            "ADDRESS" to listOf("address", "locality", "street", "road", "village", "ward", "colony"),
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
            "TRANSPORT" to listOf("bus", "auto", "taxi", "cab", "train", "rail", "transport", "route", "how to reach"),
            "PAYMENT" to listOf("cash", "card", "upi", "payment"),
            "FACILITY" to listOf("facility", "facilities", "service", "services", "amenity", "amenities"),
        )

        private fun containsAny(text: String, vararg terms: String): Boolean = terms.any(text::contains)
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
