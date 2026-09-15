package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalEntityResolverTest {
    @Test
    fun sharedExternalIdWinsEvenWhenNamesDiffer() {
        val candidate = row(
            id = "canonical:ranchi-station",
            name = "Ranchi Junction",
            type = EntityType.RAILWAY_STATION,
            refsJson = "{\"osm\":\"node:123\"}",
        )
        val discovered = entity(
            id = "osm:node:123",
            name = "Ranchi Railway Station",
            type = EntityType.RAILWAY_STATION,
            refs = mapOf("osm" to "node:123"),
        )

        assertEquals(candidate.id, CanonicalEntityResolver.resolve(discovered, listOf(candidate))?.id)
    }

    @Test
    fun nearbyTypedNameVariantResolvesToCanonicalEntity() {
        val candidate = row(
            id = "canonical:ranchi-station",
            name = "Ranchi Junction",
            type = EntityType.RAILWAY_STATION,
            latitude = 23.3495,
            longitude = 85.3356,
        )
        val discovered = entity(
            id = "wikidata:station",
            name = "Ranchi Railway Station",
            type = EntityType.RAILWAY_STATION,
            latitude = 23.3497,
            longitude = 85.3358,
        )

        assertEquals(candidate.id, CanonicalEntityResolver.resolve(discovered, listOf(candidate))?.id)
    }

    @Test
    fun aliasCanResolveSameEntity() {
        val candidate = row(
            id = "canonical:hatia",
            name = "Hatia Railway Station",
            type = EntityType.RAILWAY_STATION,
            aliasesJson = "[\"Hatia Station\"]",
        )
        val discovered = entity(
            id = "source:hatia",
            name = "Hatia Station",
            type = EntityType.RAILWAY_STATION,
        )

        assertEquals(candidate.id, CanonicalEntityResolver.resolve(discovered, listOf(candidate))?.id)
    }

    @Test
    fun similarNameFarAwayDoesNotMerge() {
        val candidate = row(
            id = "canonical:central-school-one",
            name = "Central School",
            type = EntityType.FACILITY,
            latitude = 23.35,
            longitude = 85.33,
        )
        val discovered = entity(
            id = "source:central-school-two",
            name = "Central School",
            type = EntityType.FACILITY,
            latitude = 23.45,
            longitude = 85.45,
        )

        assertNull(CanonicalEntityResolver.resolve(discovered, listOf(candidate)))
    }

    @Test
    fun incompatibleTypeWithoutExactIdentityDoesNotMerge() {
        val candidate = row(
            id = "canonical:ranchi-city",
            name = "Ranchi",
            type = EntityType.CITY,
            latitude = 23.3441,
            longitude = 85.3096,
        )
        val discovered = entity(
            id = "source:ranchi-station",
            name = "Ranchi Railway Station",
            type = EntityType.RAILWAY_STATION,
            latitude = 23.3495,
            longitude = 85.3356,
        )

        assertNull(CanonicalEntityResolver.resolve(discovered, listOf(candidate)))
    }

    private fun entity(
        id: String,
        name: String,
        type: EntityType,
        latitude: Double? = null,
        longitude: Double? = null,
        refs: Map<String, String> = emptyMap(),
    ) = KnowledgeEntity(
        id = id,
        name = name,
        type = type,
        latitude = latitude,
        longitude = longitude,
        region = "Jharkhand",
        country = "India",
        externalRefs = refs,
    )

    private fun row(
        id: String,
        name: String,
        type: EntityType,
        latitude: Double? = null,
        longitude: Double? = null,
        aliasesJson: String = "[]",
        refsJson: String = "{}",
    ) = KnowledgeEntityRow(
        id = id,
        name = name,
        normalizedName = normalizeText(name),
        type = type.name,
        description = null,
        latitude = latitude,
        longitude = longitude,
        region = "Jharkhand",
        country = "India",
        aliasesJson = aliasesJson,
        externalRefsJson = refsJson,
        discoveredAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        lastCrawledAtEpochMillis = null,
        discoveryDepth = 0,
        enabled = true,
    )
}
