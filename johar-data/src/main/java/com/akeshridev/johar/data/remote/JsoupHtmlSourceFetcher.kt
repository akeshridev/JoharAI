package com.akeshridev.johar.data.remote

import org.jsoup.Jsoup

internal class JsoupHtmlSourceFetcher : HtmlSourceFetcher {
    override fun fetch(url: String): String = Jsoup.connect(url)
        .userAgent("JoharAI/0.1 (Android)")
        .timeout(20_000)
        .get()
        .outerHtml()
}
