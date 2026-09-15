package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow
import kotlin.test.Test
import kotlin.test.assertEquals

class FoundationCoverageReporterTest {
    @Test
    fun domainFor_usesPackSubtypeForCommonManCategories() {
        assertEquals(FoundationDomain.EDUCATION, FoundationCoverageReporter.domainFor(entity("school", "FACILITY", "SCHOOL")))
        assertEquals(FoundationDomain.DAILY_LIFE, FoundationCoverageReporter.domainFor(entity("atm", "FACILITY", "ATM")))
        assertEquals(FoundationDomain.HEALTH_EMERGENCY, FoundationCoverageReporter.domainFor(entity("pharmacy", "SHOP", "PHARMACY")))
        assertEquals(FoundationDomain.TRANSPORT_MOBILITY, FoundationCoverageReporter.domainFor(entity("station", "RAILWAY_STATION", "RAILWAY_STATION")))
        assertEquals(FoundationDomain.FOOD_MARKETS, FoundationCoverageReporter.domainFor(entity("cafe", "RESTAURANT", "CAFE")))
    }

    @Test
    fun build_reportsCoordinatesAliasesFactsAndEvidenceCompleteness() {
        val hospital = entity("rims", "HOSPITAL", "HOSPITAL", coordinates = true, aliases = true)
        val school = entity("school", "FACILITY", "SCHOOL", coordinates = false, aliases = false)
        val facts = listOf(
            fact("fact:1", hospital.id, evidence = "Official address", publisher = "RIMS"),
            fact("fact:2", hospital.id, evidence = "", publisher = "RIMS"),
        )

        val report = FoundationCoverageReporter.build(listOf(hospital, school), facts)
        val health = report.domains.first { it.domain == FoundationDomain.HEALTH_EMERGENCY }
        val education = report.domains.first { it.domain == FoundationDomain.EDUCATION }

        assertEquals(1, health.entities)
        assertEquals(1, health.withCoordinates)
        assertEquals(1, health.withAliases)
        assertEquals(1, health.withFacts)
        assertEquals(2, health.facts)
        assertEquals(1, health.evidenceCompleteFacts)

        assertEquals(1, education.entities)
        assertEquals(0, education.withCoordinates)
        assertEquals(0, education.withFacts)
    }

    private fun entity(
        id: String,
        type: String,
        packType: String,
        coordinates: Boolean = false,
        aliases: Boolean = false,
    ) = KnowledgeEntityRow(
        id = id,
        name = id,
        normalizedName = id,
        type = type,
        description = null,
        latitude = if (coordinates) 23.36 else null,
        longitude = if (coordinates) 85.33 else null,
        region = "Jharkhand",
        country = "India",
        aliasesJson = if (aliases) "[\"alias\"]" else "[]",
        externalRefsJson = "{\"joharPackType\":\"$packType\"}",
        discoveredAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        lastCrawledAtEpochMillis = null,
        discoveryDepth = 1,
        enabled = true,
    )

    private fun fact(
        id: String,
        entityId: String,
        evidence: String,
        publisher: String,
    ) = SourceFactRow(
        id = id,
        entityId = entityId,
        sourceUrl = "https://example.org/$id",
        publisher = publisher,
        retrievedAtEpochMillis = 1L,
        domain = "TRAVEL_LOGISTICS",
        field = "address",
        valueType = "TEXT",
        textValue = "Ranchi",
        numberValue = null,
        unit = null,
        booleanValue = null,
        evidenceText = evidence,
        factType = "OBSERVED",
        state = "ACTIVE",
        freshness = "STATIC_OR_SLOW",
    )
}
