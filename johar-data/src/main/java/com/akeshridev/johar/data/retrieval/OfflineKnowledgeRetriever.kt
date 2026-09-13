package com.akeshridev.johar.data.retrieval

import android.content.Context
import com.akeshridev.johar.data.crawl.normalizeText
import com.akeshridev.johar.data.local.JoharDatabaseProvider
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow

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
        val entities = dao.allEnabledEntities()
        val locationTokens = findLocationTokens(queryTokens, entities)
        val asksStateFact = asksStateFact(normalizedQuery)

        return entities
            .asSequence()
            .map { entity ->
                val facts = dao.factsForEntity(entity.id)
                val relationshipLocations = relationshipLocationTokens(entity)
                val score = score(
                    entity = entity,
                    facts = facts,
                    queryTokens = queryTokens,
                    locationTokens = locationTokens,
                    relationshipLocations = relationshipLocations,
                    preferredTypes = preferredTypes,
                    asksStateFact = asksStateFact,
                )
                OfflineKnowledgeHit(
                    entityId = entity.id,
                    name = entity.name,
                    type = entity.type,
                    description = entity.description,
                    score = score,
                    facts = rankFacts(facts, normalizedQuery, queryTokens),
                )
            }
            .filter { it.score > 0 }
            .sortedWith(
                compareByDescending<OfflineKnowledgeHit> { it.score }
                    .thenBy { it.name },
            )
            .take(limit)
            .toList()
    }

    private fun score(
        entity: KnowledgeEntityRow,
        facts: List<SourceFactRow>,
        queryTokens: Set<String>,
        locationTokens: Set<String>,
        relationshipLocations: Set<String>,
        preferredTypes: Set<String>,
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
                score += 24
            } else if (entity.type in GENERIC_LOCATION_TYPES) {
                score -= 8
            }
        }

        if (asksStateFact && entity.name.equals("Jharkhand", ignoreCase = true)) {
            score += 30
        }

        if (locationTokens.isNotEmpty()) {
            val directLocationMatch = locationTokens.any { token ->
                token in nameTokens || token in descriptionTokens || token in aliasTokens
            }
            val relationshipLocationMatch = locationTokens.any(relationshipLocations::contains)

            if (directLocationMatch || relationshipLocationMatch) {
                score += if (preferredTypes.contains(entity.type)) 24 else 10
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
                fact to score
            }
            .sortedByDescending { (_, score) -> score }

        val relevant = ranked.filter { (_, score) -> score > 0 }
        val selected = if (relevant.isNotEmpty()) relevant else ranked.take(3)
        return selected.take(5).map { (fact, _) -> toHitFact(fact) }
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

    companion object {
        private val GENERIC_LOCATION_TYPES = setOf("CITY", "TOWN", "DISTRICT", "REGION", "PLACE")

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
