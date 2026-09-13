package com.akeshridev.johar.data.remote

import org.jsoup.Connection
import org.jsoup.Jsoup

internal class JsoupHttpTextFetcher(
    private val userAgent: String = DEFAULT_USER_AGENT,
) : HttpTextFetcher {

    override fun get(
        url: String,
        headers: Map<String, String>,
    ): String = execute(
        Jsoup.connect(url)
            .method(Connection.Method.GET)
            .headers(headers),
    )

    override fun postForm(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String>,
    ): String = execute(
        Jsoup.connect(url)
            .method(Connection.Method.POST)
            .data(form)
            .headers(headers),
    )

    private fun execute(connection: Connection): String {
        val response = connection
            .userAgent(userAgent)
            .ignoreContentType(true)
            .ignoreHttpErrors(false)
            .timeout(TIMEOUT_MILLIS)
            .maxBodySize(MAX_BODY_BYTES)
            .execute()

        check(response.statusCode() in 200..299) {
            "HTTP ${response.statusCode()} from ${response.url()}"
        }
        return response.body()
    }

    companion object {
        private const val DEFAULT_USER_AGENT =
            "JoharAI/0.1 (+https://github.com/akeshridev/JoharAI)"
        private const val TIMEOUT_MILLIS = 25_000
        private const val MAX_BODY_BYTES = 8 * 1024 * 1024
    }
}
