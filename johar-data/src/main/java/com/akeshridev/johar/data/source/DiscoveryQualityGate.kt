package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityType

internal object DiscoveryQualityGate {
    fun accept(
        keyword: CrawlKeyword,
        name: String,
        description: String?,
        inferredType: EntityType? = null,
    ): Boolean {
        val text = normalizeText(listOfNotNull(name, description).joinToString(" "))
        if (text.isBlank()) return false
        if (looksLikeUnresolvedSourceId(name)) return false

        val categorySupported = when (keyword.category) {
            DiscoveryCategory.FOOD ->
                inferredType == EntityType.FOOD || FOOD_TERMS.any(text::contains)
            DiscoveryCategory.FESTIVALS ->
                inferredType == EntityType.FESTIVAL || FESTIVAL_TERMS.any(text::contains)
            DiscoveryCategory.CULTURE ->
                inferredType == EntityType.CULTURAL_PRACTICE || CULTURE_TERMS.any(text::contains)
            DiscoveryCategory.EMERGENCY ->
                inferredType in EMERGENCY_TYPES || EMERGENCY_TERMS.any(text::contains)
            DiscoveryCategory.LOCAL_BAZAR ->
                inferredType in MARKET_TYPES || MARKET_TERMS.any(text::contains)
            DiscoveryCategory.PLACES ->
                inferredType in PLACE_TYPES || PLACE_TERMS.any(text::contains)
            DiscoveryCategory.WEATHER -> false
        }
        if (!categorySupported) return false

        return hasJharkhandEvidence(text) || isJharkhandSpecificKeyword(keyword)
    }

    fun looksLikeUnresolvedSourceId(name: String): Boolean =
        WIKIDATA_ID.matches(name.trim())

    private fun hasJharkhandEvidence(text: String): Boolean =
        JHARKHAND_TERMS.any(text::contains)

    private fun isJharkhandSpecificKeyword(keyword: CrawlKeyword): Boolean {
        val term = normalizeText(keyword.term)
        return JHARKHAND_TERMS.any(term::contains)
    }

    private val EMERGENCY_TYPES = setOf(
        EntityType.HOSPITAL,
        EntityType.POLICE_STATION,
        EntityType.EMERGENCY_SERVICE,
    )
    private val MARKET_TYPES = setOf(EntityType.MARKET, EntityType.SHOP)
    private val PLACE_TYPES = setOf(
        EntityType.PLACE,
        EntityType.TOURIST_ATTRACTION,
        EntityType.NATURAL_FEATURE,
        EntityType.VILLAGE,
        EntityType.TOWN,
        EntityType.CITY,
        EntityType.DISTRICT,
        EntityType.REGION,
        EntityType.RIVER,
        EntityType.AIRPORT,
        EntityType.RAILWAY_STATION,
        EntityType.BUS_STAND,
        EntityType.FACILITY,
    )

    private val JHARKHAND_TERMS = listOf(
        "jharkhand",
        "ranchi",
        "jamshedpur",
        "dhanbad",
        "bokaro",
        "deoghar",
        "dumka",
        "hazaribagh",
        "giridih",
        "palamu",
        "latehar",
        "khunti",
        "simdega",
        "gumla",
        "lohardaga",
        "chaibasa",
        "seraikela",
        "pakur",
        "godda",
        "sahibganj",
        "jamtara",
        "ramgarh",
        "chatra",
        "koderma",
        "garhwa",
    )

    private val FOOD_TERMS = listOf(
        "food", "dish", "cuisine", "snack", "sweet", "recipe", "bread", "rice", "curry", "chutney",
    )
    private val FESTIVAL_TERMS = listOf(
        "festival", "fair", "mela", "puja", "celebration", "utsav",
    )
    private val CULTURE_TERMS = listOf(
        "culture", "cultural", "dance", "music", "language", "tribe", "tribal", "people", "folk", "art",
        "craft", "painting", "tradition", "ritual",
    )
    private val EMERGENCY_TERMS = listOf(
        "hospital", "clinic", "police", "ambulance", "fire station", "emergency",
    )
    private val MARKET_TERMS = listOf(
        "market", "bazar", "bazaar", "haat", "mandi", "shop", "seller", "butcher",
    )
    private val PLACE_TERMS = listOf(
        "waterfall", "falls", "park", "sanctuary", "dam", "hill", "lake", "temple", "tourist", "village",
        "town", "city", "district", "river", "station", "airport",
    )

    private val WIKIDATA_ID = Regex("^Q\\d+$", RegexOption.IGNORE_CASE)
}
