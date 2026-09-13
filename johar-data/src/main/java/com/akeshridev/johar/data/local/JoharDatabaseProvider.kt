package com.akeshridev.johar.data.local

import android.content.Context
import androidx.room.Room

internal object JoharDatabaseProvider {
    @Volatile
    private var instance: JoharDatabase? = null

    fun get(context: Context): JoharDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            JoharDatabase::class.java,
            "johar.db",
        )
            .createFromAsset("database/johar-base-2026.09.db")
            .addMigrations(JoharMigrations.MIGRATION_1_2)
            .build()
            .also { instance = it }
    }
}
