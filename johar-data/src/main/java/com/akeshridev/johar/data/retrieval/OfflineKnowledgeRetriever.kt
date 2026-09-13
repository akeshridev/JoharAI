package com.akeshridev.johar.data.retrieval

import android.content.Context
import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow
import org.json.JSONObject

class OfflineKnowledgeRetriever(context: Context) {
    private val dao = JoharDatabaseProvider.get(context.applicationContext).knowledgeDao()

    fun retrieve(query: String, limit: Int = 5): List<OfflineKnowledgeHit> {
        val normalizedQuery = normalizeText(query)
        val queryTokens = tokens(normalizedQuery)
        val entities = dao.allEnabledEntities()
        val locationTokens = findLocationTokens(queryTokens, entities)
        val districtTokens = entities.filter { it.type == "DISTRICT" }
            .flatMap { tokens(normalizeText(it.name)) }.toSet()
        val preferredTypes = preferredTypes(normalizedQuery)
        val preferredPackTypes = preferredPackTypes(normalizedQuery)
        val asksStateFact = containsAny(normalizedQuery, "state animal", "state bird", "state tree", "state flower")

        return entities.asSequence()
            .map { entity ->
                val facts = dao.factsForEntity(entity.id)
                val packType = packType(entity)
                val locationMatch = locationMatch(entity, locationTokens, districtTokens)
                ScoredEntity(
                    hit = OfflineKnowledgeHit(
                        entityId = entity.id,
                        name = entity.name,
                        type = entity.type,
                        packType = packType,
                        description = entity.description,
                        score = score(
                            entity, packType, facts, queryTokens, locationTokens, locationMatch,
                            preferredTypes, preferredPackTypes, asksStateFact,
                        ),
                        facts = rankFacts(facts, normalizedQuery, queryTokens),
                    ),
                    locationMatch = locationMatch,
                )
            }
            .filter { candidate ->
                candidate.hit.score > 0 &&
                    (locationTokens.isEmpty() || preferredPackTypes.isEmpty() ||
                        (candidate.locationMatch && candidate.hit.packType in preferredPackTypes))
            }
            .map { it.hit }
            .sortedWith(compareByDescending<OfflineKnowledgeHit> { it.score }.thenBy { it.name })
            .take(limit)
            .toList()
    }

    private fun locationMatch(
        entity: KnowledgeEntityRow,
        locationTokens: Set<String>,
        districtTokens: Set<String>,
    ): Boolean {
        if (locationTokens.isEmpty()) return true

        val relationLocations = dao.relationshipsFromEntity(entity.id)
            .asSequence()
            .filter { it.predicate in LOCATION_RELATIONSHIPS }
            .mapNotNull { dao.getEntity(it.toEntityId) }
            .flatMap { tokens(normalizeText(it.name)).asSequence() }
            .toSet()
        if (relationLocations.isNotEmpty()) return locationTokens.any(relationLocations::contains)

        val name = tokens(normalizeText(entity.name))
        val aliases = tokens(normalizeText(entity.aliasesJson))
        if (locationTokens.any { it in name || it in aliases }) return true

        val description = normalizeText(entity.description.orEmpty())
        val explicitDistricts = districtTokens.filterTo(mutableSetOf()) { district ->
            description.contains("located in $district") || description.contains("$district district")
        }
        if (explicitDistricts.isNotEmpty()) return locationTokens.any(explicitDistricts::contains)

        return locationTokens.any { location ->
            description.contains("near $location") ||
                description.contains("around $location") ||
                description.contains("in $location") ||
                description.contains("$location district") ||
                description.contains("$location city") ||
                description.contains("$location purulia")
        }
    }

    private fun score(
        entity: KnowledgeEntityRow,
        packType: String?,
        facts: List<SourceFactRow>,
        queryTokens: Set<String>,
        locationTokens: Set<String>,
        locationMatch: Boolean,
        preferredTypes: Set<String>,
        preferredPackTypes: Set<String>,
        asksStateFact: Boolean,
    ): Int {
        val nameTokens = tokens(normalizeText(entity.name))
        val descriptionTokens = tokens(normalizeText(entity.description.orEmpty()))
        val aliasTokens = tokens(normalizeText(entity.aliasesJson))
        val factTokens = tokens(normalizeText(facts.joinToString(" ") {
            listOfNotNull(splitFieldName(it.field), it.textValue, it.numberValue?.toString(), it.booleanValue?.toString())
                .joinToString(" ")
        }))

        var score = 0
        score += queryTokens.intersect(nameTokens).size * 12
        score += queryTokens.intersect(aliasTokens).size * 6
        score += queryTokens.intersect(descriptionTokens).size * 4
        score += queryTokens.intersect(factTokens).size * 5
        if (queryTokens.isNotEmpty() && nameTokens.containsAll(queryTokens)) score += 20
        if (entity.type in preferredTypes) score += 18
        else if (preferredTypes.isNotEmpty() && entity.type in GENERIC_LOCATION_TYPES) score -= 8

        if (preferredPackTypes.isNotEmpty()) {
            if (packType in preferredPackTypes) score += 36
            else if (packType in CONTRASTING_PACK_TYPES) score -= 18
        }
        if (asksStateFact && entity.name.equals("Jharkhand", true)) score += 30
        if (locationTokens.isNotEmpty()) {
            if (locationMatch) score += if (packType in preferredPackTypes) 30 else 10
            else if (preferredPackTypes.isNotEmpty()) score -= 24
        }
        return score
    }

