package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.crawl.CrawlSeed

internal class TracingSourceAdapter(
    private val delegate: CrawlSourceAdapter,
) : CrawlSourceAdapter {
    override val id: String = delegate.id

    override fun supports(seed: CrawlSeed): Boolean = delegate.supports(seed)

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val subject = seed.name
        TraceLog.start("entity", id, subject)
        return try {
            delegate.crawl(seed).also { result ->
                val detail = if (result == null) {
                    "result=empty"
                } else {
                    "facts=${result.facts.size} relationships=${result.relationships.size} media=${result.media.size} discovered=${result.discoveredEntities.size}"
                }
                TraceLog.success("entity", id, subject, detail)
            }
        } catch (error: Throwable) {
            TraceLog.failure("entity", id, subject, error)
            throw error
        }
    }
}
