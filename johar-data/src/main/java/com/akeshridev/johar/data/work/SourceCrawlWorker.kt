package com.akeshridev.johar.data.work

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.akeshridev.johar.data.crawl.CrawledSourceLogDumper
import com.akeshridev.johar.data.crawl.SourceCrawler
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.parser.HtmlTextCleaner
import com.akeshridev.johar.data.remote.JsoupHtmlSourceFetcher
import com.akeshridev.johar.domain.crawl.CrawlTarget

class SourceCrawlWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val target = inputData.getString(KEY_TARGET)
            ?.let { runCatching { CrawlTarget.valueOf(it) }.getOrNull() }
            ?: return Result.failure()

        return try {
            val dao = JoharDatabaseProvider.get(applicationContext).crawledSourceDao()
            SourceCrawler(
                fetcher = JsoupHtmlSourceFetcher(),
                cleaner = HtmlTextCleaner(),
                dao = dao,
                logDumper = CrawledSourceLogDumper(dao),
            ).crawl(target)
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
