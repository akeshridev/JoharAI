package com.akeshridev.johar.data.work

import android.util.Log
import com.akeshridev.johar.data.crawl.CrawlStats
import com.akeshridev.johar.data.local.KnowledgeDao
import com.akeshridev.johar.domain.crawl.CrawlTarget

internal object CrawlSummaryLogger {
    fun print(target: CrawlTarget, stats: CrawlStats, dao: KnowledgeDao) {
        Log.i("JoharCrawl", "*** JOHAR CRAWL SUCCESS ***")
        Log.i("JoharCrawl", "*** Target = $target ***")
        Log.i("JoharCrawl", "*** Total Entity fetched = ${stats.entities} ***")
        Log.i("JoharCrawl", "*** Total Facts saved = ${stats.facts} ***")
        Log.i("JoharCrawl", "*** Total Relationships saved = ${stats.relationships} ***")
        Log.i("JoharCrawl", "*** Total Media saved = ${stats.media} ***")
        Log.i("JoharCrawl", "*** Total Categories = ${stats.entityTypes.size} ***")
        stats.entityTypes.forEach { (type, count) ->
            Log.i("JoharCrawl", "*** CATEGORY $type = $count ***")
        }
        val rows = dao.allEnabledEntities()
        rows.forEachIndexed { index, row ->
            Log.i("JoharCrawl", "*** ROW #${index + 1} name=${row.name} type=${row.type} id=${row.id} depth=${row.discoveryDepth} lat=${row.latitude} lon=${row.longitude} crawled=${row.lastCrawledAtEpochMillis} ***")
        }
        Log.i("JoharCrawl", "*** END JOHAR CRAWL SUCCESS ***")
    }
}
