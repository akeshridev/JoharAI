package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.crawl.CrawlKeyword

internal class TracingDiscoveryAdapter(
    private val delegate: KeywordDiscoveryAdapter,
) : KeywordDiscoveryAdapter {
    override val id: String = delegate.id

    override fun supports(keyword: CrawlKeyword): Boolean = delegate.supports(keyword)

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val subject = keyword.term
        TraceLog.start("keyword", id, subject)
        return try {
            delegate.discover(keyword).also { result ->
                TraceLog.success(
                    "keyword",
                    id,
                    subject,
                    "entities=${result.entities.size} keywords=${result.keywords.size}",
                )
            }
        } catch (error: Throwable) {
            TraceLog.failure("keyword", id, subject, error)
            throw error
        }
    }
}
