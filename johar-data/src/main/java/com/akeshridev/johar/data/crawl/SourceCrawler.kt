package com.akeshridev.johar.data.crawl

import com.akeshridev.johar.data.local.CrawledSourceDao
import com.akeshridev.johar.data.local.CrawledSourceEntity
import com.akeshridev.johar.data.parser.HtmlTextCleaner
import com.akeshridev.johar.data.remote.HtmlSourceFetcher
import com.akeshridev.johar.data.source.SourceCatalog
import com.akeshridev.johar.domain.crawl.CrawlTarget

internal class SourceCrawler(
    private val fetcher: HtmlSourceFetcher,
    private val cleaner: HtmlTextCleaner,
    private val dao: CrawledSourceDao,
    private val logDumper: CrawledSourceLogDumper,
) {
    fun crawl(target: CrawlTarget) {
        val source = SourceCatalog.resolve(target)
        val html = fetcher.fetch(source.url)
        val cleanText = cleaner.clean(html)
        check(cleanText.isNotBlank()) { "No content extracted from ${source.url}" }

        dao.upsert(
            CrawledSourceEntity(
                sourceUrl = source.url,
                entityId = source.entityId,
                publisher = source.publisher,
                fetchedAtEpochMillis = System.currentTimeMillis(),
                content = cleanText,
            ),
        )

        logDumper.dumpAll()
    }
}
