package com.akeshridev.johar.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.akeshridev.johar.data.crawl.normalizeText
import org.json.JSONArray
import org.json.JSONObject

/**
 * Adds small, source-backed knowledge upgrades on top of the prebuilt Room seed.
 *
 * This is intentionally separate from the crawler. The booster exists so we can
 * expand grounded query/answer experiments without rebuilding the binary seed DB
 * for every content iteration. Each booster version is applied once and recorded
 * in crawled_sources. Existing entities are resolved by normalized name first so
 * the booster enriches the seed instead of creating duplicate records.
 */
internal object JoharBoosterLoader {
    private const val ASSET_PATH = "johar/johar-booster-2026.09-v1.json"
    private const val MARKER_PREFIX = "asset://johar-booster/"
    private const val TAG = "JoharBooster"

    fun applyIfNeeded(context: Context, database: JoharDatabase) {
        val raw = database.openHelper.writableDatabase
        val root = context.assets.open(ASSET_PATH).bufferedReader().use { JSONObject(it.readText()) }
        val version = root.getString("version")
        val marker = "$MARKER_PREFIX$version"

        if (isApplied(raw, marker)) {
            Log.i(TAG, "already applied version=$version")
            return
        }

        val now = System.currentTimeMillis()
        val resolvedEntityIds = linkedMapOf<String, String>()
        var entityAdds = 0
        var factAdds = 0
        var relationshipAdds = 0

        raw.beginTransaction()
        try {
            val entities = root.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val item = entities.getJSONObject(index)
                val key = item.getString("key")
                val name = item.getString("name")
                val normalizedName = normalizeText(name)
                val existing = findEntity(raw, normalizedName)
                val entityId = existing?.id ?: "booster:$key"

                if (existing == null) {
                    raw.insert(
                        "knowledge_entities",
                        SQLiteDatabase.CONFLICT_IGNORE,
                        ContentValues().apply {
                            put("id", entityId)
                            put("name", name)
                            put("normalizedName", normalizedName)
                            put("type", item.getString("type"))
                            put("description", item.optString("description").takeIf(String::isNotBlank))
                            putNull("latitude")
                            putNull("longitude")
                            put("region", "Jharkhand")
                            put("country", "India")
                            put("aliasesJson", item.optJSONArray("aliases").toJsonArrayString())
                            put("externalRefsJson", JSONObject().put("joharPackType", item.optString("packType")).toString())
                            put("discoveredAtEpochMillis", now)
                            put("updatedAtEpochMillis", now)
                            putNull("lastCrawledAtEpochMillis")
                            put("discoveryDepth", 0)
                            put("enabled", 1)
                        },
                    )
                    entityAdds += 1
                } else {
                    enrichExistingEntity(raw, existing, item, now)
                }
                resolvedEntityIds[key] = entityId
            }

            val facts = root.getJSONArray("facts")
            for (index in 0 until facts.length()) {
                val item = facts.getJSONObject(index)
                val entityId = resolvedEntityIds[item.getString("entityKey")] ?: continue
                val id = "booster:$version:fact:$index"
                raw.insert(
                    "source_facts",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("id", id)
                        put("entityId", entityId)
                        put("sourceUrl", item.getString("sourceUrl"))
                        put("publisher", item.getString("publisher"))
                        put("retrievedAtEpochMillis", now)
                        put("domain", item.getString("domain"))
                        put("field", item.getString("field"))
                        put("valueType", "TEXT")
                        put("textValue", item.getString("value"))
                        putNull("numberValue")
                        putNull("unit")
                        putNull("booleanValue")
                        put("evidenceText", item.getString("evidence"))
                        put("factType", "SOURCE_FACT")
                        put("state", "KNOWN")
                        put("freshness", item.getString("freshness"))
                    },
                )
                factAdds += 1
            }

            val relationships = root.optJSONArray("relationships") ?: JSONArray()
            for (index in 0 until relationships.length()) {
                val item = relationships.getJSONObject(index)
                val fromId = resolvedEntityIds[item.getString("fromKey")] ?: continue
                val toId = findEntity(
                    raw,
                    normalizeText(item.getString("toName")),
                    item.optString("toType").takeIf(String::isNotBlank),
                )?.id ?: continue
                raw.insert(
                    "entity_relationships",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("id", "booster:$version:rel:$index")
                        put("fromEntityId", fromId)
                        put("toEntityId", toId)
                        put("predicate", item.getString("predicate"))
                        put("sourceUrl", item.getString("sourceUrl"))
                        put("publisher", item.getString("publisher"))
                        put("retrievedAtEpochMillis", now)
                        put("evidenceText", item.getString("evidence"))
                    },
                )
                relationshipAdds += 1
            }

            raw.insert(
                "crawled_sources",
                SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues().apply {
                    put("sourceUrl", marker)
                    put("entityId", "booster:$version")
                    put("publisher", "Johar AI")
                    put("fetchedAtEpochMillis", now)
                    put("content", "Applied $ASSET_PATH")
                },
            )
            raw.setTransactionSuccessful()
        } finally {
            raw.endTransaction()
        }

        Log.i(
            TAG,
            "applied version=$version newEntities=$entityAdds facts=$factAdds relationships=$relationshipAdds",
        )
    }

    private fun isApplied(db: androidx.sqlite.db.SupportSQLiteDatabase, marker: String): Boolean =
        db.query("SELECT 1 FROM crawled_sources WHERE sourceUrl = ? LIMIT 1", arrayOf(marker)).use { it.moveToFirst() }

    private fun findEntity(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        normalizedName: String,
        type: String? = null,
    ): ExistingEntity? {
        val sql = if (type == null) {
            "SELECT id, description, externalRefsJson FROM knowledge_entities WHERE normalizedName = ? ORDER BY enabled DESC, discoveryDepth ASC LIMIT 1"
        } else {
            "SELECT id, description, externalRefsJson FROM knowledge_entities WHERE normalizedName = ? AND type = ? ORDER BY enabled DESC, discoveryDepth ASC LIMIT 1"
        }
        val args = if (type == null) arrayOf(normalizedName) else arrayOf(normalizedName, type)
        return db.query(sql, args).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ExistingEntity(
                id = cursor.getString(0),
                description = cursor.getString(1),
                externalRefsJson = cursor.getString(2),
            )
        }
    }

    private fun enrichExistingEntity(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        existing: ExistingEntity,
        item: JSONObject,
        now: Long,
    ) {
        val values = ContentValues().apply { put("updatedAtEpochMillis", now) }
        if (existing.description.isNullOrBlank()) {
            item.optString("description").takeIf(String::isNotBlank)?.let { values.put("description", it) }
        }
        val packType = item.optString("packType")
        if (packType.isNotBlank()) {
            val refs = runCatching { JSONObject(existing.externalRefsJson.ifBlank { "{}" }) }.getOrElse { JSONObject() }
            if (refs.optString("joharPackType").isBlank()) {
                refs.put("joharPackType", packType)
                values.put("externalRefsJson", refs.toString())
            }
        }
        db.update("knowledge_entities", SQLiteDatabase.CONFLICT_ABORT, values, "id = ?", arrayOf(existing.id))
    }

    private fun JSONArray?.toJsonArrayString(): String = this?.toString() ?: "[]"

    private data class ExistingEntity(
        val id: String,
        val description: String?,
        val externalRefsJson: String,
    )
}
