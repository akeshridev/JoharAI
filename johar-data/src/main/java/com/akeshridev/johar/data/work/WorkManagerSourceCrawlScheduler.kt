package com.akeshridev.johar.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.akeshridev.johar.domain.crawl.CrawlTarget
import com.akeshridev.johar.domain.crawl.SourceCrawlScheduler

class WorkManagerSourceCrawlScheduler(
    context: Context,
) : SourceCrawlScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueue(target: CrawlTarget) {
        val request = OneTimeWorkRequest.Builder(SourceCrawlWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(
                Data.Builder()
                    .putString(SourceCrawlWorker.KEY_TARGET, target.name)
                    .build(),
            )
            .build()

        workManager.enqueueUniqueWork(
            "johar-source-crawl-${target.name}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
