package com.akeshridev.johar.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.akeshridev.johar.data.crawl.normalizeText
import org.json.JSONArray
import org.json.JSONObject

/**
 * Applies small, source-backed knowledge upgrades on top of the prebuilt Room seed.
 * Each asset is versioned and idempotent so query experiments can evolve without
 * rebuilding the binary database for every content iteration.
 */
internal object JoharBoosterLoader {
    private val ASSET_PATHS = listOf(
        "johar/johar-booster-2026.09-v1.json",
        "johar/johar-booster-2026.09-v2.json",
        "johar/johar-booster-2026.09-v3-spatial.json",
        "johar/johar-booster-2026.09-v4-ranchi-essentials.json",
    )
    private const val MARKER_PREFIX = "asset://johar-booster/"
    private const val TAG = "JoharBooster"

    fun applyIfNeeded(context: Context, database: JoharDatabase) {
        ASSET_PATHS.forEach { assetPath -> applyAssetIfNeeded(context, database, assetPath) }
    }

    private fun applyAssetIfNeeded(context: Context, database: JoharDatabase, assetPath: String) {
        val raw = database.openHelper.writableDatabase
        val root = context.assets.open(assetPath).bufferedReader().use { JSONObject(it.readText()) }
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
                            putOptionalDouble("latitude", item, "latitude")
                            putOptionalDouble("longitude", item, "longitude")
                            put("region", item.optString("region").takeIf(String::isNotBlank) ?: "Jharkhand")
                            put("country", item.optString("country").takeIf(String::isNotBlank) ?: "India")
                            put("aliasesJson", item.optJSONArray("aliases").toJsonArrayString())
                            put("externalRefsJson", boosterRefs(item).toString())
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

            val facts = root.optJSONArray("facts") ?: JSONArray()
            for (index in 0 until facts.length()) {
                val item = facts.getJSONObject(index)
                val entityId = resolvedEntityIds[item.getString("entityKey")] ?: continue
                raw.insert(
                    "source_facts",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("id", "booster:$version:fact:$index")
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
                    put("content", "Applied $assetPath")
                },
            )
            raw.setTransactionSuccessful()
        } finally {
            raw.endTransaction()
        }

        Log.i(
            TAG,
            "applied version=$version asset=$assetPath newEntities=$entityAdds facts=$factAdds relationships=$relationshipAdds",
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
            "SELECT id, description, externalRefsJson, aliasesJson, latitude, longitude, region FROM knowledge_entities WHERE normalizedName = ? ORDER BY enabled DESC, discoveryDepth ASC LIMIT 1"
        } else {
            "SELECT id, description, externalRefsJson, aliasesJson, latitude, longitude, region FROM knowledge_entities WHERE normalizedName = ? AND type = ? ORDER BY enabled DESC, discoveryDepth ASC LIMIT 1"
        }
        val args = if (type == null) arrayOf(normalizedName) else arrayOf(normalizedName, type)
        return db.query(sql, args).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ExistingEntity(
                id = cursor.getString(0),
                description = if (cursor.isNull(1)) null else cursor.getString(1),
                externalRefsJson = cursor.getString(2),
                aliasesJson = if (cursor.isNull(3)) "[]" else cursor.getString(3),
                latitude = if (cursor.isNull(4)) null else cursor.getDouble(4),
                longitude = if (cursor.isNull(5)) null else cursor.getDouble(5),
                region = if (cursor.isNull(6)) null else cursor.getString(6),
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
        if (existing.latitude == null && item.has("latitude") && !item.isNull("latitude")) {
            values.put("latitude", item.getDouble("latitude"))
        }
        if (existing.longitude == null && item.has("longitude") && !item.isNull("longitude")) {
            values.put("longitude", item.getDouble("longitude"))
        }
        item.optString("region").takeIf(String::isNotBlank)?.let { desiredRegion ->
            if (!existing.region.equals(desiredRegion, ignoreCase = true)) values.put("region", desiredRegion)
        }

        val mergedAliases = linkedMapOf<String, String>()
        runCatching { JSONArray(existing.aliasesJson.ifBlank { "[]" }) }.getOrNull()?.let { aliases ->
            for (index in 0 until aliases.length()) {
                aliases.optString(index).takeIf(String::isNotBlank)?.let { alias ->
                    mergedAliases.putIfAbsent(normalizeText(alias), alias)
                }
            }
        }
        item.optJSONArray("aliases")?.let { aliases ->
            for (index in 0 until aliases.length()) {
                aliases.optString(index).takeIf(String::isNotBlank)?.let { alias ->
                    mergedAliases.putIfAbsent(normalizeText(alias), alias)
                }
            }
        }
        values.put("aliasesJson", JSONArray(mergedAliases.values.toList()).toString())

        val refs = runCatching { JSONObject(existing.externalRefsJson.ifBlank { "{}" }) }.getOrElse { JSONObject() }
        val boosterRefs = boosterRefs(item)
        boosterRefs.keys().forEach { key -> refs.put(key, boosterRefs.get(key)) }
        values.put("externalRefsJson", refs.toString())

        db.update("knowledge_entities", SQLiteDatabase.CONFLICT_ABORT, values, "id = ?", arrayOf(existing.id))
    }

    private fun boosterRefs(item: JSONObject): JSONObject = JSONObject().apply {
        item.optString("packType").takeIf(String::isNotBlank)?.let { put("joharPackType", it) }
        item.optJSONObject("externalRefs")?.let { refs ->
            refs.keys().forEach { key -> put(key, refs.get(key)) }
        }
    }

    private fun ContentValues.putOptionalDouble(column: String, item: JSONObject, key: String) {
        if (item.has(key) && !item.isNull(key)) put(column, item.getDouble(key)) else putNull(column)
    }

    private fun JSONArray?.toJsonArrayString(): String = this?.toString() ?: "[]"

    private data class ExistingEntity(
        val id: String,
        val description: String?,
        val externalRefsJson: String,
        val aliasesJson: String,
        val latitude: Double?,
        val longitude: Double?,
        val region: String?,
    )
}
