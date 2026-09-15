package com.akeshridev.johar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.akeshridev.johar.conversation.*
import android.database.sqlite.SQLiteDatabase
import com.akeshridev.johar.data.retrieval.*
import com.akeshridev.johar.data.routing.RanchiOfflineRouter
import com.akeshridev.johar.data.routing.RanchiRouteResult
import com.akeshridev.johar.data.spatial.*
import com.akeshridev.johar.ui.model.JoharContent
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Pipeline evaluation, not a UI automation benchmark. All diagnostics stay in androidTest. */
@RunWith(AndroidJUnit4::class)
class Golden100EvaluationTest {
    @Test fun evaluateGolden100() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val args = InstrumentationRegistry.getArguments()
        val start = args.getString("goldenStart")?.toInt() ?: 1
        val end = args.getString("goldenEnd")?.toInt() ?: 100
        val cases = instrumentation.context.assets.open("golden-100.jsonl").bufferedReader()
            .useLines { it.filter(String::isNotBlank).map(::JSONObject).toList() }
        check(cases.size == 100 && start in 1..100 && end in start..100)
        val spatial = RanchiSpatialEngine(context)
        val road = RanchiOfflineRouter(context)
        var retrievalTrace: OfflineRetrievalTrace? = null
        val retriever = OfflineKnowledgeRetriever(context) { retrievalTrace = it }
        val generator = DeterministicJoharAnswerGenerator(retriever)
        // Read-only provenance inspection is test-owned; keep Room internals private.
        SQLiteDatabase.openDatabase(context.getDatabasePath("johar.db").path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            fun sourceUrls(id: String): List<String> = db.rawQuery(
                "SELECT DISTINCT sourceUrl FROM source_facts WHERE entityId = ?", arrayOf(id),
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            fun count(table: String): Long = db.rawQuery("SELECT COUNT(*) FROM $table", null).use {
                it.moveToFirst(); it.getLong(0)
            }
            val snapshot = JSONObject().put("startedAtEpochMillis", System.currentTimeMillis())
                .put("entityCount", count("knowledge_entities")).put("factCount", count("source_facts"))
                .put("database", "working johar.db; normal seed/booster initialization")
            val calls = mutableListOf<JSONObject>()
            var roadResult: RanchiRouteResult? = null
            fun record(kind: String, input: String, places: List<RanchiSpatialPlace>): List<RanchiSpatialPlace> {
                calls += JSONObject().put("kind", kind).put("input", input)
                    .put("candidates", JSONArray(places.map { p -> placeJson(p).put("sourceCount", sourceUrls(p.id).size) }))
                return places
            }
            // Same dependencies/limits as JoharGraph; wrappers only observe executed calls.
            val router = JoharQueryRouter(
                resolvePlace = { record("resolvePlace", it, spatial.resolvePlace(it, limit = 20)) },
                nearby = { origin, types -> record("nearby", "$origin types=$types",
                    spatial.nearby(origin, radiusKm = 5.0, types = types, limit = 100)) },
                discoverPlaces = { record("categoryDiscovery", it.sorted().joinToString(), spatial.discoverPlaces(it)) },
                knowledgeAnswer = {
                    calls += JSONObject().put("kind", "knowledgeAnswer").put("input", it).put("candidates", JSONArray())
                    generator.answer(it)
                },
                routeInstalled = { road.isInstalled() },
                route = { from, to -> road.route(from, to).also { roadResult = it } },
            )
            val output = File(context.filesDir, "golden-100-diagnostics.jsonl")
            output.bufferedWriter().use { writer ->
                cases.subList(start - 1, end).forEach { case ->
                    router.resetConversationState()
                    retrievalTrace = null
                    roadResult = null
                    calls.clear()
                    val query = case.getString("query")
                    val began = android.os.SystemClock.elapsedRealtime()
                    val result = router.answer(query)
                    val content = JoharContentMapper.map(result)
                    val elapsed = android.os.SystemClock.elapsedRealtime() - began
                    val trace = retrievalTrace
                    val evidence = (result as? JoharQueryResult.Grounded)?.answer?.evidence.orEmpty()
                    val places = when (result) {
                        is JoharQueryResult.Places -> result.places
                        is JoharQueryResult.Route -> listOf(result.origin, result.destination)
                        is JoharQueryResult.Comparison -> listOf(result.left, result.right)
                        is JoharQueryResult.Itinerary -> result.places
                        else -> emptyList()
                    }
                    val selected = places.map(::placeJson) + evidence.map(::hitJson)
                    val ids = selected.map { it.getString("entityId") }
                    val sources = ids.flatMap { id -> sourceUrls(id) }.distinct()
                    val normalized = trace?.normalizedQuery
                    val fallback = trace?.fallbackReason ?: when {
                        result is JoharQueryResult.Clarification -> "ORIGIN_OR_ENDPOINT_CLARIFICATION"
                        result is JoharQueryResult.Grounded && result.answer.mode == JoharAnswerMode.NO_ANSWER -> "NO_SUPPORTED_OFFLINE_ANSWER"
                        else -> null
                    }
                    val record = JSONObject()
                        .put("id", case.getString("id")).put("query", query).put("runtimeSnapshot", snapshot)
                        .put("normalizedQuery", normalized ?: JSONObject.NULL)
                        .put("normalizationStage", if (trace == null) "retrieval_not_called" else "retrieval")
                        .put("detectedIntent", JSONArray(calls.map { it.getString("kind") }.distinct()))
                        .put("detectedCategory", JSONArray((trace?.preferredTypes.orEmpty() + calls.filter { it.getString("kind") == "categoryDiscovery" }.flatMap { it.getString("input").split(", ") }).sorted()))
                        .put("aliasesApplied", JSONObject(trace?.matchedAliases.orEmpty()))
                        .put("aliasNormalizationChanged", normalized?.let { it != query.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim() } ?: JSONObject.NULL)
                        .put("classification", JSONObject()
                            .put("routeExecuted", roadResult != null)
                            .put("nearbyExecuted", calls.any { it.getString("kind") == "nearby" })
                            .put("liveGuard", result is JoharQueryResult.Grounded && result.tone == GroundedTone.NOT_CONFIRMED))
                        .put("retrievalCandidates", JSONArray(trace?.candidates.orEmpty().map { hit ->
                            hitJson(hit).put("matchedAliases", JSONArray(trace?.matchedAliases?.get(hit.entityId).orEmpty()))
                                .put("matchedBy", if (trace?.matchedAliases?.containsKey(hit.entityId) == true) "stored_alias" else "lexical_and_type_score")
                        }))
                        .put("spatialCalls", JSONArray(calls))
                        .put("selectedEntities", JSONArray(selected))
                        .put("sources", JSONArray(sources.map { url -> JSONObject().put("url", url)
                            .put("name", runCatching { java.net.URI(url).host }.getOrNull() ?: url) }))
                        .put("evidence", JSONArray(evidence.flatMap { hit -> hit.facts.map { fact ->
                            JSONObject().put("entityId", hit.entityId).put("field", fact.field)
                                .put("value", fact.value).put("sourceUrl", fact.sourceUrl).put("freshness", fact.freshness)
                        } }))
                        .put("finalResultType", content.javaClass.simpleName)
                        .put("finalVisibleAnswer", visible(content))
                        .put("visibleSourceNames", JSONArray((content as? JoharContent.Grounded)?.sources.orEmpty().map { it.sourceName }))
                        .put("answerMode", (result as? JoharQueryResult.Grounded)?.answer?.mode?.name ?: JSONObject.NULL)
                        .put("fallbackReason", fallback ?: JSONObject.NULL)
                        .put("elapsedMs", elapsed)
                        .put("routeEvidence", when (val r = roadResult) {
                            is RanchiRouteResult.Success -> JSONObject().put("distanceMeters", r.distanceMeters)
                                .put("durationSeconds", r.durationSeconds).put("pointCount", r.points.size)
                            is RanchiRouteResult.Unavailable -> JSONObject().put("reason", r.reason)
                            else -> JSONObject.NULL
                        })
                        .put("verdict", "UNREVIEWED")
                        .put("notes", JSONArray(listOf("Actual router + mapped content; no Activity rendering.",
                            "Normalization is null when retrieval was not called. Candidate traces contain at most 20 ranked retrieval hits.",
                            "Spatial record sources are provenance, not proof of every practical claim.")))
                    writer.appendLine(record.toString())
                    writer.flush()
                }
            }
        }
    }

