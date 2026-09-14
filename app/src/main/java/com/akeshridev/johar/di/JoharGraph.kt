package com.akeshridev.johar.di

import android.content.Context
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import com.akeshridev.johar.data.spatial.RanchiSpatialEngine
import com.akeshridev.johar.data.work.WorkManagerSourceCrawlScheduler
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import com.akeshridev.johar.domain.crawl.SourceCrawlScheduler

class JoharGraph(context: Context) {
    private val appContext = context.applicationContext
    private val scheduler: SourceCrawlScheduler = WorkManagerSourceCrawlScheduler(appContext)

    val offlineKnowledgeRetriever = OfflineKnowledgeRetriever(appContext)
    val ranchiSpatialEngine = RanchiSpatialEngine(appContext)
    val scheduleSourceCrawlUseCase = ScheduleSourceCrawlUseCase(scheduler)
}
