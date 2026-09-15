package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrawlBootstrapTest {
    @Test
    fun rootKeywords_coverEveryV1DiscoveryCategory() {
        val keywords = CrawlBootstrap.rootKeywords("ranchi")
        val categories = keywords.map { it.category }.toSet()

        assertTrue(DiscoveryCategory.PLACES in categories)
        assertTrue(DiscoveryCategory.FOOD in categories)
        assertTrue(DiscoveryCategory.FESTIVALS in categories)
        assertTrue(DiscoveryCategory.CULTURE in categories)
        assertTrue(DiscoveryCategory.EMERGENCY in categories)
        assertTrue(DiscoveryCategory.LOCAL_BAZAR in categories)
    }

    @Test
    fun rootKeywords_includeEverydayLocalDiscoveryTerms() {
        val terms = CrawlBootstrap.rootKeywords("ranchi")
            .map { normalizeText(it.term) }

        assertTrue(terms.any { "hospital" in it })
        assertTrue(terms.any { "pharmac" in it })
        assertTrue(terms.any { "railway" in it })
        assertTrue(terms.any { "bank" in it || "atm" in it })
        assertTrue(terms.any { "petrol" in it })
        assertTrue(terms.any { "toilet" in it })
        assertTrue(terms.any { "restaurant" in it })
        assertTrue(terms.any { "haat" in it })
        assertTrue(terms.any { "school" in it })
        assertTrue(terms.any { "college" in it })
        assertTrue(terms.any { "universit" in it })
        assertTrue(terms.any { "traditional food" in it })
        assertTrue(terms.any { "waterfall" in it })
    }

    @Test
    fun commonManPlan_coversAllFoundationDomains() {
        val counts = RanchiCommonManAcquisitionPlan.countsByDomain()

        RanchiCommonManAcquisitionPlan.Domain.entries.forEach { domain ->
            assertTrue(counts.getOrDefault(domain, 0) > 0, "Missing acquisition seeds for $domain")
        }
    }

    @Test
    fun commonManPlan_prioritizesDailyNeedsBeforeLowerPriorityKnowledge() {
        val seeds = RanchiCommonManAcquisitionPlan.seeds
        val firstP2 = seeds.indexOfFirst { it.priority == 2 }
        val lastP0 = seeds.indexOfLast { it.priority == 0 }

        assertTrue(firstP2 > lastP0)
        assertTrue(seeds.take(lastP0 + 1).all { it.priority == 0 })
    }

    @Test
    fun rootKeywords_haveStableUniqueIds() {
        val keywords = CrawlBootstrap.rootKeywords("ranchi")
        assertEquals(keywords.size, keywords.map { it.id }.toSet().size)
    }
}
