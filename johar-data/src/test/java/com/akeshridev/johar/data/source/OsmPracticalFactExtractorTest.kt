package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.Freshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmPracticalFactExtractorTest {
    @Test
    fun extractsPracticalTagsFromOverpassElement() {
        val raw = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 123,
                  "lat": 23.35,
                  "lon": 85.33,
                  "tags": {
                    "name": "Example Cafe",
                    "amenity": "cafe",
                    "addr:street": "Main Road",
                    "phone": "+91 12345 67890",
                    "opening_hours": "Mo-Su 08:00-21:00",
                    "cuisine": "indian",
                    "wheelchair": "yes",
                    "source": "survey"
                  }
                }
              ]
            }
        """.trimIndent()

        val facts = OsmPracticalFactExtractor.fromOverpassRaw(
            rawContent = raw,
            publisher = "OpenStreetMap contributors",
            retrievedAtEpochMillis = 1000L,
        )

        assertTrue(facts.any { it.entityId == "osm:node:123" && it.field == "osm.amenity" })
        assertTrue(facts.any { it.field == "osm.addr:street" && it.value == FactValue.Text("Main Road") })
        assertTrue(facts.any { it.field == "osm.phone" })
        assertTrue(facts.any { it.field == "osm.cuisine" })
        assertTrue(facts.any { it.field == "osm.wheelchair" && it.value == FactValue.BooleanValue(true) })
        assertFalse(facts.any { it.field == "osm.source" })
        assertFalse(facts.any { it.field == "osm.name" })
        assertTrue(facts.all { it.sourceUrl == "https://www.openstreetmap.org/node/123" })
    }

    @Test
    fun openingHoursIsMetadataNotLiveTruth() {
        val raw = """
            {
              "elements": [
                {
                  "type": "way",
                  "id": 456,
                  "tags": {
                    "name": "Example Market",
                    "opening_hours": "09:00-18:00"
                  }
                }
              ]
            }
        """.trimIndent()

        val fact = OsmPracticalFactExtractor.fromOverpassRaw(raw, "OpenStreetMap contributors")
            .single()

        assertEquals("osm.opening_hours", fact.field)
        assertEquals(Freshness.SEASONAL, fact.freshness)
    }
}
