package com.akeshridev.johar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object JoharMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `knowledge_entities` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `normalizedName` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `description` TEXT,
                    `latitude` REAL,
                    `longitude` REAL,
                    `region` TEXT,
                    `country` TEXT,
                    `aliasesJson` TEXT NOT NULL,
                    `externalRefsJson` TEXT NOT NULL,
                    `discoveredAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `lastCrawledAtEpochMillis` INTEGER,
                    `discoveryDepth` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_entities_normalizedName` ON `knowledge_entities` (`normalizedName`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_entities_lastCrawledAtEpochMillis` ON `knowledge_entities` (`lastCrawledAtEpochMillis`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_entities_type` ON `knowledge_entities` (`type`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `source_facts` (
                    `id` TEXT NOT NULL,
                    `entityId` TEXT NOT NULL,
                    `sourceUrl` TEXT NOT NULL,
                    `publisher` TEXT NOT NULL,
                    `retrievedAtEpochMillis` INTEGER NOT NULL,
                    `domain` TEXT NOT NULL,
                    `field` TEXT NOT NULL,
                    `valueType` TEXT NOT NULL,
                    `textValue` TEXT,
                    `numberValue` REAL,
                    `unit` TEXT,
                    `booleanValue` INTEGER,
                    `evidenceText` TEXT NOT NULL,
                    `factType` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `freshness` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_facts_entityId` ON `source_facts` (`entityId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_facts_domain` ON `source_facts` (`domain`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_facts_sourceUrl` ON `source_facts` (`sourceUrl`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `entity_relationships` (
                    `id` TEXT NOT NULL,
                    `fromEntityId` TEXT NOT NULL,
                    `toEntityId` TEXT NOT NULL,
                    `predicate` TEXT NOT NULL,
                    `sourceUrl` TEXT NOT NULL,
                    `publisher` TEXT NOT NULL,
                    `retrievedAtEpochMillis` INTEGER NOT NULL,
                    `evidenceText` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entity_relationships_fromEntityId` ON `entity_relationships` (`fromEntityId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entity_relationships_toEntityId` ON `entity_relationships` (`toEntityId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entity_relationships_predicate` ON `entity_relationships` (`predicate`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_assets` (
                    `id` TEXT NOT NULL,
                    `entityId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `sourceUrl` TEXT NOT NULL,
                    `mediaUrl` TEXT NOT NULL,
                    `previewUrl` TEXT,
                    `title` TEXT,
                    `description` TEXT,
                    `creator` TEXT,
                    `attributionText` TEXT,
                    `license` TEXT,
                    `licenseUrl` TEXT,
                    `mimeType` TEXT,
                    `width` INTEGER,
                    `height` INTEGER,
                    `durationMillis` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_entityId` ON `media_assets` (`entityId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_type` ON `media_assets` (`type`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `crawl_keywords` (
                    `id` TEXT NOT NULL,
                    `entityId` TEXT,
                    `category` TEXT NOT NULL,
                    `term` TEXT NOT NULL,
                    `normalizedTerm` TEXT NOT NULL,
                    `discoveredFrom` TEXT NOT NULL,
                    `lastCrawledAtEpochMillis` INTEGER,
                    `enabled` INTEGER NOT NULL,
                    `failureCount` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_crawl_keywords_entityId` ON `crawl_keywords` (`entityId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_crawl_keywords_category` ON `crawl_keywords` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_crawl_keywords_lastCrawledAtEpochMillis` ON `crawl_keywords` (`lastCrawledAtEpochMillis`)")
        }
    }
}
