package com.akeshridev.johar.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.akeshridev.johar.domain.crawl.CrawlTarget
import com.akeshridev.johar.domain.crawl.SourceCrawlScheduler
import java.util.concurrent.TimeUnit

class WorkManagerSourceCrawlScheduler(
    context: Context,
) : SourceCrawlScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueue(target: CrawlTarget) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val immediateRequest = OneTimeWorkRequest.Builder(SourceCrawlWorker::class.java)
            .setConstraints(constraints)
            .setInputData(inputData(target))
            .build()

        // Do not cancel a crawl already in progress when the developer/user taps again.
        // The crawler persists its queue in Room, so the next periodic/manual run can resume.
        workManager.enqueueUniqueWork(
            "johar-source-crawl-${target.name}",
            ExistingWorkPolicy.KEEP,
            immediateRequest,
        )

        val periodicRequest = PeriodicWorkRequest.Builder(
            SourceCrawlWorker::class.java,
            REFRESH_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setInputData(inputData(CrawlTarget.JHARKHAND))
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }

    private fun inputData(target: CrawlTarget): Data = Data.Builder()
        .putString(SourceCrawlWorker.KEY_TARGET, target.name)
        .build()

    companion object {
        private const val PERIODIC_WORK_NAME = "johar-knowledge-refresh"
        private const val REFRESH_INTERVAL_HOURS = 24L
    }
}
