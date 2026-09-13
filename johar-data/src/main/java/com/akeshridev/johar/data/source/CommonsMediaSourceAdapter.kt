package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.crawl.stableId
import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.media.MediaAsset
import com.akeshridev.johar.domain.media.MediaType
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class CommonsMediaSourceAdapter(
    private val fetcher: HttpTextFetcher,
) : CrawlSourceAdapter {
    override val id: String = "wikimedia_commons"

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val ownerId = requireNotNull(seed.entityId)
        val query = "${seed.name} ${seed.region}"
        val sourceUrl = "$API?action=query&format=json&generator=search" +
            "&gsrsearch=${encode(query)}&gsrnamespace=6&gsrlimit=$MEDIA_LIMIT" +
            "&prop=imageinfo&iiprop=url%7Cmime%7Csize%7Cextmetadata&iiurlwidth=1200"
        val raw = fetcher.get(sourceUrl)
        val pages = JSONObject(raw).optJSONObject("query")?.optJSONObject("pages") ?: return null
        val media = mutableListOf<MediaAsset>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val mediaUrl = info.optString("url").takeIf(String::isNotBlank) ?: continue
            val mime = info.optString("mime").takeIf(String::isNotBlank)
            val metadata = info.optJSONObject("extmetadata")
            val descriptionUrl = info.optString("descriptionurl").takeIf(String::isNotBlank)
                ?: "https://commons.wikimedia.org/wiki/${encodePath(page.optString("title"))}"
            val title = page.optString("title")
                .removePrefix("File:")
                .takeIf(String::isNotBlank)

            media += MediaAsset(
                id = stableId("media", ownerId, descriptionUrl, mediaUrl),
                entityId = ownerId,
                type = if (mime?.startsWith("video/") == true) MediaType.VIDEO else MediaType.IMAGE,
                sourceUrl = descriptionUrl,
                mediaUrl = mediaUrl,
                previewUrl = info.optString("thumburl").takeIf(String::isNotBlank),
                title = title,
                description = metadataValue(metadata, "ImageDescription"),
                creator = metadataValue(metadata, "Artist"),
                attributionText = metadataValue(metadata, "Credit"),
                license = metadataValue(metadata, "LicenseShortName"),
                licenseUrl = metadataValue(metadata, "LicenseUrl"),
                mimeType = mime,
                width = info.optInt("width").takeIf { it > 0 },
                height = info.optInt("height").takeIf { it > 0 },
            )
        }

        return SourceResult(
            sourceUrl = sourceUrl,
            publisher = PUBLISHER,
            rawContent = raw,
            media = media,
        )
    }

    private fun metadataValue(metadata: JSONObject?, key: String): String? = metadata
        ?.optJSONObject(key)
        ?.optString("value")
        ?.replace(Regex("<[^>]+>"), " ")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf(String::isNotBlank)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun encodePath(value: String): String = value
        .split('/')
        .joinToString("/") { encode(it).replace("+", "%20") }

    companion object {
        private const val PUBLISHER = "Wikimedia Commons"
        private const val API = "https://commons.wikimedia.org/w/api.php"
        private const val MEDIA_LIMIT = 12
    }
}
