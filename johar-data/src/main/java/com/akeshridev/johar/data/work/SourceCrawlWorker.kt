package com.akeshridev.johar.data.work

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.akeshridev.johar.data.crawl.KnowledgeStore
import com.akeshridev.johar.data.crawl.SourceCrawler
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.remote.JsoupHttpTextFetcher
import com.akeshridev.johar.data.source.CommonsMediaSourceAdapter
import com.akeshridev.johar.data.source.MediaWikiTextSourceAdapter
import com.akeshridev.johar.data.source.OpenMeteoSourceAdapter
import com.akeshridev.johar.data.source.OverpassSourceAdapter
import com.akeshridev.johar.data.source.OverpassSpecializedDiscoveryAdapter
import com.akeshridev.johar.data.source.RanchiDistrictOfficialSourceAdapter
import com.akeshridev.johar.data.source.TracingDiscoveryAdapter
import com.akeshridev.johar.data.source.TracingSourceAdapter
import com.akeshridev.johar.data.source.WikidataSourceAdapter
import com.akeshridev.johar.domain.crawl.CrawlTarget
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.source.KnowledgeDomain

class SourceCrawlWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val target = inputData.getString(KEY_TARGET)
            ?.let { runCatching { CrawlTarget.valueOf(it) }.getOrNull() }
            ?: return Result.failure()

        return try {
            val database = JoharDatabaseProvider.get(applicationContext)
            val fetcher = JsoupHttpTextFetcher()
            val wikidata = WikidataSourceAdapter(fetcher)
            val overpass = OverpassSourceAdapter(fetcher)
            val specializedOverpass = OverpassSpecializedDiscoveryAdapter(fetcher)
            val ranchiOfficial = RanchiDistrictOfficialSourceAdapter(fetcher)
            val wikipedia = MediaWikiTextSourceAdapter(
                id = "wikipedia",
                host = "en.wikipedia.org",
                publisher = "Wikipedia",
                defaultDomain = KnowledgeDomain.HISTORY_CULTURE,
                referenceKey = "wikipedia_pageid",
                discoveryCategories = setOf(
                    DiscoveryCategory.PLACES,
                    DiscoveryCategory.FOOD,
                    DiscoveryCategory.FESTIVALS,
                    DiscoveryCategory.CULTURE,
                    DiscoveryCategory.LOCAL_BAZAR,
                ),
                fetcher = fetcher,
            )
            val wikivoyage = MediaWikiTextSourceAdapter(
                id = "wikivoyage",
                host = "en.wikivoyage.org",
                publisher = "Wikivoyage",
                defaultDomain = KnowledgeDomain.TRAVEL_LOGISTICS,
                referenceKey = "wikivoyage_pageid",
                discoveryCategories = setOf(
                    DiscoveryCategory.PLACES,
                    DiscoveryCategory.FOOD,
                ),
                fetcher = fetcher,
            )
            val commons = CommonsMediaSourceAdapter(fetcher)
            val weather = OpenMeteoSourceAdapter(fetcher)
            val store = KnowledgeStore(
                knowledgeDao = database.knowledgeDao(),
                sourceDao = database.crawledSourceDao(),
            )

            val entityAdapters = buildList {
                if (target == CrawlTarget.RANCHI) add(ranchiOfficial)
                add(wikidata)
                if (target != CrawlTarget.JHARKHAND) add(overpass)
                add(wikipedia)
                add(wikivoyage)
                add(commons)
                add(weather)
            }.map(::TracingSourceAdapter)

            val discoveryAdapters = buildList {
                add(specializedOverpass)
                // Ranchi has an explicit intent-to-OSM mapper. Running the older generic OSM
                // discovery beside it reintroduces statewide/generic tourism noise.
                if (target != CrawlTarget.RANCHI) add(overpass)
                add(wikidata)
                add(wikipedia)
                add(wikivoyage)
            }.map(::TracingDiscoveryAdapter)

            val stats = SourceCrawler(
                store = store,
                sourceAdapters = entityAdapters,
                discoveryAdapters = discoveryAdapters,
            ).crawl(target)

            CrawlSummaryLogger.print(target, stats, database.knowledgeDao())
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Crawl failed for $target on attempt $runAttemptCount", error)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_TARGET = "crawl_target"
        private const val TAG = "JoharCrawl"
        private const val MAX_RETRIES = 2
    }
}
