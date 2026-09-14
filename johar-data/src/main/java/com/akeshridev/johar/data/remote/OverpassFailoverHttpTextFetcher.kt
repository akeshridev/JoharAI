package com.akeshridev.johar.data.remote

/**
 * Routes Overpass requests across multiple public instances so one overloaded endpoint does not
 * make Ranchi discovery fail. Non-Overpass traffic is delegated unchanged.
 */
internal class OverpassFailoverHttpTextFetcher(
    private val delegate: HttpTextFetcher,
    private val endpoints: List<String> = DEFAULT_ENDPOINTS,
) : HttpTextFetcher {

    override fun get(
        url: String,
        headers: Map<String, String>,
    ): String = delegate.get(url, headers)

    override fun postForm(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String>,
    ): String {
        if (!isOverpass(url)) return delegate.postForm(url, form, headers)

        var lastFailure: Exception? = null
        val candidates = (endpoints + url).distinct()
        for (endpoint in candidates) {
            try {
                return delegate.postForm(endpoint, form, headers)
            } catch (error: Exception) {
                lastFailure = error
            }
        }

        throw lastFailure ?: IllegalStateException("No Overpass endpoint configured")
    }

    private fun isOverpass(url: String): Boolean =
        url.contains("overpass", ignoreCase = true) ||
            url.contains("maps.mail.ru/osm/tools/overpass", ignoreCase = true)

    companion object {
        private val DEFAULT_ENDPOINTS = listOf(
            "https://overpass.private.coffee/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
            "https://overpass-api.de/api/interpreter",
        )
    }
}
