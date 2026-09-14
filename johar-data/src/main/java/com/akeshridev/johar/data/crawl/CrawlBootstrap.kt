package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlTarget
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity

internal object CrawlBootstrap {
    fun rootKeywords(
        rootEntityId: String,
        target: CrawlTarget = CrawlTarget.RANCHI,
    ): List<CrawlKeyword> = when (target) {
        CrawlTarget.RANCHI -> ranchiKeywords(rootEntityId)
        CrawlTarget.JHARKHAND -> jharkhandKeywords(rootEntityId)
        CrawlTarget.DASSAM_FALLS -> emptyList()
    }

    private fun ranchiKeywords(rootEntityId: String): List<CrawlKeyword> = buildList {
        // Places, localities, tourism and mobility landmarks.
        seed(rootEntityId, DiscoveryCategory.PLACES, "neighborhoods in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "localities in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "villages near Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "tourist attractions in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "waterfalls near Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "picnic spots near Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "parks in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "children parks in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "lakes near Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "dams near Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "viewpoints near Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "temples in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "churches in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "mosques in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "gurudwaras in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "museums in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "heritage places in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "hotels in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "convention centres in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "event venues in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "railway stations in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "bus terminals in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "airport in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "parking in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "colleges in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "universities in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "libraries in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "stadiums in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "gyms in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "cinemas in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "government offices in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "industrial areas in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "business areas in Ranchi")
        seed(rootEntityId, DiscoveryCategory.PLACES, "coworking spaces in Ranchi")

        // Food and eating. Keep Jharkhand terms when they represent cuisine, but discover them around Ranchi.
        seed(rootEntityId, DiscoveryCategory.FOOD, "traditional food in Ranchi Jharkhand")
        seed(rootEntityId, DiscoveryCategory.FOOD, "local food in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FOOD, "seasonal food in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FOOD, "tribal food in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FOOD, "Dhuska in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FOOD, "Rugra in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "restaurants in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "vegetarian restaurants in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "cafes in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "street food in Ranchi")

        // Festivals, culture and history.
        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "festivals in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "Sarhul in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "Karma festival in Ranchi")
        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "local fairs in Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "Munda culture in Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "Oraon culture in Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "Birsa Munda Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "Chota Nagpur history Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "tribal culture in Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "dance music crafts in Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "Sohrai art in Ranchi")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "languages spoken in Ranchi")

        // Health and emergency.
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "hospitals in Ranchi")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "clinics in Ranchi")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "pharmacies in Ranchi")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "police stations in Ranchi")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "fire stations in Ranchi")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "ambulance services in Ranchi")

        // Markets, shopping and everyday local services.
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "haat in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "bazar in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "weekly markets in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "mandi in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "vegetable markets in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "fish markets in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "mutton shops in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "pork shops in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "handicraft shops in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "shopping malls in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "supermarkets in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "banks and ATMs in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "petrol pumps in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "EV charging stations in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "public toilets in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "mobile repair shops in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "bike repair shops in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "car repair shops in Ranchi")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "courier services in Ranchi")
    }

    private fun jharkhandKeywords(rootEntityId: String): List<CrawlKeyword> = buildList {
        // Retain a small statewide mode for later expansion, but it is not the V1 default.
        seed(rootEntityId, DiscoveryCategory.PLACES, "places in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.PLACES, "waterfalls in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.FOOD, "traditional food of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "festivals of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "culture of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "hospitals in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "local markets in Jharkhand")
    }

    fun keywordsFor(entity: KnowledgeEntity): List<CrawlKeyword> {
        val category = when (entity.type) {
            EntityType.FOOD -> DiscoveryCategory.FOOD
            EntityType.FESTIVAL -> DiscoveryCategory.FESTIVALS
            EntityType.CULTURAL_PRACTICE -> DiscoveryCategory.CULTURE
            EntityType.MARKET,
            EntityType.SHOP,
            EntityType.RESTAURANT -> DiscoveryCategory.LOCAL_BAZAR
            EntityType.HOSPITAL,
            EntityType.POLICE_STATION,
            EntityType.EMERGENCY_SERVICE -> DiscoveryCategory.EMERGENCY
            else -> DiscoveryCategory.PLACES
        }

        return listOf(
            keyword(
                entityId = entity.id,
                category = category,
                term = entity.name,
                discoveredFrom = "entity:${entity.id}",
            ),
        )
    }

    private fun MutableList<CrawlKeyword>.seed(
        entityId: String,
        category: DiscoveryCategory,
        term: String,
    ) {
        add(keyword(entityId, category, term, "bootstrap"))
    }

    private fun keyword(
        entityId: String,
        category: DiscoveryCategory,
        term: String,
        discoveredFrom: String,
    ): CrawlKeyword = CrawlKeyword(
        id = stableId("keyword", entityId, category.name, normalizeText(term)),
        entityId = entityId,
        category = category,
        term = term,
        discoveredFrom = discoveredFrom,
    )
}
