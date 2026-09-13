package com.akeshridev.johar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CrawledSourceEntity::class,
        KnowledgeEntityRow::class,
        SourceFactRow::class,
        EntityRelationshipRow::class,
        MediaAssetRow::class,
        CrawlKeywordRow::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class JoharDatabase : RoomDatabase() {
    abstract fun crawledSourceDao(): CrawledSourceDao
    abstract fun knowledgeDao(): KnowledgeDao
}
