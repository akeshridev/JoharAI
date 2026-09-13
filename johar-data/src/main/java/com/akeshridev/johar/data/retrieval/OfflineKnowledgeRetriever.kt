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
        val asksStateFact = normalizedQuery.contains("state animal") ||
            normalizedQuery.contains("state bird") ||
            normalizedQuery.contains("state tree") ||
            normalizedQuery.contains("state flower")

        return dao.allEnabledEntities()
            .asSequence()
            .map { entity ->
                val facts = dao.factsForEntity(entity.id)
                val score = score(
                    entity = entity,
                    facts = facts,
                    queryTokens = queryTokens,
                    preferredTypes = preferredTypes,
                    asksStateFact = asksStateFact,
                )
                OfflineKnowledgeHit(
                    entityId = entity.id,
                    name = entity.name,
                    type = entity.type,
                    description = entity.description,
                    score = score,
                    facts = facts.map(::toHitFact),
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
        preferredTypes: Set<String>,
        asksStateFact: Boolean,
    ): Int {
        val name = normalizeText(entity.name)
        val description = normalizeText(entity.description.orEmpty())
        val aliases = normalizeText(entity.aliasesJson)
        val factText = normalizeText(
            facts.joinToString(" ") { fact ->
                listOfNotNull(
                    fact.field,
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
        if (preferredTypes.contains(entity.type)) score += 10

        if (asksStateFact && entity.name.equals("Jharkhand", ignoreCase = true)) {
            score += 30
        }

        if (queryTokens.contains("deoghar") &&
            (description.contains("deoghar") || aliases.contains("deoghar") || name.contains("deoghar"))
        ) {
            score += 12
        }

        if (queryTokens.contains("ranchi") &&
            (description.contains("ranchi") || aliases.contains("ranchi") || name.contains("ranchi"))
        ) {
            score += 12
        }

        return score
    }

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
