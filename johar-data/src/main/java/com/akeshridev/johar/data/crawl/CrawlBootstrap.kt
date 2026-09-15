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

    private fun ranchiKeywords(rootEntityId: String): List<CrawlKeyword> =
        RanchiCommonManAcquisitionPlan.seeds.map { seed ->
            keyword(
                entityId = rootEntityId,
                category = seed.category,
                term = seed.term,
                discoveredFrom = "bootstrap",
            )
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
