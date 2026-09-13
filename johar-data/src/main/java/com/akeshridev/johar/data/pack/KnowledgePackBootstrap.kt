package com.akeshridev.johar.data.pack

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object KnowledgePackBootstrap {
    private const val UNIQUE_WORK_NAME = "johar-knowledge-pack-bootstrap"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<KnowledgePackImportWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
