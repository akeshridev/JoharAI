package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RanchiEducationDiscoveryAdapterTest {
    @Test
    fun schoolsSeed_usesExplicitSchoolSelectorAndPreservesSchoolPackType() {
        val fetcher = RecordingFetcher(
            response = """
                {"elements":[{"type":"node","id":42,"lat":23.36,"lon":85.33,"tags":{"name":"Example School","amenity":"school","addr:city":"Ranchi"}}]}
            """.trimIndent(),
        )
        val adapter = RanchiEducationDiscoveryAdapter(fetcher, endpoint = "https://example.test/overpass")
        val keyword = keyword("schools in Ranchi")

        assertTrue(adapter.supports(keyword))
        val result = adapter.discover(keyword)

        assertTrue(fetcher.lastForm.orEmpty()["data"].orEmpty().contains("[\"amenity\"=\"school\"]"))
        assertEquals(1, result.entities.size)
        assertEquals("Example School", result.entities.single().name)
        assertEquals("SCHOOL", result.entities.single().externalRefs["joharPackType"])
        assertEquals(23.36, result.entities.single().latitude)
        assertEquals(85.33, result.entities.single().longitude)
    }

    @Test
    fun unrelatedPlaceSeed_isRejected() {
        val adapter = RanchiEducationDiscoveryAdapter(RecordingFetcher("{\"elements\":[]}"))

        assertFalse(adapter.supports(keyword("parks in Ranchi")))
        assertFalse(adapter.supports(keyword("schools in Jharkhand")))
    }

    private fun keyword(term: String) = CrawlKeyword(
        id = "test",
        entityId = "ranchi",
        category = DiscoveryCategory.PLACES,
        term = term,
        discoveredFrom = "bootstrap",
    )

    private class RecordingFetcher(private val response: String) : HttpTextFetcher {
        var lastForm: Map<String, String>? = null

        override fun get(url: String, headers: Map<String, String>): String = response

        override fun postForm(
            url: String,
            form: Map<String, String>,
            headers: Map<String, String>,
        ): String {
            lastForm = form
            return response
        }
    }
}
