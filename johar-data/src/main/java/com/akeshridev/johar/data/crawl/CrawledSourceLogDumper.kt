package com.akeshridev.johar.data.crawl

import android.util.Log
import com.akeshridev.johar.data.local.CrawledSourceDao

internal class CrawledSourceLogDumper(
    private val dao: CrawledSourceDao,
) {
    fun dumpAll() {
        val sources = dao.getAll()
        Log.d(TAG, "stored_sources=${sources.size}")

        sources.forEach { source ->
            Log.d(
                TAG,
                "source=${source.sourceUrl} publisher=${source.publisher} " +
                    "fetchedAt=${source.fetchedAtEpochMillis} chars=${source.content.length}",
            )

            source.content.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
                Log.d(TAG, "content[$index]=$chunk")
            }
        }
    }

    private companion object {
        const val TAG = "JoharCrawl"
        const val LOG_CHUNK_SIZE = 3_500
    }
}
