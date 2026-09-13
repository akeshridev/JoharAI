package com.akeshridev.johar.domain.crawl

import com.akeshridev.johar.domain.entity.EntityType

enum class CrawlTarget(
    val seed: CrawlSeed,
) {
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
