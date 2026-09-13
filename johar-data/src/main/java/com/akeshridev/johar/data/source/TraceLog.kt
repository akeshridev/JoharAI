package com.akeshridev.johar.data.source

import android.util.Log

internal object TraceLog {
    fun start(kind: String, adapter: String, subject: String) {
        Log.i("JoharCrawl", "STEP ${kind}_start adapter=$adapter subject=$subject")
    }

    fun success(kind: String, adapter: String, subject: String, detail: String) {
        Log.i("JoharCrawl", "STEP ${kind}_success adapter=$adapter subject=$subject $detail")
    }

    fun failure(kind: String, adapter: String, subject: String, error: Throwable) {
        Log.w("JoharCrawl", "STEP ${kind}_failed adapter=$adapter subject=$subject", error)
    }
}
