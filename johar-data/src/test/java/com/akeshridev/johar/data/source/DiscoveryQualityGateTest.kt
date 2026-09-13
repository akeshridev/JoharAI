package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryQualityGateTest {

    @Test
    fun rejectsWrongCategoryResultsSeenInRealCrawl() {
        assertFalse(
            DiscoveryQualityGate.accept(
                keyword = keyword(DiscoveryCategory.FOOD, "traditional food in Jharkhand"),
                name = "Dhanbad",
                description = "city in Jharkhand, India",
                inferredType = EntityType.CITY,
            ),
        )
        assertFalse(
            DiscoveryQualityGate.accept(
                keyword = keyword(DiscoveryCategory.LOCAL_BAZAR, "bazar in Jharkhand"),
                name = "Ranchi",
                description = "capital city of Jharkhand, India",
                inferredType = EntityType.CITY,
            ),
        )
        assertFalse(
            DiscoveryQualityGate.accept(
                keyword = keyword(DiscoveryCategory.PLACES, "rivers in Jharkhand"),
                name = "Bihar",
                description = "state in eastern India",
                inferredType = EntityType.REGION,
            ),
        )
    }

    @Test
    fun acceptsCategoryAndJharkhandSupportedResults() {
        assertTrue(
            DiscoveryQualityGate.accept(
                keyword = keyword(DiscoveryCategory.FOOD, "traditional food in Jharkhand"),
                name = "Dhuska",
                description = "traditional food and fried dish popular in Jharkhand",
                inferredType = EntityType.FOOD,
            ),
        )
        assertTrue(
            DiscoveryQualityGate.accept(
                keyword = keyword(DiscoveryCategory.CULTURE, "folk dance in Jharkhand"),
                name = "Chhau",
                description = "traditional dance associated with Jharkhand and eastern India",
                inferredType = EntityType.CULTURAL_PRACTICE,
            ),
        )
        assertTrue(
            DiscoveryQualityGate.accept(
                keyword = keyword(DiscoveryCategory.LOCAL_BAZAR, "haat in Jharkhand"),
                name = "Khunti Haat",
                description = "weekly market in Khunti, Jharkhand",
                inferredType = EntityType.MARKET,
            ),
        )
    }

    @Test
    fun rejectsUnresolvedWikidataIds() {
        assertFalse(DiscoveryQualityGate.looksLikeUnresolvedSourceId("Dhuska"))
        assertTrue(DiscoveryQualityGate.looksLikeUnresolvedSourceId("Q367344"))
        assertTrue(DiscoveryQualityGate.looksLikeUnresolvedSourceId("q5089"))
    }

    private fun keyword(category: DiscoveryCategory, term: String) = CrawlKeyword(
        id = "test:$category:$term",
        entityId = "entity:jharkhand",
        category = category,
        term = term,
        discoveredFrom = "bootstrap",
    )
}
