package com.akeshridev.johar.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

internal object JoharDatabaseProvider {
    @Volatile
    private var instance: JoharDatabase? = null

    fun get(context: Context): JoharDatabase = instance ?: synchronized(this) {
        val appContext = context.applicationContext
        val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
        val existedBeforeOpen = databaseFile.exists()

        instance ?: Room.databaseBuilder(
            appContext,
            JoharDatabase::class.java,
            DATABASE_NAME,
        )
            .createFromAsset("johar-base-2026.09.db")
            .addMigrations(JoharMigrations.MIGRATION_1_2)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        logSeedDiagnostics(db, importedFromAsset = !existedBeforeOpen)
                    }
                },
            )
            .build()
            .also { instance = it }
    }

    private fun logSeedDiagnostics(
        db: SupportSQLiteDatabase,
        importedFromAsset: Boolean,
    ) {
        val entityCount = count(db, "knowledge_entities")
        val factCount = count(db, "source_facts")
        val relationshipCount = count(db, "entity_relationships")
        val mediaCount = count(db, "media_assets")

        if (importedFromAsset) {
            Log.i(TAG, "Prebuilt Johar database imported successfully from APK asset")
        } else {
            Log.i(TAG, "Existing Johar database opened successfully")
        }

        Log.i(
            TAG,
            "records entities=$entityCount facts=$factCount relationships=$relationshipCount media=$mediaCount",
        )

        db.query(
            """
            SELECT name, type, description
            FROM knowledge_entities
            WHERE enabled = 1
            ORDER BY name ASC
            LIMIT 10
            """.trimIndent(),
        ).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val descriptionIndex = cursor.getColumnIndexOrThrow("description")
            var index = 1
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val type = cursor.getString(typeIndex)
                val description = if (cursor.isNull(descriptionIndex)) {
                    null
                } else {
                    cursor.getString(descriptionIndex)
                }
                Log.i(
                    TAG,
                    "sample[$index] name=$name type=$type description=${description.orEmpty()}",
                )
                index += 1
            }
        }
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Long =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

    private const val DATABASE_NAME = "johar.db"
    private const val TAG = "JoharDB"
}
