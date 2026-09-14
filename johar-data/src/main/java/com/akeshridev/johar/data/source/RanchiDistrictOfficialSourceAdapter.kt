package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.crawl.stableId
import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.entity.KnowledgeEntity
import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.Freshness
import com.akeshridev.johar.domain.source.KnowledgeDomain
import com.akeshridev.johar.domain.source.SourceFact
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * High-trust Ranchi V1 source. The District Ranchi NIC site publishes practical public
 * utility, police, locality/PIN and tourist information that should be available offline
 * without relying on model memory.
 *
 * Individual page failures are isolated so one stale category cannot block the whole crawl.
 */
internal class RanchiDistrictOfficialSourceAdapter(
    private val fetcher: HttpTextFetcher,
) : CrawlSourceAdapter {
    override val id: String = "ranchi_district_official"

    override fun supports(seed: CrawlSeed): Boolean =
        seed.name.equals("Ranchi", ignoreCase = true) && seed.entityType == EntityType.CITY

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val ownerId = requireNotNull(seed.entityId)
        val retrievedAt = System.currentTimeMillis()
        val entities = mutableListOf<KnowledgeEntity>()
        val facts = mutableListOf<SourceFact>()
        val snapshots = mutableListOf<String>()

        DIRECTORY_PAGES.forEach { spec ->
            runCatching { fetcher.get(spec.url) }
                .onSuccess { raw ->
                    snapshots += "URL=${spec.url}\n$raw"
                    parseDirectoryPage(spec, raw, retrievedAt, entities, facts)
                }
        }

        runCatching { fetcher.get(POLICE_URL) }
            .onSuccess { raw ->
                snapshots += "URL=$POLICE_URL\n$raw"
                parsePolice(raw, retrievedAt, entities, facts)
            }

        runCatching { fetcher.get(PINCODE_URL) }
            .onSuccess { raw ->
                snapshots += "URL=$PINCODE_URL\n$raw"
                parseCityLocalities(raw, retrievedAt, entities, facts)
            }

        runCatching { fetcher.get(TOURIST_PLACES_URL) }
            .onSuccess { raw ->
                snapshots += "URL=$TOURIST_PLACES_URL\n$raw"
                parseTouristPlaces(raw, retrievedAt, entities, facts, snapshots)
            }

        runCatching { fetcher.get(HOME_URL) }
            .onSuccess { raw ->
                snapshots += "URL=$HOME_URL\n$raw"
                parseHelplines(ownerId, raw, retrievedAt, facts)
            }

        if (snapshots.isEmpty()) return null

        return SourceResult(
            sourceUrl = HOME_URL,
            publisher = PUBLISHER,
            rawContent = snapshots.joinToString("\n\n--- SOURCE ---\n\n"),
            discoveredEntities = entities.distinctBy { normalizeText(it.name) to it.type },
            facts = facts.distinctBy { listOf(it.entityId, it.sourceUrl, it.field, factValueKey(it.value)) },
        )
    }

    private fun parseDirectoryPage(
        spec: DirectorySpec,
        raw: String,
        retrievedAt: Long,
        entities: MutableList<KnowledgeEntity>,
        facts: MutableList<SourceFact>,
    ) {
        val document = Jsoup.parse(raw, spec.url)
        document.select("h2").forEach { heading ->
            val name = heading.text().trim()
            if (!validDirectoryName(name, spec.pageTitle)) return@forEach

            val container = nearestItemContainer(heading)
            val address = container
                ?.selectFirst("a[href*=google.com]")
                ?.text()
                ?.trim()
                ?.takeIf(String::isNotBlank)
            val text = container?.text().orEmpty()
            val phone = PHONE_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', ',')
            val website = container
                ?.select("a[href]")
                ?.asSequence()
                ?.map { it.absUrl("href") }
                ?.firstOrNull(::isUsefulExternalWebsite)

            val entityId = officialEntityId(spec.key, name)
            entities += KnowledgeEntity(
                id = entityId,
                name = name,
                type = spec.entityType,
                description = buildDescription(spec.label, address),
                region = "Jharkhand",
                country = "India",
                externalRefs = mapOf(
                    REF_OFFICIAL_URL to spec.url,
                    REF_OFFICIAL_CATEGORY to spec.key,
                    REF_PACK_TYPE to spec.packType,
                ),
            )

            facts += sourceFact(
                entityId = entityId,
                sourceUrl = spec.url,
                retrievedAt = retrievedAt,
                domain = spec.domain,
                field = "officialCategory",
                value = spec.label,
                evidence = "$name is listed under ${spec.pageTitle} by District Ranchi.",
                freshness = Freshness.EVERGREEN,
            )
            address?.let {
                facts += sourceFact(
                    entityId = entityId,
                    sourceUrl = spec.url,
                    retrievedAt = retrievedAt,
                    domain = KnowledgeDomain.GEOGRAPHY,
                    field = "address",
                    value = it,
                    evidence = it,
                    freshness = Freshness.EVERGREEN,
                )
            }
            phone?.let {
                facts += sourceFact(
                    entityId = entityId,
                    sourceUrl = spec.url,
                    retrievedAt = retrievedAt,
                    domain = spec.domain,
                    field = "phone",
                    value = it,
                    evidence = "Phone: $it",
                    freshness = Freshness.LIVE,
                )
            }
            website?.let {
                facts += sourceFact(
                    entityId = entityId,
                    sourceUrl = spec.url,
                    retrievedAt = retrievedAt,
                    domain = KnowledgeDomain.FACILITIES,
                    field = "website",
                    value = it,
                    evidence = "Website: $it",
                    freshness = Freshness.LIVE,
                )
            }
        }
    }

    private fun parsePolice(
        raw: String,
        retrievedAt: Long,
        entities: MutableList<KnowledgeEntity>,
        facts: MutableList<SourceFact>,
    ) {
        val document = Jsoup.parse(raw, POLICE_URL)
        document.select("table tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 3) return@forEach
            val name = cells[0].text().trim()
            if (name.isBlank() || name.equals("POLICE STATION", true)) return@forEach

            val landline = cells[1].text().trim().takeUnless(::isEmptyContact)
            val mobile = cells[2].text().trim().takeUnless(::isEmptyContact)
            val entityId = officialEntityId("police", name)

            entities += KnowledgeEntity(
                id = entityId,
                name = name,
                type = EntityType.POLICE_STATION,
                description = "Police station in Ranchi district.",
                region = "Jharkhand",
                country = "India",
                externalRefs = mapOf(
                    REF_OFFICIAL_URL to POLICE_URL,
                    REF_OFFICIAL_CATEGORY to "police",
                    REF_PACK_TYPE to "POLICE_STATION",
                ),
            )

            landline?.let {
                facts += sourceFact(
                    entityId, POLICE_URL, retrievedAt, KnowledgeDomain.SAFETY_EMERGENCY,
                    "landline", it, "$name landline: $it", Freshness.LIVE,
                )
            }
            mobile?.let {
                facts += sourceFact(
                    entityId, POLICE_URL, retrievedAt, KnowledgeDomain.SAFETY_EMERGENCY,
                    "mobile", it, "$name mobile: $it", Freshness.LIVE,
                )
            }
        }
    }

    private fun parseCityLocalities(
        raw: String,
        retrievedAt: Long,
        entities: MutableList<KnowledgeEntity>,
        facts: MutableList<SourceFact>,
    ) {
        val document = Jsoup.parse(raw, PINCODE_URL)
        document.select("table tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 6) return@forEach
            val office = cells[1].text().trim()
            val taluk = cells[2].text().trim()
            val district = cells[3].text().trim()
            val state = cells[4].text().trim()
            val pincode = cells[5].text().trim()

            // The official page includes legacy entries from old district boundaries. For the
            // city prototype, retain rows explicitly attached to Ranchi taluk only.
            if (!district.equals("Ranchi", true) || !taluk.equals("Ranchi", true)) return@forEach
            if (office.isBlank() || pincode.length !in 5..6) return@forEach

            val entityId = officialEntityId("locality", office)
            entities += KnowledgeEntity(
                id = entityId,
                name = office,
                type = EntityType.PLACE,
                description = "$office locality/postal area in Ranchi city.",
                region = "Jharkhand",
                country = "India",
                externalRefs = mapOf(
                    REF_OFFICIAL_URL to PINCODE_URL,
                    REF_OFFICIAL_CATEGORY to "locality",
                    REF_PACK_TYPE to "LOCALITY",
                ),
            )
            facts += sourceFact(
                entityId, PINCODE_URL, retrievedAt, KnowledgeDomain.GEOGRAPHY,
                "pincode", pincode, "$office | $taluk | $district | $state | $pincode", Freshness.EVERGREEN,
            )
            facts += sourceFact(
                entityId, PINCODE_URL, retrievedAt, KnowledgeDomain.GEOGRAPHY,
                "taluk", taluk, "$office is listed under $taluk taluk.", Freshness.EVERGREEN,
            )
        }
    }

    private fun parseTouristPlaces(
        raw: String,
        retrievedAt: Long,
        entities: MutableList<KnowledgeEntity>,
        facts: MutableList<SourceFact>,
        snapshots: MutableList<String>,
    ) {
        val document = Jsoup.parse(raw, TOURIST_PLACES_URL)
        val links = document.select("a[href*=/tourist-place/]")
            .mapNotNull { anchor ->
                val name = anchor.text().trim().takeIf(String::isNotBlank) ?: return@mapNotNull null
                val url = anchor.absUrl("href").takeIf(String::isNotBlank) ?: return@mapNotNull null
                name to url
            }
            .distinctBy { it.second }

        links.take(MAX_TOURIST_DETAIL_PAGES).forEach { (listedName, url) ->
            val detailRaw = runCatching { fetcher.get(url) }.getOrNull()
            if (detailRaw != null) snapshots += "URL=$url\n$detailRaw"
            val detail = detailRaw?.let { Jsoup.parse(it, url) }
            val detailName = detail?.selectFirst("h1")?.text()?.trim()
            val name = detailName?.takeIf { it.isNotBlank() } ?: listedName
            val description = detail?.select("p")
                ?.asSequence()
                ?.map { it.text().trim() }
                ?.firstOrNull { it.length >= MIN_TOURIST_DESCRIPTION_CHARS && !isFooterText(it) }
                ?: nearbySummary(document, url)
            val entityId = officialEntityId("tourism", name)
            val packType = touristPackType(name)

            entities += KnowledgeEntity(
                id = entityId,
                name = name,
                type = touristEntityType(packType),
                description = description,
                region = "Jharkhand",
                country = "India",
                externalRefs = mapOf(
                    REF_OFFICIAL_URL to url,
                    REF_OFFICIAL_CATEGORY to "tourism",
                    REF_PACK_TYPE to packType,
                ),
            )
            description?.let {
                facts += sourceFact(
                    entityId, url, retrievedAt, KnowledgeDomain.TOURISM,
                    "description", it, it, Freshness.EVERGREEN,
                )
            }
            detail?.let { page ->
                howToReachFacts(entityId, url, page, retrievedAt).forEach(facts::add)
            }
        }
    }

    private fun howToReachFacts(
        entityId: String,
        sourceUrl: String,
        document: Document,
        retrievedAt: Long,
    ): List<SourceFact> = buildList {
        document.select("h3").forEach { heading ->
            val label = normalizeText(heading.text())
            val field = when {
                "air" in label -> "reachByAir"
                "train" in label || "rail" in label -> "reachByTrain"
                "road" in label -> "reachByRoad"
                else -> null
            } ?: return@forEach
            val value = heading.nextElementSibling()?.text()?.trim().orEmpty()
            if (value.isBlank()) return@forEach
            add(
                sourceFact(
                    entityId, sourceUrl, retrievedAt, KnowledgeDomain.TRAVEL_LOGISTICS,
                    field, value, value, Freshness.EVERGREEN,
                ),
            )
        }
    }

    private fun parseHelplines(
        ownerId: String,
        raw: String,
        retrievedAt: Long,
        facts: MutableList<SourceFact>,
    ) {
        val text = Jsoup.parse(raw, HOME_URL).text()
        HELPLINES.forEach { spec ->
            val match = spec.regex.find(text) ?: return@forEach
            val number = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (number.isBlank()) return@forEach
            facts += sourceFact(
                ownerId, HOME_URL, retrievedAt, KnowledgeDomain.SAFETY_EMERGENCY,
                spec.field, number, match.value, Freshness.LIVE,
            )
        }
    }

    private fun sourceFact(
        entityId: String,
        sourceUrl: String,
        retrievedAt: Long,
        domain: KnowledgeDomain,
        field: String,
        value: String,
        evidence: String,
        freshness: Freshness,
    ): SourceFact = SourceFact(
        entityId = entityId,
        sourceUrl = sourceUrl,
        publisher = PUBLISHER,
        retrievedAtEpochMillis = retrievedAt,
        domain = domain,
        field = field,
        value = FactValue.Text(value),
        evidenceText = evidence,
        freshness = freshness,
    )

    private fun nearbySummary(document: Document, detailUrl: String): String? {
        val anchor = document.select("a[href]").firstOrNull { it.absUrl("href") == detailUrl } ?: return null
        val container = nearestItemContainer(anchor) ?: anchor.parent() ?: return null
        return container.text()
            .replace(anchor.text(), "")
            .replace("Direction", "")
            .replace("Share on Facebook", "")
            .replace("Share on X (Formaly Twitter)", "")
            .trim()
            .takeIf { it.length >= 30 }
    }

    private fun nearestItemContainer(element: Element): Element? =
        element.parents().firstOrNull { parent ->
            parent.tagName() == "li" || parent.classNames().any { it.contains("row", true) || it.contains("item", true) }
        } ?: element.parent()

    private fun validDirectoryName(name: String, pageTitle: String): Boolean {
        if (name.isBlank() || name.equals(pageTitle, true)) return false
        val normalized = normalizeText(name)
        return normalized !in GENERIC_HEADINGS && !normalized.startsWith("load more")
    }

    private fun buildDescription(label: String, address: String?): String =
        listOfNotNull("$label in Ranchi", address).joinToString(". ")

    private fun officialEntityId(category: String, name: String): String =
        stableId("ranchi-official", category, normalizeText(name))

    private fun touristPackType(name: String): String {
        val normalized = normalizeText(name)
        return when {
            "waterfall" in normalized || " fall" in " $normalized" -> "WATERFALL"
            "temple" in normalized || "mandir" in normalized -> "TEMPLE"
            "hill" in normalized -> "HILL"
            "garden" in normalized || "park" in normalized -> "PARK"
            "museum" in normalized -> "MUSEUM"
            "zoo" in normalized -> "ZOO"
            "aquarium" in normalized -> "AQUARIUM"
            else -> "TOURIST_ATTRACTION"
        }
    }

    private fun touristEntityType(packType: String): EntityType = when (packType) {
        "WATERFALL", "HILL" -> EntityType.NATURAL_FEATURE
        else -> EntityType.TOURIST_ATTRACTION
    }

    private fun isUsefulExternalWebsite(url: String): Boolean =
        url.startsWith("http") &&
            !url.contains("google.com") &&
            !url.contains("facebook.com") &&
            !url.contains("x.com") &&
            !url.contains("twitter.com") &&
            !url.contains("ranchi.nic.in") &&
            !url.contains("nic.in")

    private fun isEmptyContact(value: String): Boolean =
        value.isBlank() || value == "-" || value == "–" || value.equals("NA", true)

    private fun isFooterText(value: String): Boolean {
        val normalized = normalizeText(value)
        return normalized.contains("content owned by") ||
            normalized.contains("national informatics centre") ||
            normalized.contains("website policies")
    }

    private fun factValueKey(value: FactValue): String = when (value) {
        is FactValue.Text -> value.value
        is FactValue.Number -> "${value.value}:${value.unit.orEmpty()}"
        is FactValue.BooleanValue -> value.value.toString()
    }

    private data class DirectorySpec(
        val key: String,
        val label: String,
        val pageTitle: String,
        val url: String,
        val entityType: EntityType,
        val domain: KnowledgeDomain,
        val packType: String,
    )

    private data class HelplineSpec(
        val field: String,
        val regex: Regex,
    )

    companion object {
        private const val PUBLISHER = "District Ranchi, Government of Jharkhand / National Informatics Centre"
        private const val HOME_URL = "https://ranchi.nic.in/"
        private const val POLICE_URL = "https://ranchi.nic.in/police/"
        private const val PINCODE_URL = "https://ranchi.nic.in/pincode-ranchi-district/"
        private const val TOURIST_PLACES_URL = "https://ranchi.nic.in/tourist-places/"
        private const val REF_OFFICIAL_URL = "ranchiOfficialUrl"
        private const val REF_OFFICIAL_CATEGORY = "ranchiOfficialCategory"
        private const val REF_PACK_TYPE = "joharPackType"
        private const val MAX_TOURIST_DETAIL_PAGES = 20
        private const val MIN_TOURIST_DESCRIPTION_CHARS = 55

        private val PHONE_REGEX = Regex("(?i)Phone\\s*:\\s*([+0-9][+0-9\\s-]{5,})")
        private val GENERIC_HEADINGS = setOf(
            "photo gallery", "how to reach", "by air", "by train", "by road", "public utilities",
        )

        private val DIRECTORY_PAGES = listOf(
            DirectorySpec(
                key = "hospital",
                label = "Hospital/health facility",
                pageTitle = "Hospital",
                url = "https://ranchi.nic.in/public-utility-category/hospital/",
                entityType = EntityType.HOSPITAL,
                domain = KnowledgeDomain.SAFETY_EMERGENCY,
                packType = "HOSPITAL",
            ),
            DirectorySpec(
                key = "hospital",
                label = "Hospital/health facility",
                pageTitle = "Hospital",
                url = "https://ranchi.nic.in/public-utility-category/hospital/page/2/",
                entityType = EntityType.HOSPITAL,
                domain = KnowledgeDomain.SAFETY_EMERGENCY,
                packType = "HOSPITAL",
            ),
            DirectorySpec(
                key = "college-university",
                label = "College/university",
                pageTitle = "College/University",
                url = "https://ranchi.nic.in/public-utility-category/college-university/",
                entityType = EntityType.ORGANIZATION,
                domain = KnowledgeDomain.FACILITIES,
                packType = "COLLEGE_UNIVERSITY",
            ),
            DirectorySpec(
                key = "school",
                label = "School",
                pageTitle = "School",
                url = "https://ranchi.nic.in/public-utility-category/school/",
                entityType = EntityType.ORGANIZATION,
                domain = KnowledgeDomain.FACILITIES,
                packType = "SCHOOL",
            ),
            DirectorySpec(
                key = "bank",
                label = "Bank",
                pageTitle = "Bank",
                url = "https://ranchi.nic.in/public-utility-category/bank/",
                entityType = EntityType.ORGANIZATION,
                domain = KnowledgeDomain.FACILITIES,
                packType = "BANK",
            ),
            DirectorySpec(
                key = "bank",
                label = "Bank",
                pageTitle = "Bank",
                url = "https://ranchi.nic.in/public-utility-category/bank/page/2/",
                entityType = EntityType.ORGANIZATION,
                domain = KnowledgeDomain.FACILITIES,
                packType = "BANK",
            ),
            DirectorySpec(
                key = "electricity",
                label = "Electricity/public utility",
                pageTitle = "Electricity",
                url = "https://ranchi.nic.in/public-utility-category/electricity/",
                entityType = EntityType.ORGANIZATION,
                domain = KnowledgeDomain.FACILITIES,
                packType = "ELECTRICITY_UTILITY",
            ),
            DirectorySpec(
                key = "municipality",
                label = "Municipality",
                pageTitle = "Municipality",
                url = "https://ranchi.nic.in/public-utility-category/municipality/",
                entityType = EntityType.ORGANIZATION,
                domain = KnowledgeDomain.FACILITIES,
                packType = "MUNICIPALITY",
            ),
        )

        private val HELPLINES = listOf(
            HelplineSpec("helpline.women", Regex("(?i)Woman\\s+helpline\\s*[-:]\\s*(\\d{2,6})")),
            HelplineSpec("helpline.child", Regex("(?i)Child\\s+helpline\\s*[-:]\\s*(\\d{2,6})")),
            HelplineSpec("helpline.police", Regex("(?i)Police\\s+helpline\\s*[-:]\\s*(\\d{2,6})")),
            HelplineSpec("helpline.ambulance", Regex("(?i)Ambulance\\s+seva\\s*[-:]\\s*(\\d{2,6})")),
        )
    }
}
