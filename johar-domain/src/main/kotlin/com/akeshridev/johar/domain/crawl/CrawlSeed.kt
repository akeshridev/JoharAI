package com.akeshridev.johar.domain.crawl

import com.akeshridev.johar.domain.entity.EntityType

data class CrawlSeed(
    val name: String,
    val region: String = "Jharkhand",
    val country: String = "India",
    val entityId: String? = null,
    val entityType: EntityType? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val externalRefs: Map<String, String> = emptyMap(),
    val depth: Int = 0,
)
