package com.akeshridev.johar.di

import android.content.Context
import com.akeshridev.johar.data.work.WorkManagerSourceCrawlScheduler
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import com.akeshridev.johar.domain.crawl.SourceCrawlScheduler

class JoharGraph(context: Context) {
    private val scheduler: SourceCrawlScheduler =
        WorkManagerSourceCrawlScheduler(context.applicationContext)

    val scheduleSourceCrawlUseCase = ScheduleSourceCrawlUseCase(scheduler)
}
