package com.akeshridev.johar.data.parser

import org.jsoup.Jsoup

internal class HtmlTextCleaner {
    fun clean(html: String): String {
        val document = Jsoup.parse(html)
        document.select("script, style, noscript, nav, header, footer, form").remove()

        val contentRoot = document.selectFirst("main") ?: document.body()
        return contentRoot
            .text()
            .replace(WHITESPACE, " ")
            .trim()
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
