package com.akeshridev.johar.di

import android.content.Context
import com.akeshridev.johar.conversation.JoharQueryRouter
import com.akeshridev.johar.data.retrieval.DeterministicJoharAnswerGenerator
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import com.akeshridev.johar.data.routing.RanchiOfflineRouter
import com.akeshridev.johar.data.spatial.RanchiSpatialEngine
import com.akeshridev.johar.data.work.WorkManagerSourceCrawlScheduler
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import com.akeshridev.johar.domain.crawl.SourceCrawlScheduler

class JoharGraph(context: Context) {
    private val appContext = context.applicationContext
    private val scheduler: SourceCrawlScheduler = WorkManagerSourceCrawlScheduler(appContext)

    val offlineKnowledgeRetriever by lazy { OfflineKnowledgeRetriever(appContext) }
    val ranchiSpatialEngine by lazy { RanchiSpatialEngine(appContext) }
    val ranchiOfflineRouter by lazy { RanchiOfflineRouter(appContext) }

    fun conversationRouter(): JoharQueryRouter {
        val answerGenerator = DeterministicJoharAnswerGenerator(offlineKnowledgeRetriever)
        return JoharQueryRouter(
            resolvePlace = { ranchiSpatialEngine.resolvePlace(it, limit = 20) },
            nearby = { origin, types -> ranchiSpatialEngine.nearby(origin, radiusKm = 5.0, types = types, limit = 100) },
            knowledgeAnswer = { answerGenerator.answer(it) },
            routeInstalled = { ranchiOfflineRouter.isInstalled() },
            route = { origin, destination -> ranchiOfflineRouter.route(origin, destination) },
        )
    }

    val scheduleSourceCrawlUseCase = ScheduleSourceCrawlUseCase(scheduler)
}
