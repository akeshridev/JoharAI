package com.akeshridev.johar.domain.crawl

import com.akeshridev.johar.domain.entity.EntityType

enum class CrawlTarget(
    val seed: CrawlSeed,
) {
    RANCHI(
        CrawlSeed(
            name = "Ranchi",
            region = "Jharkhand",
            country = "India",
            entityType = EntityType.CITY,
        ),
    ),
    JHARKHAND(
        CrawlSeed(
            name = "Jharkhand",
            region = "Jharkhand",
            country = "India",
            entityType = EntityType.REGION,
        ),
    ),
    DASSAM_FALLS(
        CrawlSeed(
            name = "Dassam Falls",
            region = "Jharkhand",
            country = "India",
            entityType = EntityType.TOURIST_ATTRACTION,
        ),
    ),
}
