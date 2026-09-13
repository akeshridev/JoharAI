package com.akeshridev.johar.data.retrieval

import android.content.Context
import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow
import org.json.JSONObject

class OfflineKnowledgeRetriever(
    context: Context,
) {
    private val dao = JoharDatabaseProvider.get(context.applicationContext).knowledgeDao()

    fun retrieve(
        query: String,
        limit: Int = 5,
    ): List<OfflineKnowledgeHit> {
        val normalizedQuery = normalizeText(query)
        val queryTokens = meaningfulTokens(normalizedQuery)
        val preferredTypes = preferredTypes(normalizedQuery)
        val preferredPackTypes = preferredPackTypes(normalizedQuery)
        val entities = dao.allEnabledEntities()
        val locationTokens = findLocationTokens(queryTokens, entities)
        val asksStateFact = asksStateFact(normalizedQuery)

        return entities
            .asSequence()
            .map { entity ->
                val facts = dao.factsForEntity(entity.id)
                val relationshipLocations = relationshipLocationTokens(entity)
                val packType = packType(entity)
                val locationMatch = locationMatch(
                    entity = entity,
                    locationTokens = locationTokens,
                    relationshipLocations = relationshipLocations,
                )
                val score = score(
                    entity = entity,
                    packType = packType,
                    facts = facts,
                    queryTokens = queryTokens,
                    locationTokens = locationTokens,
                    locationMatch = locationMatch,
                    preferredTypes = preferredTypes,
                    preferredPackTypes = preferredPackTypes,
                    asksStateFact = asksStateFact,
                )
                ScoredEntity(
                    hit = OfflineKnowledgeHit(
                        entityId = entity.id,
                        name = entity.name,
                        type = entity.type,
                        packType = packType,
                        description = entity.description,
                        score = score,
                        facts = rankFacts(facts, normalizedQuery, queryTokens),
                    ),
                    locationMatch = locationMatch,
                )
            }
            .filter { candidate ->
                candidate.hit.score > 0 &&
                    passesExplicitLocationConstraint(
                        candidate = candidate,
                        locationTokens = locationTokens,
                        preferredPackTypes = preferredPackTypes,
                    )
            }
            .map(ScoredEntity::hit)
            .sortedWith(
                compareByDescending<OfflineKnowledgeHit> { it.score }
                    .thenBy { it.name },
            )
            .take(limit)
            .toList()
    }

    private fun passesExplicitLocationConstraint(
        candidate: ScoredEntity,
        locationTokens: Set<String>,
        preferredPackTypes: Set<String>,
    ): Boolean {
        if (locationTokens.isEmpty() || preferredPackTypes.isEmpty()) return true
        return candidate.locationMatch && candidate.hit.packType in preferredPackTypes
    }

    private fun locationMatch(
        entity: KnowledgeEntityRow,
        locationTokens: Set<String>,
        relationshipLocations: Set<String>,
    ): Boolean {
        if (locationTokens.isEmpty()) return true

        if (relationshipLocations.isNotEmpty()) {
            return locationTokens.any(relationshipLocations::contains)
        }

        val nameTokens = meaningfulTokens(normalizeText(entity.name))
        val descriptionTokens = meaningfulTokens(normalizeText(entity.description.orEmpty()))
        val aliasTokens = meaningfulTokens(normalizeText(entity.aliasesJson))
        return locationTokens.any { token ->
            token in nameTokens || token in descriptionTokens || token in aliasTokens
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
        val name = normalizeText(entity.name)
        val description = normalizeText(entity.description.orEmpty())
        val aliases = normalizeText(entity.aliasesJson)
        val factText = normalizeText(
            facts.joinToString(" ") { fact ->
                listOfNotNull(
                    splitFieldName(fact.field),
                    fact.textValue,
                    fact.numberValue?.toString(),
                    fact.booleanValue?.toString(),
                ).joinToString(" ")
            },
        )

        val nameTokens = meaningfulTokens(name)
        val descriptionTokens = meaningfulTokens(description)
        val aliasTokens = meaningfulTokens(aliases)
        val factTokens = meaningfulTokens(factText)

        var score = 0
        score += queryTokens.intersect(nameTokens).size * 12
        score += queryTokens.intersect(aliasTokens).size * 6
        score += queryTokens.intersect(descriptionTokens).size * 4
        score += queryTokens.intersect(factTokens).size * 5

        if (queryTokens.isNotEmpty() && nameTokens.containsAll(queryTokens)) score += 20

        if (preferredTypes.isNotEmpty()) {
            if (preferredTypes.contains(entity.type)) {
                score += 18
            } else if (entity.type in GENERIC_LOCATION_TYPES) {
                score -= 8
            }
        }

        if (preferredPackTypes.isNotEmpty()) {
            when {
                packType != null && packType in preferredPackTypes -> score += 36
                packType != null && packType in CONTRASTING_PACK_TYPES -> score -= 18
                else -> Unit
            }
        }

        if (asksStateFact && entity.name.equals("Jharkhand", ignoreCase = true)) {
            score += 30
        }

        if (locationTokens.isNotEmpty()) {
            if (locationMatch) {
                score += when {
                    packType != null && packType in preferredPackTypes -> 30
                    preferredTypes.contains(entity.type) -> 20
                    else -> 10
                }
            } else if (preferredPackTypes.isNotEmpty()) {
                score -= 24
            }
        }

        return score
    }

    private fun relationshipLocationTokens(entity: KnowledgeEntityRow): Set<String> =
        dao.relationshipsFromEntity(entity.id)
            .asSequence()
            .filter { relationship ->
                relationship.predicate == "LOCATED_IN_DISTRICT" ||
                    relationship.predicate == "SUBDIVISION_OF" ||
                    relationship.predicate == "PART_OF_JHARKHAND"
            }
            .mapNotNull { relationship -> dao.getEntity(relationship.toEntityId) }
            .flatMap { target -> meaningfulTokens(normalizeText(target.name)).asSequence() }
            .toSet()

    private fun findLocationTokens(
        queryTokens: Set<String>,
        entities: List<KnowledgeEntityRow>,
    ): Set<String> {
        val knownLocations = entities
            .asSequence()
            .filter { it.type in GENERIC_LOCATION_TYPES }
            .flatMap { entity -> meaningfulTokens(normalizeText(entity.name)).asSequence() }
            .toSet()
        return queryTokens.intersect(knownLocations)
    }

    private fun rankFacts(
        facts: List<SourceFactRow>,
        normalizedQuery: String,
        queryTokens: Set<String>,
    ): List<OfflineKnowledgeFact> {
        val ranked = facts
            .map { fact ->
                val fieldText = normalizeText(splitFieldName(fact.field))
                val valueText = normalizeText(
                    listOfNotNull(
                        fact.textValue,
                        fact.numberValue?.toString(),
                        fact.booleanValue?.toString(),
                    ).joinToString(" "),
                )
                val fieldTokens = meaningfulTokens(fieldText)
                val valueTokens = meaningfulTokens(valueText)
                var score = queryTokens.intersect(fieldTokens).size * 12
                score += queryTokens.intersect(valueTokens).size * 3
                score += semanticFactBoost(normalizedQuery, fieldText)
                RankedFact(fact, score)
            }
            .sortedByDescending(RankedFact::score)

        val relevant = ranked.filter { it.score > 0 }
        val selected = if (relevant.isNotEmpty()) relevant else ranked.take(3)

        return selected
            .asSequence()
            .map { rankedFact -> toHitFact(rankedFact.fact) }
            .distinctBy { fact -> normalizeText("${fact.field}:${fact.value}") }
            .take(5)
            .toList()
    }

    private fun semanticFactBoost(query: String, field: String): Int = when {
        containsAny(query, "state animal") && field.contains("state animal") -> 40
        containsAny(query, "state bird") && field.contains("state bird") -> 40
        containsAny(query, "state tree") && field.contains("state tree") -> 40
        containsAny(query, "state flower") && field.contains("state flower") -> 40
        containsAny(query, "district", "districts") && field.contains("district") -> 24
        containsAny(query, "division", "divisions") && field.contains("division") -> 24
        containsAny(query, "language", "languages") && field.contains("language") -> 24
        else -> 0
    }

    private fun asksStateFact(query: String): Boolean =
        containsAny(query, "state animal", "state bird", "state tree", "state flower")

    private fun splitFieldName(value: String): String = value
        .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace('_', ' ')

    private fun preferredTypes(query: String): Set<String> = buildSet {
        if (containsAny(query, "rugra", "food", "dish", "cuisine", "khana")) add("FOOD")
        if (containsAny(query, "festival", "parab", "mela")) add("FESTIVAL")
        if (containsAny(query, "temple", "mandir", "dham", "pilgrimage")) add("TOURIST_ATTRACTION")
        if (containsAny(query, "waterfall", "falls", "jharna")) add("NATURAL_FEATURE")
        if (containsAny(query, "river")) add("RIVER")
        if (containsAny(query, "city")) add("CITY")
        if (containsAny(query, "district")) add("DISTRICT")
    }

    private fun preferredPackTypes(query: String): Set<String> = buildSet {
        if (containsAny(query, "temple", "mandir", "dham")) add("TEMPLE")
        if (containsAny(query, "pilgrimage")) add("PILGRIMAGE")
        if (containsAny(query, "waterfall", "falls", "jharna")) add("WATERFALL")
        if (containsAny(query, "dam", "reservoir")) add("DAM")
        if (containsAny(query, "lake")) add("LAKE")
        if (containsAny(query, "forest")) add("FOREST")
        if (containsAny(query, "wildlife", "sanctuary")) add("WILDLIFE_SANCTUARY")
        if (containsAny(query, "tiger reserve")) add("TIGER_RESERVE")
    }

    private fun packType(entity: KnowledgeEntityRow): String? = runCatching {
        JSONObject(entity.externalRefsJson)
            .optString("joharPackType")
            .takeIf(String::isNotBlank)
    }.getOrNull()

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }

    private fun meaningfulTokens(text: String): Set<String> = text
        .split(' ')
        .asSequence()
        .map(String::trim)
        .filter { it.length >= 2 }
        .filterNot(STOP_WORDS::contains)
        .toSet()

    private fun toHitFact(fact: SourceFactRow): OfflineKnowledgeFact = OfflineKnowledgeFact(
        field = fact.field,
        value = fact.textValue
            ?: fact.numberValue?.let { number ->
                if (fact.unit.isNullOrBlank()) number.toString() else "$number ${fact.unit}"
            }
            ?: fact.booleanValue?.toString()
            ?: "",
        freshness = fact.freshness,
        sourceUrl = fact.sourceUrl,
    )

    private data class RankedFact(
        val fact: SourceFactRow,
        val score: Int,
    )

    private data class ScoredEntity(
        val hit: OfflineKnowledgeHit,
        val locationMatch: Boolean,
    )

    companion object {
        private val GENERIC_LOCATION_TYPES = setOf("CITY", "TOWN", "DISTRICT", "REGION", "PLACE")
        private val CONTRASTING_PACK_TYPES = setOf(
            "DAM", "LAKE", "WATERFALL", "TEMPLE", "PILGRIMAGE", "ASHRAM", "MUSEUM",
            "HERITAGE_SITE", "VIEWPOINT", "PARK", "FOREST", "WILDLIFE_SANCTUARY",
            "TIGER_RESERVE", "NATIONAL_PARK",
        )

        private val STOP_WORDS = setOf(
            "what", "is", "are", "the", "a", "an", "of", "in", "near", "nearby",
            "me", "tell", "about", "ka", "ki", "ke", "kya", "hai", "mein", "me",
            "paas", "jharkhand",
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
