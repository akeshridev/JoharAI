package com.akeshridev.johar.di

import android.content.Context
import com.akeshridev.johar.data.pack.KnowledgePackLoader
import com.akeshridev.johar.data.work.WorkManagerSourceCrawlScheduler
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import com.akeshridev.johar.domain.crawl.SourceCrawlScheduler

class JoharGraph(context: Context) {
    private val appContext = context.applicationContext
    private val scheduler: SourceCrawlScheduler = WorkManagerSourceCrawlScheduler(appContext)
    private val knowledgePackLoader = KnowledgePackLoader(appContext)

    val scheduleSourceCrawlUseCase = ScheduleSourceCrawlUseCase(scheduler)

    fun loadKnowledgePack() = knowledgePackLoader.load()
}
