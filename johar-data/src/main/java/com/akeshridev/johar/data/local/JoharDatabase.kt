package com.akeshridev.johar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CrawledSourceEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class JoharDatabase : RoomDatabase() {
    abstract fun crawledSourceDao(): CrawledSourceDao
}
