package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.crawl.CrawlKeyword

/** Adds practical OSM tag facts to an existing Overpass-backed discovery adapter. */
internal class OsmPracticalFactEnrichingDiscoveryAdapter(
    private val delegate: KeywordDiscoveryAdapter,
) : KeywordDiscoveryAdapter {
    override val id: String = "${delegate.id}_practical_facts"

    override fun supports(keyword: CrawlKeyword): Boolean = delegate.supports(keyword)

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val result = delegate.discover(keyword)
        val facts = OsmPracticalFactExtractor.fromOverpassRaw(
            rawContent = result.rawContent,
            publisher = result.publisher,
        )
        return result.copy(facts = result.facts + facts)
    }
}
