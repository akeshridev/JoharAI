package com.akeshridev.johar.domain.crawl

class ScheduleSourceCrawlUseCase(
    private val scheduler: SourceCrawlScheduler,
) {
    operator fun invoke(target: CrawlTarget = CrawlTarget.JHARKHAND) {
        scheduler.enqueue(target)
    }
}