    private fun placeJson(p: RanchiSpatialPlace) = JSONObject().put("entityId", p.id).put("name", p.name)
        .put("type", p.type).put("distanceKm", p.distanceKm ?: JSONObject.NULL)
        .put("latitude", p.coordinate.latitude).put("longitude", p.coordinate.longitude)

    private fun hitJson(h: OfflineKnowledgeHit) = JSONObject().put("entityId", h.entityId).put("name", h.name)
        .put("type", h.type).put("packType", h.packType ?: JSONObject.NULL).put("score", h.score)
        .put("sourceCount", h.facts.map { it.sourceUrl }.distinct().size)

    private fun visible(c: JoharContent): String = when (c) {
        is JoharContent.Grounded -> c.text
        is JoharContent.Places -> listOfNotNull(c.intro).plus(c.items.flatMap { listOfNotNull(it.name, it.area, it.description) + it.metadata }).joinToString("\n")
        is JoharContent.Utilities -> listOfNotNull(c.intro).plus(c.items.flatMap { listOfNotNull(it.title, it.subtitle, it.metadata) }).joinToString("\n")
        is JoharContent.Clarification -> (listOf(c.prompt) + c.options).joinToString("\n")
        is JoharContent.Route -> (listOf(c.route.title, c.route.durationLabel, c.route.distanceLabel) +
            c.route.stops.flatMap { listOfNotNull(it.title, it.subtitle, it.trailingLabel) } + c.route.status).joinToString("\n")
        else -> error("Golden set needs an explicit visible-text serializer for ${c.javaClass.simpleName}")
    }
}
