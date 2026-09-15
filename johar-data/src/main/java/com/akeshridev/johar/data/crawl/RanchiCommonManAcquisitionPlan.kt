package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.domain.crawl.DiscoveryCategory

/**
 * Product-oriented Ranchi acquisition plan.
 *
 * This is intentionally separate from source adapters. It describes what normal Ranchi users
 * need first; adapters decide how each seed is resolved. All results still flow into the same
 * canonical entity/fact/relationship model.
 */
internal object RanchiCommonManAcquisitionPlan {
    enum class Domain {
        HEALTH_EMERGENCY,
        TRANSPORT_MOBILITY,
        DAILY_LIFE,
        EDUCATION,
        FOOD_MARKETS,
        PLACES_RECREATION,
        CULTURE_LOCAL_KNOWLEDGE,
    }

    data class Seed(
        val domain: Domain,
        val priority: Int,
        val category: DiscoveryCategory,
        val term: String,
    )

    val seeds: List<Seed> = listOf(
        // P0: common daily needs. Keep these first because public sources are best-effort.
        Seed(Domain.HEALTH_EMERGENCY, 0, DiscoveryCategory.EMERGENCY, "hospitals in Ranchi"),
        Seed(Domain.HEALTH_EMERGENCY, 0, DiscoveryCategory.EMERGENCY, "pharmacies in Ranchi"),
        Seed(Domain.HEALTH_EMERGENCY, 0, DiscoveryCategory.EMERGENCY, "clinics in Ranchi"),
        Seed(Domain.HEALTH_EMERGENCY, 0, DiscoveryCategory.EMERGENCY, "police stations in Ranchi"),
        Seed(Domain.TRANSPORT_MOBILITY, 0, DiscoveryCategory.PLACES, "railway stations in Ranchi"),
        Seed(Domain.TRANSPORT_MOBILITY, 0, DiscoveryCategory.PLACES, "bus terminals in Ranchi"),
        Seed(Domain.TRANSPORT_MOBILITY, 0, DiscoveryCategory.PLACES, "airport in Ranchi"),
        Seed(Domain.DAILY_LIFE, 0, DiscoveryCategory.LOCAL_BAZAR, "banks and ATMs in Ranchi"),
        Seed(Domain.DAILY_LIFE, 0, DiscoveryCategory.LOCAL_BAZAR, "petrol pumps in Ranchi"),
        Seed(Domain.DAILY_LIFE, 0, DiscoveryCategory.LOCAL_BAZAR, "public toilets in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 0, DiscoveryCategory.LOCAL_BAZAR, "restaurants in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 0, DiscoveryCategory.LOCAL_BAZAR, "haat in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 0, DiscoveryCategory.PLACES, "localities in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 0, DiscoveryCategory.PLACES, "parks in Ranchi"),
        Seed(Domain.EDUCATION, 0, DiscoveryCategory.PLACES, "schools in Ranchi"),
        Seed(Domain.EDUCATION, 0, DiscoveryCategory.PLACES, "colleges in Ranchi"),
        Seed(Domain.EDUCATION, 0, DiscoveryCategory.PLACES, "universities in Ranchi"),

        // P1: breadth that makes "Ask anything about Ranchi" useful beyond emergency lookup.
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "neighborhoods in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "tourist attractions in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "temples in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "children parks in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "waterfalls near Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "picnic spots near Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "lakes near Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "dams near Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "viewpoints near Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "parking in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 1, DiscoveryCategory.PLACES, "hotels in Ranchi"),
        Seed(Domain.EDUCATION, 1, DiscoveryCategory.PLACES, "libraries in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "cafes in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "street food in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "vegetarian restaurants in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "bazar in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "weekly markets in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "mandi in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "vegetable markets in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "fish markets in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "mutton shops in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 1, DiscoveryCategory.LOCAL_BAZAR, "pork shops in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "supermarkets in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "shopping malls in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "EV charging stations in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "mobile repair shops in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "bike repair shops in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "car repair shops in Ranchi"),
        Seed(Domain.DAILY_LIFE, 1, DiscoveryCategory.LOCAL_BAZAR, "courier services in Ranchi"),
        Seed(Domain.HEALTH_EMERGENCY, 1, DiscoveryCategory.EMERGENCY, "fire stations in Ranchi"),
        Seed(Domain.HEALTH_EMERGENCY, 1, DiscoveryCategory.EMERGENCY, "ambulance services in Ranchi"),

        // P2: local identity, culture and less-frequent but valuable knowledge.
        Seed(Domain.FOOD_MARKETS, 2, DiscoveryCategory.FOOD, "traditional food in Ranchi Jharkhand"),
        Seed(Domain.FOOD_MARKETS, 2, DiscoveryCategory.FOOD, "local food in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 2, DiscoveryCategory.FOOD, "seasonal food in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 2, DiscoveryCategory.FOOD, "tribal food in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 2, DiscoveryCategory.FOOD, "Dhuska in Ranchi"),
        Seed(Domain.FOOD_MARKETS, 2, DiscoveryCategory.FOOD, "Rugra in Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.FESTIVALS, "festivals in Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.FESTIVALS, "Sarhul in Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.FESTIVALS, "Karma festival in Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.CULTURE, "Munda culture in Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.CULTURE, "Oraon culture in Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.CULTURE, "Birsa Munda Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.CULTURE, "Chota Nagpur history Ranchi"),
        Seed(Domain.CULTURE_LOCAL_KNOWLEDGE, 2, DiscoveryCategory.CULTURE, "languages spoken in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "churches in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "mosques in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "gurudwaras in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "museums in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "heritage places in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "stadiums in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "gyms in Ranchi"),
        Seed(Domain.PLACES_RECREATION, 2, DiscoveryCategory.PLACES, "cinemas in Ranchi"),
        Seed(Domain.DAILY_LIFE, 2, DiscoveryCategory.PLACES, "government offices in Ranchi"),
    ).sortedWith(compareBy<Seed> { it.priority }.thenBy { it.domain.ordinal })

    fun countsByDomain(): Map<Domain, Int> = seeds.groupingBy { it.domain }.eachCount()
}
