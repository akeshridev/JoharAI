package com.akeshridev.johar.domain.crawl

class ScheduleSourceCrawlUseCase(
    private val scheduler: SourceCrawlScheduler,
) {
    operator fun invoke(target: CrawlTarget = CrawlTarget.DASSAM_FALLS) {
        scheduler.enqueue(target)
    }
}
