package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlSeed

/**
 * Fetches practical OSM metadata for already-known Ranchi entities in one Overpass request.
 *
 * Backfill candidates already have stable OSM refs, so there is no reason to resolve them by
 * name or run nearby discovery again. This keeps the backfill cheap and deterministic.
 */
internal class RanchiOsmBulkBackfill(
    private val fetcher: HttpTextFetcher,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    fun fetch(seeds: List<CrawlSeed>): Result {
        val refs = seeds.mapNotNull { seed ->
            seed.externalRefs[OSM_REF]
                ?.takeIf(::isValidRef)
        }.distinct()

        if (refs.isEmpty()) return Result(rawContent = "", facts = emptyList())

        val query = buildQuery(refs)
        val raw = fetcher.postForm(endpoint, mapOf("data" to query))
        return Result(
            rawContent = raw,
            facts = OsmPracticalFactExtractor.fromOverpassRaw(
                rawContent = raw,
                publisher = PUBLISHER,
            ),
        )
    }

    internal fun buildQuery(refs: List<String>): String {
        val idsByType = refs
            .filter(::isValidRef)
            .distinct()
            .map { ref ->
                val (type, id) = ref.split(':', limit = 2)
                type to id
            }
            .groupBy({ it.first }, { it.second })

        val clauses = OSM_TYPES.mapNotNull { type ->
            val ids = idsByType[type].orEmpty()
            if (ids.isEmpty()) null else "  $type(id:${ids.joinToString(",")});"
        }.joinToString("\n")

        require(clauses.isNotBlank()) { "No valid OSM refs for bulk backfill" }
        return """
            [out:json][timeout:25];
            (
            $clauses
            );
            out center tags;
        """.trimIndent()
    }

    private fun isValidRef(ref: String): Boolean {
        val parts = ref.split(':', limit = 2)
        return parts.size == 2 &&
            parts[0] in OSM_TYPES &&
            parts[1].toLongOrNull()?.let { it > 0L } == true
    }

    data class Result(
        val rawContent: String,
        val facts: List<com.akeshridev.johar.domain.source.SourceFact>,
    )

    companion object {
        private const val OSM_REF = "osm"
        private const val PUBLISHER = "OpenStreetMap contributors"
        private const val DEFAULT_ENDPOINT = "https://overpass-api.de/api/interpreter"
        private val OSM_TYPES = listOf("node", "way", "relation")
    }
}
