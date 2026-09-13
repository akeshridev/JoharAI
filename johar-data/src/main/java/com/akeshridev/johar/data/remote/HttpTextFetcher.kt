package com.akeshridev.johar.data.remote

internal interface HttpTextFetcher {
    fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): String

    fun postForm(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): String
}
