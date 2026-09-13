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
        ).build().also { instance = it }
    }
}
