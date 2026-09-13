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
        val title = normalizeText(name)
        val descriptionText = normalizeText(description.orEmpty())
        if (title.isBlank()) return false
        if (looksLikeUnresolvedSourceId(name)) return false
        if (isGenericOrNonEntityTitle(title)) return false

        val categorySupported = when (keyword.category) {
            DiscoveryCategory.FOOD ->
                inferredType == EntityType.FOOD && FOOD_TERMS.any(title::contains)
            DiscoveryCategory.FESTIVALS ->
                inferredType == EntityType.FESTIVAL && FESTIVAL_TERMS.any(title::contains)
            DiscoveryCategory.CULTURE ->
                inferredType == EntityType.CULTURAL_PRACTICE && CULTURE_TERMS.any(title::contains)
            DiscoveryCategory.EMERGENCY ->
                inferredType in EMERGENCY_TYPES && EMERGENCY_TERMS.any(title::contains)
            DiscoveryCategory.LOCAL_BAZAR ->
                inferredType in MARKET_TYPES && MARKET_TERMS.any(title::contains)
            DiscoveryCategory.PLACES ->
                inferredType in PLACE_TYPES && isPlaceLikeTitle(title)
            DiscoveryCategory.WEATHER -> false
        }
        if (!categorySupported) return false

        return hasJharkhandEvidence(title, descriptionText)
    }

    fun looksLikeUnresolvedSourceId(name: String): Boolean =
        WIKIDATA_ID.matches(name.trim())

    private fun isGenericOrNonEntityTitle(title: String): Boolean =
        GENERIC_TITLE_PREFIXES.any(title::startsWith) ||
            NON_V1_TITLE_TERMS.any(title::contains)

    private fun isPlaceLikeTitle(title: String): Boolean =
        JHARKHAND_LOCALITY_TERMS.any(title::contains) ||
            PLACE_TITLE_TERMS.any(title::contains)

    private fun hasJharkhandEvidence(title: String, description: String): Boolean {
        if ("jharkhand" in title) return true
        if (JHARKHAND_LOCALITY_TERMS.any(title::contains)) return true
        return JHARKHAND_LOCATION_PHRASES.any(description::contains)
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

    private val JHARKHAND_LOCALITY_TERMS = listOf(
        "ranchi", "jamshedpur", "dhanbad", "bokaro", "deoghar", "dumka", "hazaribagh", "giridih",
        "palamu", "latehar", "khunti", "simdega", "gumla", "lohardaga", "chaibasa", "seraikela",
        "pakur", "godda", "sahibganj", "jamtara", "ramgarh", "chatra", "koderma", "garhwa",
    )

    private val JHARKHAND_LOCATION_PHRASES = listOf(
        "in jharkhand",
        "of jharkhand",
        "jharkhand india",
        "jharkhand state",
        "jharkhand district",
        "located in jharkhand",
        "situated in jharkhand",
    )

    private val FOOD_TERMS = listOf(
        "food", "dish", "cuisine", "snack", "sweet", "roti", "pitha", "chutney", "pickle", "laddu", "ladoo",
    )
    private val FESTIVAL_TERMS = listOf(
        "festival", "fair", "mela", "puja", "utsav", "sarhul", "sohrai", "karam",
    )
    private val CULTURE_TERMS = listOf(
        "culture", "dance", "music", "language", "tribe", "tribal", "folk", "art", "craft", "painting",
        "tradition", "ritual",
    )
    private val EMERGENCY_TERMS = listOf(
        "hospital", "clinic", "police", "ambulance", "fire station", "emergency",
    )
    private val MARKET_TERMS = listOf(
        "market", "bazar", "bazaar", "haat", "mandi", "shop",
    )
    private val PLACE_TITLE_TERMS = listOf(
        "waterfall", "falls", "national park", "wildlife sanctuary", "forest reserve", "dam", "hill", "lake",
        "temple", "village", "town", "city", "district", "river", "railway station", "airport",
    )

    private val GENERIC_TITLE_PREFIXES = listOf(
        "list of ",
        "outline of ",
        "tourism in ",
    )

    private val NON_V1_TITLE_TERMS = listOf(
        "legislative assembly",
        " assembly election",
        " election",
        "government of ",
        "cricket team",
    )

    private val WIKIDATA_ID = Regex("^Q\\d+$", RegexOption.IGNORE_CASE)
}
