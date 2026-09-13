package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity

internal object CrawlBootstrap {
    fun rootKeywords(rootEntityId: String): List<CrawlKeyword> = buildList {
        seed(rootEntityId, DiscoveryCategory.PLACES, "places in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.PLACES, "waterfalls in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.PLACES, "picnic spots in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.PLACES, "villages in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.PLACES, "rivers in Jharkhand")

        seed(rootEntityId, DiscoveryCategory.FOOD, "traditional food of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.FOOD, "local food of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.FOOD, "seasonal food of Jharkhand")

        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "festivals of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.FESTIVALS, "tribal festivals of Jharkhand")

        seed(rootEntityId, DiscoveryCategory.CULTURE, "culture of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "tribal culture of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "dance of Jharkhand")
        seed(rootEntityId, DiscoveryCategory.CULTURE, "crafts of Jharkhand")

        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "hospitals in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.EMERGENCY, "police stations in Jharkhand")

        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "haat in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "bazar in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "weekly markets in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "local markets in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "butcher shops in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "meat shops in Jharkhand")
        seed(rootEntityId, DiscoveryCategory.LOCAL_BAZAR, "pork shops in Jharkhand")
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
