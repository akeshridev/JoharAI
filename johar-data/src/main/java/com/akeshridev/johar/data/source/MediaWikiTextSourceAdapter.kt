package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.crawl.stableId
import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlKeyword
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.crawl.DiscoveryCategory
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import com.akeshridev.johar.domain.media.MediaAsset
import com.akeshridev.johar.domain.media.MediaType
import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.Freshness
import com.akeshridev.johar.domain.source.KnowledgeDomain
import com.akeshridev.johar.domain.source.SourceFact
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class MediaWikiTextSourceAdapter(
    override val id: String,
    private val host: String,
    private val publisher: String,
    private val defaultDomain: KnowledgeDomain,
    private val referenceKey: String,
    private val discoveryCategories: Set<DiscoveryCategory>,
    private val fetcher: HttpTextFetcher,
) : CrawlSourceAdapter, KeywordDiscoveryAdapter {

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val ownerId = requireNotNull(seed.entityId)
        val pageId = seed.externalRefs[referenceKey]?.toLongOrNull()
            ?: resolvePageId(seed)
            ?: return null
        val pageUrl = pageQueryUrl(pageId)
        val raw = fetcher.get(pageUrl)
        val page = firstPage(JSONObject(raw)) ?: return null
        if (page.optBoolean("missing", false)) return null

        val title = page.optString("title").takeIf(String::isNotBlank) ?: seed.name
        val fullUrl = page.optString("fullurl").takeIf(String::isNotBlank)
            ?: "https://$host/?curid=$pageId"
        val extract = page.optString("extract").trim()
        val excerpt = extract.take(MAX_FACT_TEXT_CHARS)
        val retrievedAt = System.currentTimeMillis()
        val pageImage = page.optJSONObject("original") ?: page.optJSONObject("thumbnail")
        val thumbnail = page.optJSONObject("thumbnail")

        val media = pageImage?.optString("source")
            ?.takeIf(String::isNotBlank)
            ?.let { imageUrl ->
                listOf(
                    MediaAsset(
                        id = stableId("media", ownerId, fullUrl, imageUrl),
                        entityId = ownerId,
                        type = MediaType.IMAGE,
                        sourceUrl = fullUrl,
                        mediaUrl = imageUrl,
                        previewUrl = thumbnail?.optString("source")?.takeIf(String::isNotBlank),
                        title = title,
                    ),
                )
            }
            .orEmpty()

        val facts = if (excerpt.isBlank()) {
            emptyList()
        } else {
            listOf(
                SourceFact(
                    entityId = ownerId,
                    sourceUrl = fullUrl,
                    publisher = publisher,
                    retrievedAtEpochMillis = retrievedAt,
                    domain = defaultDomain,
                    field = "$id.article_excerpt",
                    value = FactValue.Text(excerpt),
                    evidenceText = excerpt.take(MAX_EVIDENCE_CHARS),
                    freshness = Freshness.EVERGREEN,
                ),
            )
        }

        val entity = KnowledgeEntity(
            id = ownerId,
            name = title,
            type = seed.entityType ?: EntityType.OTHER,
            description = extract.lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.take(MAX_DESCRIPTION_CHARS),
            latitude = seed.latitude,
            longitude = seed.longitude,
            region = seed.region,
            country = seed.country,
            externalRefs = seed.externalRefs + (referenceKey to pageId.toString()),
        )

        return SourceResult(
            sourceUrl = fullUrl,
            publisher = publisher,
            rawContent = raw,
            entity = entity,
            facts = facts,
            media = media,
        )
    }

    override fun supports(keyword: CrawlKeyword): Boolean = keyword.category in discoveryCategories

    override fun discover(keyword: CrawlKeyword): DiscoveryResult {
        val url = searchUrl(keyword.term, DISCOVERY_LIMIT)
        val raw = fetcher.get(url)
        val results = JSONObject(raw)
            .optJSONObject("query")
            ?.optJSONArray("search")
            ?: JSONArray()
        val entities = buildList {
            for (index in 0 until results.length()) {
                val result = results.optJSONObject(index) ?: continue
                val pageId = result.optLong("pageid")
                val title = result.optString("title")
                if (pageId <= 0 || title.isBlank()) continue
                add(
                    KnowledgeEntity(
                        id = "$id:$pageId",
                        name = title,
                        type = typeFor(keyword.category),
                        description = result.optString("snippet")
                            .replace(Regex("<[^>]+>"), " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                            .takeIf(String::isNotBlank),
                        region = "Jharkhand",
                        country = "India",
                        externalRefs = mapOf(referenceKey to pageId.toString()),
                    ),
                )
            }
        }
        return DiscoveryResult(
            sourceUrl = url,
            publisher = publisher,
            rawContent = raw,
            entities = entities.distinctBy { it.id },
        )
    }

    private fun resolvePageId(seed: CrawlSeed): Long? {
        val raw = fetcher.get(searchUrl("${seed.name} ${seed.region}", RESOLUTION_LIMIT))
        val results = JSONObject(raw)
            .optJSONObject("query")
            ?.optJSONArray("search")
            ?: return null
        if (results.length() == 0) return null

        val normalizedSeed = normalizeText(seed.name)
        var bestId: Long? = null
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val result = results.optJSONObject(index) ?: continue
            val candidateId = result.optLong("pageid")
            if (candidateId <= 0) continue
            val title = result.optString("title")
            var score = 0
            if (normalizeText(title) == normalizedSeed) score += 100
            if (title.contains(seed.name, ignoreCase = true)) score += 20
            if (score > bestScore) {
                bestScore = score
                bestId = candidateId
            }
        }
        return bestId
    }

    private fun searchUrl(query: String, limit: Int): String =
        "https://$host/w/api.php?action=query&format=json&list=search" +
            "&srlimit=$limit&srsearch=${encode(query)}"

    private fun pageQueryUrl(pageId: Long): String =
        "https://$host/w/api.php?action=query&format=json&pageids=$pageId" +
            "&prop=extracts%7Cpageimages%7Cinfo&explaintext=1&exsectionformat=plain" +
            "&inprop=url&piprop=thumbnail%7Coriginal&pithumbsize=800"

    private fun firstPage(root: JSONObject): JSONObject? {
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val keys = pages.keys()
        while (keys.hasNext()) {
            return pages.optJSONObject(keys.next())
        }
        return null
    }

    private fun typeFor(category: DiscoveryCategory): EntityType = when (category) {
        DiscoveryCategory.FOOD -> EntityType.FOOD
        DiscoveryCategory.FESTIVALS -> EntityType.FESTIVAL
        DiscoveryCategory.CULTURE -> EntityType.CULTURAL_PRACTICE
        DiscoveryCategory.LOCAL_BAZAR -> EntityType.MARKET
        DiscoveryCategory.EMERGENCY -> EntityType.EMERGENCY_SERVICE
        else -> EntityType.PLACE
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    companion object {
        private const val RESOLUTION_LIMIT = 5
        private const val DISCOVERY_LIMIT = 15
        private const val MAX_FACT_TEXT_CHARS = 8_000
        private const val MAX_DESCRIPTION_CHARS = 500
        private const val MAX_EVIDENCE_CHARS = 2_000
    }
}
