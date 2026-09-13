package com.akeshridev.johar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crawled_sources")
data class CrawledSourceEntity(
    @PrimaryKey val sourceUrl: String,
    val entityId: String,
    val publisher: String,
    val fetchedAtEpochMillis: Long,
    val content: String,
)
