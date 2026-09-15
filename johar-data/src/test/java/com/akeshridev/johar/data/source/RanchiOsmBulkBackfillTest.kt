package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.remote.HttpTextFetcher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RanchiOsmBulkBackfillTest {
    @Test
    fun buildQuery_groupsKnownRefsByOsmType() {
        val backfill = RanchiOsmBulkBackfill(NoopFetcher())

        val query = backfill.buildQuery(
            listOf(
                "node:101",
                "node:102",
                "way:201",
                "relation:301",
                "bad:999",
                "node:not-a-number",
                "node:101",
            ),
        )

        assertTrue(query.contains("node(id:101,102);"))
        assertTrue(query.contains("way(id:201);"))
        assertTrue(query.contains("relation(id:301);"))
        assertFalse(query.contains("bad"))
        assertFalse(query.contains("not-a-number"))
    }

    private class NoopFetcher : HttpTextFetcher {
        override fun get(url: String, headers: Map<String, String>): String = ""

        override fun postForm(
            url: String,
            form: Map<String, String>,
            headers: Map<String, String>,
        ): String = ""
    }
}
