package com.akeshridev.johar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KnowledgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertEntity(entity: KnowledgeEntityRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertEntities(entities: List<KnowledgeEntityRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertFacts(facts: List<SourceFactRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRelationships(relationships: List<EntityRelationshipRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMedia(media: List<MediaAssetRow>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertKeywords(keywords: List<CrawlKeywordRow>)

    @Query("DELETE FROM source_facts WHERE entityId = :entityId AND sourceUrl = :sourceUrl")
    fun deleteFactsForSource(entityId: String, sourceUrl: String)

    @Query("DELETE FROM entity_relationships WHERE fromEntityId = :entityId AND sourceUrl = :sourceUrl")
    fun deleteRelationshipsForSource(entityId: String, sourceUrl: String)

    @Query("DELETE FROM media_assets WHERE entityId = :entityId AND sourceUrl = :sourceUrl")
    fun deleteMediaForSource(entityId: String, sourceUrl: String)

    @Query("SELECT * FROM knowledge_entities WHERE id = :id LIMIT 1")
    fun getEntity(id: String): KnowledgeEntityRow?

    @Query(
        """
        SELECT * FROM knowledge_entities
        WHERE normalizedName = :normalizedName
          AND (:region IS NULL OR region = :region)
        ORDER BY discoveryDepth ASC
        LIMIT 1
        """,
    )
    fun findEntity(normalizedName: String, region: String?): KnowledgeEntityRow?

    @Query(
        """
        SELECT * FROM knowledge_entities
        WHERE enabled = 1
          AND (normalizedName LIKE '%' || :normalizedQuery || '%'
               OR aliasesJson LIKE '%' || :normalizedQuery || '%')
        ORDER BY
          CASE WHEN normalizedName = :normalizedQuery THEN 0 ELSE 1 END,
          discoveryDepth ASC,
          name ASC
        LIMIT :limit
        """,
    )
    fun searchEntities(
        normalizedQuery: String,
        limit: Int = 20,
    ): List<KnowledgeEntityRow>

    @Query(
        """
        SELECT * FROM source_facts
        WHERE entityId = :entityId
        ORDER BY domain ASC, field ASC, retrievedAtEpochMillis DESC
        """,
    )
    fun factsForEntity(entityId: String): List<SourceFactRow>

    @Query(
        """
        SELECT * FROM entity_relationships
        WHERE fromEntityId = :entityId
        ORDER BY predicate ASC, retrievedAtEpochMillis DESC
        """,
    )
    fun relationshipsFromEntity(entityId: String): List<EntityRelationshipRow>

    @Query(
        """
        SELECT * FROM media_assets
        WHERE entityId = :entityId
        ORDER BY type ASC, title ASC
        """,
    )
    fun mediaForEntity(entityId: String): List<MediaAssetRow>

    @Query(
        """
        SELECT * FROM knowledge_entities
        WHERE enabled = 1
          AND (lastCrawledAtEpochMillis IS NULL OR lastCrawledAtEpochMillis < :staleBeforeEpochMillis)
        ORDER BY
          CASE WHEN lastCrawledAtEpochMillis IS NULL THEN 0 ELSE 1 END,
          discoveryDepth ASC,
          lastCrawledAtEpochMillis ASC
        LIMIT :limit
        """,
    )
    fun nextEntitiesToCrawl(
        staleBeforeEpochMillis: Long,
        limit: Int,
    ): List<KnowledgeEntityRow>

    @Query(
        """
        UPDATE knowledge_entities
        SET lastCrawledAtEpochMillis = :crawledAtEpochMillis,
            updatedAtEpochMillis = :crawledAtEpochMillis
        WHERE id = :entityId
        """,
    )
    fun markEntityCrawled(entityId: String, crawledAtEpochMillis: Long)

    @Query(
        """
        SELECT * FROM crawl_keywords
        WHERE enabled = 1
          AND (lastCrawledAtEpochMillis IS NULL OR lastCrawledAtEpochMillis < :staleBeforeEpochMillis)
        ORDER BY
          CASE WHEN lastCrawledAtEpochMillis IS NULL THEN 0 ELSE 1 END,
          failureCount ASC,
          lastCrawledAtEpochMillis ASC
        LIMIT :limit
        """,
    )
    fun nextKeywordsToCrawl(
        staleBeforeEpochMillis: Long,
        limit: Int,
    ): List<CrawlKeywordRow>

    @Query(
        """
        UPDATE crawl_keywords
        SET lastCrawledAtEpochMillis = :crawledAtEpochMillis,
            failureCount = 0
        WHERE id = :keywordId
        """,
    )
    fun markKeywordCrawled(keywordId: String, crawledAtEpochMillis: Long)

    @Query(
        """
        UPDATE crawl_keywords
        SET lastCrawledAtEpochMillis = :crawledAtEpochMillis,
            failureCount = failureCount + 1,
            enabled = CASE WHEN failureCount + 1 >= :disableAfterFailures THEN 0 ELSE enabled END
        WHERE id = :keywordId
        """,
    )
    fun markKeywordFailed(
        keywordId: String,
        crawledAtEpochMillis: Long,
        disableAfterFailures: Int,
    )

    @Query("SELECT COUNT(*) FROM knowledge_entities")
    fun entityCount(): Int

    @Query("SELECT COUNT(*) FROM source_facts")
    fun factCount(): Int

    @Query("SELECT COUNT(*) FROM entity_relationships")
    fun relationshipCount(): Int

    @Query("SELECT COUNT(*) FROM media_assets")
    fun mediaCount(): Int

    @Query("SELECT COUNT(*) FROM crawl_keywords WHERE enabled = 1")
    fun enabledKeywordCount(): Int
}
