package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow
import org.json.JSONArray
import org.json.JSONObject

enum class FoundationDomain {
    HEALTH_EMERGENCY,
    TRANSPORT_MOBILITY,
    DAILY_LIFE,
    EDUCATION,
    FOOD_MARKETS,
    PLACES_RECREATION,
    CULTURE_LOCAL_KNOWLEDGE,
    UNCLASSIFIED,
}

data class FoundationDomainCoverage(
    val domain: FoundationDomain,
    val entities: Int,
    val withCoordinates: Int,
    val withAliases: Int,
    val withFacts: Int,
    val facts: Int,
    val evidenceCompleteFacts: Int,
)

data class FoundationCoverageReport(
    val domains: List<FoundationDomainCoverage>,
) {
    val totalEntities: Int get() = domains.sumOf { it.entities }
    val totalFacts: Int get() = domains.sumOf { it.facts }
}

internal object FoundationCoverageReporter {
    fun build(
        entities: List<KnowledgeEntityRow>,
        facts: List<SourceFactRow>,
    ): FoundationCoverageReport {
        val factsByEntity = facts.groupBy(SourceFactRow::entityId)
        val grouped = entities.groupBy(::domainFor)

        val rows = FoundationDomain.entries.map { domain ->
            val domainEntities = grouped[domain].orEmpty()
            val domainFacts = domainEntities.flatMap { factsByEntity[it.id].orEmpty() }
            FoundationDomainCoverage(
                domain = domain,
                entities = domainEntities.size,
                withCoordinates = domainEntities.count { it.latitude != null && it.longitude != null },
                withAliases = domainEntities.count { aliasCount(it.aliasesJson) > 0 },
                withFacts = domainEntities.count { factsByEntity[it.id].orEmpty().isNotEmpty() },
                facts = domainFacts.size,
                evidenceCompleteFacts = domainFacts.count(::hasCompleteEvidence),
            )
        }
        return FoundationCoverageReport(rows)
    }

    internal fun domainFor(entity: KnowledgeEntityRow): FoundationDomain {
        val packType = packType(entity.externalRefsJson)
        return when {
            entity.type in HEALTH_TYPES || packType in HEALTH_PACK_TYPES -> FoundationDomain.HEALTH_EMERGENCY
            entity.type in TRANSPORT_TYPES || packType in TRANSPORT_PACK_TYPES -> FoundationDomain.TRANSPORT_MOBILITY
            packType in EDUCATION_PACK_TYPES -> FoundationDomain.EDUCATION
            entity.type in FOOD_MARKET_TYPES || packType in FOOD_MARKET_PACK_TYPES -> FoundationDomain.FOOD_MARKETS
            entity.type in CULTURE_TYPES || packType in CULTURE_PACK_TYPES -> FoundationDomain.CULTURE_LOCAL_KNOWLEDGE
            packType in DAILY_LIFE_PACK_TYPES -> FoundationDomain.DAILY_LIFE
            entity.type in PLACE_TYPES || packType in PLACE_PACK_TYPES -> FoundationDomain.PLACES_RECREATION
            else -> FoundationDomain.UNCLASSIFIED
        }
    }

    private fun hasCompleteEvidence(fact: SourceFactRow): Boolean =
        fact.sourceUrl.isNotBlank() &&
            fact.publisher.isNotBlank() &&
            fact.evidenceText.isNotBlank() &&
            fact.retrievedAtEpochMillis > 0L

    private fun aliasCount(json: String): Int = runCatching { JSONArray(json).length() }.getOrDefault(0)

    private fun packType(json: String): String? = runCatching {
        JSONObject(json).optString("joharPackType").takeIf(String::isNotBlank)
    }.getOrNull()

    private val HEALTH_TYPES = setOf("HOSPITAL", "POLICE_STATION", "EMERGENCY_SERVICE")
    private val HEALTH_PACK_TYPES = setOf("HOSPITAL", "PHARMACY", "POLICE_STATION", "FIRE_STATION", "AMBULANCE")

    private val TRANSPORT_TYPES = setOf("RAILWAY_STATION", "BUS_STAND", "AIRPORT")
    private val TRANSPORT_PACK_TYPES = setOf("RAILWAY_STATION", "BUS_STAND", "AIRPORT", "PARKING")

    private val EDUCATION_PACK_TYPES = setOf("SCHOOL", "COLLEGE", "UNIVERSITY", "LIBRARY")

    private val FOOD_MARKET_TYPES = setOf("RESTAURANT", "MARKET")
    private val FOOD_MARKET_PACK_TYPES = setOf(
        "RESTAURANT", "CAFE", "STREET_FOOD", "MARKET_COLLECTION", "BUTCHER", "FISH_SHOP",
        "VEGETABLE_SHOP", "HANDICRAFT_SHOP",
    )

    private val DAILY_LIFE_PACK_TYPES = setOf(
        "BANK", "ATM", "FUEL", "EV_CHARGING", "TOILET", "REPAIR", "COURIER", "MALL", "SUPERMARKET",
    )

    private val CULTURE_TYPES = setOf("FOOD", "FESTIVAL", "CULTURAL_PRACTICE")
    private val CULTURE_PACK_TYPES = setOf("HERITAGE_SITE", "MUSEUM")

    private val PLACE_TYPES = setOf(
        "PLACE", "TOURIST_ATTRACTION", "NATURAL_FEATURE", "VILLAGE", "TOWN", "CITY", "DISTRICT", "REGION", "RIVER",
    )
    private val PLACE_PACK_TYPES = setOf(
        "LOCALITY", "PARK", "WATERFALL", "DAM", "LAKE", "HILL", "VIEWPOINT", "TEMPLE", "CHURCH", "MOSQUE",
        "GURUDWARA", "HOTEL", "STADIUM", "GYM", "CINEMA", "CONVENTION_VENUE", "EVENT_VENUE", "BUSINESS_AREA",
        "INDUSTRIAL_AREA", "COWORKING",
    )
}
