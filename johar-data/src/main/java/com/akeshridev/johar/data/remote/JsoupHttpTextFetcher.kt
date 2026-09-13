package com.akeshridev.johar.data.remote

import android.util.Log
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.concurrent.atomic.AtomicLong

internal class JsoupHttpTextFetcher(
    private val userAgent: String = DEFAULT_USER_AGENT,
) : HttpTextFetcher {

    override fun get(
        url: String,
        headers: Map<String, String>,
    ): String {
        val requestId = nextRequestId()
        logRequest(
            requestId = requestId,
            method = "GET",
            url = url,
            headers = headers,
            body = null,
        )
        return execute(
            requestId = requestId,
            connection = Jsoup.connect(url)
                .method(Connection.Method.GET)
                .headers(headers),
        )
    }

    override fun postForm(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String>,
    ): String {
        val requestId = nextRequestId()
        logRequest(
            requestId = requestId,
            method = "POST",
            url = url,
            headers = headers,
            body = form.entries.joinToString("&") { (key, value) -> "$key=$value" },
        )
        return execute(
            requestId = requestId,
            connection = Jsoup.connect(url)
                .method(Connection.Method.POST)
                .data(form)
                .headers(headers),
        )
    }

    private fun execute(
        requestId: Long,
        connection: Connection,
    ): String {
        val startedAt = System.currentTimeMillis()
        return try {
            val response = connection
                .userAgent(userAgent)
                .ignoreContentType(true)
                .ignoreHttpErrors(false)
                .timeout(TIMEOUT_MILLIS)
                .maxBodySize(MAX_BODY_BYTES)
                .execute()

            val body = response.body()
            val elapsed = System.currentTimeMillis() - startedAt
            logResponse(
                requestId = requestId,
                statusCode = response.statusCode(),
                url = response.url().toString(),
                elapsedMillis = elapsed,
                body = body,
            )

            check(response.statusCode() in 200..299) {
                "HTTP ${response.statusCode()} from ${response.url()}"
            }
            body
        } catch (error: Throwable) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(
                TAG,
                "HTTP_FAIL id=$requestId elapsedMs=$elapsed type=${error::class.java.simpleName} message=${error.message}",
                error,
            )
            throw error
        }
    }

    private fun logRequest(
        requestId: Long,
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ) {
        Log.i(
            TAG,
            "HTTP_REQ id=$requestId method=$method url=$url headers=${sanitizeHeaders(headers)} bodyChars=${body?.length ?: 0}",
        )
        body?.takeIf(String::isNotBlank)?.let {
            logChunked("HTTP_REQ_BODY id=$requestId", it)
        }
    }

    private fun logResponse(
        requestId: Long,
        statusCode: Int,
        url: String,
        elapsedMillis: Long,
        body: String,
    ) {
        Log.i(
            TAG,
            "HTTP_RESP id=$requestId status=$statusCode elapsedMs=$elapsedMillis url=$url bodyChars=${body.length}",
        )
        logChunked("HTTP_RESP_BODY id=$requestId", body)
    }

    private fun logChunked(prefix: String, value: String) {
        val trace = if (value.length <= MAX_TRACE_CHARS) {
            value
        } else {
            value.take(MAX_TRACE_CHARS) +
                "\n... [TRACE TRUNCATED ${value.length - MAX_TRACE_CHARS} chars; full response still returned to crawler]"
        }

        if (trace.isEmpty()) {
            Log.d(TAG, "$prefix chunk=1/1 <empty>")
            return
        }

        val totalChunks = (trace.length + LOG_CHUNK_CHARS - 1) / LOG_CHUNK_CHARS
        trace.chunked(LOG_CHUNK_CHARS).forEachIndexed { index, chunk ->
            Log.d(TAG, "$prefix chunk=${index + 1}/$totalChunks $chunk")
        }
    }

    private fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> = headers.mapValues { (key, value) ->
        if (key.equals("Authorization", ignoreCase = true) ||
            key.equals("Cookie", ignoreCase = true) ||
            key.equals("Set-Cookie", ignoreCase = true)
        ) {
            "<redacted>"
        } else {
            value
        }
    }

    private fun nextRequestId(): Long = REQUEST_COUNTER.incrementAndGet()

    companion object {
        private const val TAG = "JoharHttp"
        private const val DEFAULT_USER_AGENT =
            "JoharAI/0.1 (+https://github.com/akeshridev/JoharAI)"
        private const val TIMEOUT_MILLIS = 25_000
        private const val MAX_BODY_BYTES = 8 * 1024 * 1024
        private const val LOG_CHUNK_CHARS = 3_000
        private const val MAX_TRACE_CHARS = 200_000
        private val REQUEST_COUNTER = AtomicLong(0)
    }
}
