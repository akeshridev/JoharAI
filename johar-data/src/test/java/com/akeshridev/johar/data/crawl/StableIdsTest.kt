package com.akeshridev.johar.data.crawl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StableIdsTest {
    @Test
    fun normalizeText_collapsesCasePunctuationAndSpacing() {
        assertEquals("dassam falls", normalizeText("  Dassam-Falls!!  "))
        assertEquals("rugra", normalizeText("RUGRA"))
    }

    @Test
    fun stableId_isDeterministicAndSensitiveToParts() {
        val first = stableId("entity", "dassam falls", "Jharkhand", "India")
        val second = stableId("entity", "dassam falls", "Jharkhand", "India")
        val different = stableId("entity", "rugra", "Jharkhand", "India")

        assertEquals(first, second)
        assertNotEquals(first, different)
    }
}
