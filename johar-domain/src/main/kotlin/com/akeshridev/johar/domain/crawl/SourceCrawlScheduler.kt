package com.akeshridev.johar.domain.crawl

interface SourceCrawlScheduler {
    fun enqueue(target: CrawlTarget)
}
