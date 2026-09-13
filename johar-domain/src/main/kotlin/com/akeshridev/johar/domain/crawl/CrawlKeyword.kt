package com.akeshridev.johar.domain.crawl

data class CrawlKeyword(
    val id: String,
    val entityId: String?,
    val category: DiscoveryCategory,
    val term: String,
    val discoveredFrom: String,
    val lastCrawledAtEpochMillis: Long? = null,
    val enabled: Boolean = true,
)

enum class DiscoveryCategory {
    PLACES,
    FOOD,
    FESTIVALS,
    CULTURE,
    EMERGENCY,
    WEATHER,
    LOCAL_BAZAR,
}