    private fun rankFacts(
        facts: List<SourceFactRow>,
        query: String,
        queryTokens: Set<String>,
    ): List<OfflineKnowledgeFact> {
        val ranked = facts.map { fact ->
            val field = normalizeText(splitFieldName(fact.field))
            val value = normalizeText(listOfNotNull(fact.textValue, fact.numberValue?.toString(), fact.booleanValue?.toString()).joinToString(" "))
            var score = queryTokens.intersect(tokens(field)).size * 12 + queryTokens.intersect(tokens(value)).size * 3
            score += when {
                query.contains("state animal") && field.contains("state animal") -> 40
                query.contains("state bird") && field.contains("state bird") -> 40
                query.contains("state tree") && field.contains("state tree") -> 40
                query.contains("state flower") && field.contains("state flower") -> 40
                else -> 0
            }
            fact to score
        }.sortedByDescending { it.second }

        val selected = ranked.filter { it.second > 0 }.ifEmpty { ranked.take(3) }
        return selected.asSequence().map { toHitFact(it.first) }
            .distinctBy { normalizeText("${it.field}:${it.value}") }
            .take(5).toList()
    }

    private fun findLocationTokens(queryTokens: Set<String>, entities: List<KnowledgeEntityRow>): Set<String> {
        val known = entities.asSequence()
            .filter { it.type in GENERIC_LOCATION_TYPES }
            .flatMap { tokens(normalizeText(it.name)).asSequence() }
            .toSet()
        return queryTokens.intersect(known)
    }

    private fun preferredTypes(query: String): Set<String> = buildSet {
        if (containsAny(query, "rugra", "food", "dish", "cuisine", "khana")) add("FOOD")
        if (containsAny(query, "festival", "parab", "mela")) add("FESTIVAL")
        if (containsAny(query, "temple", "mandir", "dham", "pilgrimage")) add("TOURIST_ATTRACTION")
        if (containsAny(query, "waterfall", "falls", "jharna")) add("NATURAL_FEATURE")
        if (query.contains("river")) add("RIVER")
        if (query.contains("city")) add("CITY")
        if (query.contains("district")) add("DISTRICT")
    }

    private fun preferredPackTypes(query: String): Set<String> = buildSet {
        if (containsAny(query, "temple", "mandir", "dham")) add("TEMPLE")
        if (query.contains("pilgrimage")) add("PILGRIMAGE")
        if (containsAny(query, "waterfall", "falls", "jharna")) add("WATERFALL")
        if (containsAny(query, "dam", "reservoir")) add("DAM")
        if (query.contains("lake")) add("LAKE")
        if (query.contains("forest")) add("FOREST")
        if (containsAny(query, "wildlife", "sanctuary")) add("WILDLIFE_SANCTUARY")
        if (query.contains("tiger reserve")) add("TIGER_RESERVE")
    }

    private fun packType(entity: KnowledgeEntityRow): String? = runCatching {
        JSONObject(entity.externalRefsJson).optString("joharPackType").takeIf(String::isNotBlank)
    }.getOrNull()

    private fun toHitFact(fact: SourceFactRow) = OfflineKnowledgeFact(
        field = fact.field,
        value = fact.textValue ?: fact.numberValue?.let { if (fact.unit.isNullOrBlank()) it.toString() else "$it ${fact.unit}" }
            ?: fact.booleanValue?.toString().orEmpty(),
        freshness = fact.freshness,
        sourceUrl = fact.sourceUrl,
    )

    private fun splitFieldName(value: String) = value.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ')
    private fun containsAny(text: String, vararg terms: String) = terms.any(text::contains)
    private fun tokens(text: String): Set<String> = text.split(' ').asSequence()
        .map(String::trim).filter { it.length >= 2 }.filterNot(STOP_WORDS::contains).toSet()

    private data class ScoredEntity(val hit: OfflineKnowledgeHit, val locationMatch: Boolean)

    companion object {
        private val LOCATION_RELATIONSHIPS = setOf("LOCATED_IN_DISTRICT", "SUBDIVISION_OF", "PART_OF_JHARKHAND")
        private val GENERIC_LOCATION_TYPES = setOf("CITY", "TOWN", "DISTRICT", "REGION", "PLACE")
        private val CONTRASTING_PACK_TYPES = setOf(
            "DAM", "LAKE", "WATERFALL", "TEMPLE", "PILGRIMAGE", "ASHRAM", "MUSEUM",
            "HERITAGE_SITE", "VIEWPOINT", "PARK", "FOREST", "WILDLIFE_SANCTUARY", "TIGER_RESERVE", "NATIONAL_PARK",
        )
        private val STOP_WORDS = setOf(
            "what", "is", "are", "the", "a", "an", "of", "in", "near", "nearby", "me", "tell", "about",
            "ka", "ki", "ke", "kya", "hai", "mein", "paas", "jharkhand",
        )
    }
}

data class OfflineKnowledgeHit(
    val entityId: String,
    val name: String,
    val type: String,
    val packType: String?,
    val description: String?,
    val score: Int,
    val facts: List<OfflineKnowledgeFact>,
)

data class OfflineKnowledgeFact(
    val field: String,
    val value: String,
    val freshness: String,
    val sourceUrl: String,
)
