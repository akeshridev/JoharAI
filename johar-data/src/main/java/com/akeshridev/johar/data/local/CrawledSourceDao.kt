package com.akeshridev.johar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CrawledSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(source: CrawledSourceEntity)

    @Query("SELECT * FROM crawled_sources ORDER BY fetchedAtEpochMillis DESC")
    fun getAll(): List<CrawledSourceEntity>
}
