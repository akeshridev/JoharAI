package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrawlBootstrapTest {
    @Test
    fun rootKeywords_coverEveryV1DiscoveryCategory() {
        val keywords = CrawlBootstrap.rootKeywords("jharkhand")
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
        val terms = CrawlBootstrap.rootKeywords("jharkhand")
            .map { normalizeText(it.term) }

        assertTrue(terms.any { "haat" in it })
        assertTrue(terms.any { "pork" in it })
        assertTrue(terms.any { "hospital" in it })
        assertTrue(terms.any { "festival" in it })
        assertTrue(terms.any { "traditional food" in it })
        assertTrue(terms.any { "waterfall" in it })
    }

    @Test
    fun rootKeywords_haveStableUniqueIds() {
        val keywords = CrawlBootstrap.rootKeywords("jharkhand")
        assertEquals(keywords.size, keywords.map { it.id }.toSet().size)
    }
}
