package com.akeshridev.johar.data.retrieval

import com.akeshridev.johar.data.crawl.normalizeText

interface JoharAnswerGenerator {
    fun answer(query: String): JoharAnswer
}

class DeterministicJoharAnswerGenerator(
    private val retriever: OfflineKnowledgeRetriever,
) : JoharAnswerGenerator {

    override fun answer(query: String): JoharAnswer {
        val normalized = normalizeText(query)
        val hits = retriever.retrieve(query, limit = 5)
        if (hits.isEmpty()) {
            return JoharAnswer(
                text = if (looksHinglish(normalized)) {
                    "Offline knowledge pack me is sawal ke liye enough information nahi hai."
                } else {
                    "The offline knowledge pack does not have enough information for this question."
                },
                evidence = emptyList(),
                mode = JoharAnswerMode.NO_ANSWER,
            )
        }

        stateFactAnswer(normalized, hits)?.let { return it }
        requestedFactAnswer(normalized, hits)?.let { return it }
        listAnswer(normalized, hits)?.let { return it }

        val top = hits.first()
        val text = top.description
            ?.takeIf(String::isNotBlank)
            ?.let { description -> "${top.name}: $description" }
            ?: top.facts.firstOrNull()?.let { fact -> "${top.name}: ${humanField(fact.field)} = ${fact.value}" }
            ?: top.name

        return JoharAnswer(
            text = text,
            evidence = listOf(top),
            mode = JoharAnswerMode.DETERMINISTIC,
        )
    }

    private fun stateFactAnswer(
        query: String,
        hits: List<OfflineKnowledgeHit>,
    ): JoharAnswer? {
        val requestedField = when {
            containsAny(query, "state animal", "rajya pashu", "rajya jaanwar", "official animal") -> "state animal"
            containsAny(query, "state bird", "rajya pakshi") -> "state bird"
            containsAny(query, "state tree", "rajya vriksh") -> "state tree"
            containsAny(query, "state flower", "rajya phool") -> "state flower"
            else -> return null
        }

        val evidenceHit = hits.firstOrNull { hit ->
            hit.facts.any { fact -> normalizeText(splitFieldName(fact.field)).contains(requestedField) }
        } ?: return null
        val fact = evidenceHit.facts.first { fact ->
            normalizeText(splitFieldName(fact.field)).contains(requestedField)
        }

        val text = if (looksHinglish(query)) {
            "Jharkhand ka $requestedField ${fact.value} hai."
        } else {
            "Jharkhand's $requestedField is ${fact.value}."
        }

        return JoharAnswer(
            text = text,
            evidence = listOf(evidenceHit),
            mode = JoharAnswerMode.DETERMINISTIC,
        )
    }

    /**
     * Detail questions must answer from a matching fact instead of turning a named
     * place into a broad category list (for example, stairs at Pahari Mandir).
     */
    private fun requestedFactAnswer(
        query: String,
        hits: List<OfflineKnowledgeHit>,
    ): JoharAnswer? {
        val requested = DETAIL_QUERY_TERMS.firstOrNull { (queryTerm, _) -> query.contains(queryTerm) } ?: return null
        val factTerms = requested.second
        val match = hits.asSequence().mapNotNull { hit ->
            val fact = hit.facts.firstOrNull { fact ->
                val searchable = normalizeText("${splitFieldName(fact.field)} ${fact.value}")
                factTerms.any(searchable::contains)
            }
            fact?.let { hit to it }
        }.firstOrNull()

        if (match != null) {
            val (hit, fact) = match
            return JoharAnswer(
                text = "${hit.name}: ${humanField(fact.field)} — ${fact.value}",
                evidence = listOf(hit),
                mode = JoharAnswerMode.DETERMINISTIC,
            )
        }

        // We may know the entity but not the requested attribute. Say that explicitly;
        // never substitute a different attraction or invent an accessibility/current fact.
        val top = hits.first()
        val label = requested.first
        return JoharAnswer(
            text = if (looksHinglish(query)) {
                "${top.name} offline data mein mila, lekin '$label' ke baare mein source-backed information confirm nahi hai."
            } else {
                "${top.name} is in the offline data, but there is no source-backed information confirming '$label'."
            },
            evidence = listOf(top),
            mode = JoharAnswerMode.NO_ANSWER,
        )
    }

    /** Lists only broad category requests. Named-entity questions stay entity-specific. */
    private fun listAnswer(
        query: String,
        hits: List<OfflineKnowledgeHit>,
    ): JoharAnswer? {
        val category = listCategory(query) ?: return null
        if (!isBroadCategoryQuery(query, category.queryTerms)) return null

        val relevant = hits.filter { hit -> hit.packType in category.packTypes || hit.type in category.entityTypes }
            .ifEmpty { hits }
            .take(5)
        val names = relevant.map { it.name }.distinct()
        if (names.isEmpty()) return null

        val joined = joinNames(names)
        val text = if (looksHinglish(query)) {
            "$joined offline data mein mile."
        } else {
            "$joined are available in Johar's offline data."
        }
        return JoharAnswer(
            text = text,
            evidence = relevant,
            mode = JoharAnswerMode.DETERMINISTIC,
        )
    }

    private fun listCategory(query: String): ListCategory? = when {
        containsAny(query, "food", "khana", "dish", "cuisine", "mithai", "sweet") -> ListCategory(
            queryTerms = setOf("food", "khana", "dish", "cuisine", "mithai", "sweet"),
            packTypes = setOf("FOOD"),
            entityTypes = setOf("FOOD"),
        )
        containsAny(query, "temple", "mandir", "dham") -> ListCategory(
            queryTerms = setOf("temple", "mandir", "dham"),
            packTypes = setOf("TEMPLE", "PILGRIMAGE"),
            entityTypes = setOf("TOURIST_ATTRACTION"),
        )
        containsAny(query, "waterfall", "waterfalls", "falls", "jharna") -> ListCategory(
            queryTerms = setOf("waterfall", "waterfalls", "falls", "jharna"),
            packTypes = setOf("WATERFALL"),
            entityTypes = setOf("NATURAL_FEATURE"),
        )
        containsAny(query, "school", "schools") -> ListCategory(
            queryTerms = setOf("school", "schools"), packTypes = setOf("SCHOOL"), entityTypes = setOf("FACILITY"),
        )
        containsAny(query, "college", "colleges", "university", "universities") -> ListCategory(
            queryTerms = setOf("college", "colleges", "university", "universities"),
            packTypes = setOf("COLLEGE", "UNIVERSITY", "COLLEGE_UNIVERSITY"), entityTypes = setOf("ORGANIZATION", "FACILITY"),
        )
        containsAny(query, "hospital", "hospitals", "clinic", "clinics") -> ListCategory(
            queryTerms = setOf("hospital", "hospitals", "clinic", "clinics"), packTypes = setOf("HOSPITAL"), entityTypes = setOf("HOSPITAL"),
        )
        containsAny(query, "police", "thana") -> ListCategory(
            queryTerms = setOf("police", "thana", "station"), packTypes = setOf("POLICE_STATION"), entityTypes = setOf("POLICE_STATION"),
        )
        containsAny(query, "market", "markets", "bazar", "bazaar", "haat", "mandi") -> ListCategory(
            queryTerms = setOf("market", "markets", "bazar", "bazaar", "haat", "mandi"),
            packTypes = setOf("MARKET", "MARKET_COLLECTION", "MARKET_TYPE"), entityTypes = setOf("MARKET"),
        )
        containsAny(query, "park", "parks", "garden") -> ListCategory(
            queryTerms = setOf("park", "parks", "garden"), packTypes = setOf("PARK", "GARDEN"), entityTypes = setOf("TOURIST_ATTRACTION"),
        )
        else -> null
    }

    private fun isBroadCategoryQuery(query: String, categoryTerms: Set<String>): Boolean {
        val meaningful = normalizeText(query).split(' ').filter(String::isNotBlank).filterNot { token ->
            token in categoryTerms || token in BROAD_QUERY_FILLER
        }
        return meaningful.isEmpty()
    }

    private fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names.first()
        2 -> "${names[0]} aur ${names[1]}"
        else -> names.dropLast(1).joinToString(", ") + " aur " + names.last()
    }

    private fun looksHinglish(query: String): Boolean = containsAny(
        query,
        " kya ", " ka ", " ki ", " ke ", " hai", " me ", " mein ", " batao", " kaha", " kahan", " paas", " rajya ", "jharna",
    ) || query.startsWith("kya ") || query.startsWith("rajya ")

    private fun humanField(value: String): String = splitFieldName(value).replaceFirstChar { it.uppercase() }

    private fun splitFieldName(value: String): String =
        value.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ')

    private fun containsAny(text: String, vararg terms: String): Boolean = terms.any(text::contains)

    private data class ListCategory(
        val queryTerms: Set<String>,
        val packTypes: Set<String>,
        val entityTypes: Set<String>,
    )

    private companion object {
        val BROAD_QUERY_FILLER = setOf(
            "ranchi", "jharkhand", "in", "mein", "me", "ka", "ki", "ke", "local", "famous", "some", "list", "show",
            "find", "best", "good", "top", "batao", "dikhao", "please", "near", "nearby",
        )

        val DETAIL_QUERY_TERMS = listOf(
            "stairs" to setOf("stairs", "steps", "step"),
            "steps" to setOf("stairs", "steps", "step"),
            "wheelchair" to setOf("wheelchair", "accessible", "accessibility"),
            "accessible" to setOf("wheelchair", "accessible", "accessibility"),
            "address" to setOf("address", "location", "locality"),
            "kahan" to setOf("address", "location", "locality"),
            "phone" to setOf("phone", "contact", "helpline"),
            "number" to setOf("phone", "contact", "helpline", "ambulance", "emergency"),
            "ambulance" to setOf("ambulance"),
            "emergency" to setOf("emergency"),
            "distance" to setOf("distance", "nearby landmark"),
        )
    }
}

data class JoharAnswer(
    val text: String,
    val evidence: List<OfflineKnowledgeHit>,
    val mode: JoharAnswerMode,
)

enum class JoharAnswerMode {
    DETERMINISTIC,
    NO_ANSWER,
}
