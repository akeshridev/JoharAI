package com.akeshridev.johar.data.remote

internal fun interface HtmlSourceFetcher {
    fun fetch(url: String): String
}
