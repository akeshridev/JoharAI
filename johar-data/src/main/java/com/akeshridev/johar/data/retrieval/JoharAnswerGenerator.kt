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
        val hits = retriever.retrieve(query, limit = 3)
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
        listAnswer(normalized, hits)?.let { return it }

        val top = hits.first()
        val text = top.description
            ?.takeIf(String::isNotBlank)
            ?.let { description -> "${top.name}: $description" }
            ?: top.facts.firstOrNull()?.let { fact -> "${top.name}: ${fact.field} = ${fact.value}" }
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

        val label = when (requestedField) {
            "state animal" -> if (looksHinglish(query)) "state animal" else "state animal"
            "state bird" -> "state bird"
            "state tree" -> "state tree"
            "state flower" -> "state flower"
            else -> requestedField
        }
        val text = if (looksHinglish(query)) {
            "Jharkhand ka $label ${fact.value} hai."
        } else {
            "Jharkhand's $label is ${fact.value}."
        }

        return JoharAnswer(
            text = text,
            evidence = listOf(evidenceHit),
            mode = JoharAnswerMode.DETERMINISTIC,
        )
    }

    private fun listAnswer(
        query: String,
        hits: List<OfflineKnowledgeHit>,
    ): JoharAnswer? {
        val isTemple = containsAny(query, "temple", "mandir", "dham", "religious place", "spiritual place")
        val isWaterfall = containsAny(query, "waterfall", "falls", "jharna")
        if (!isTemple && !isWaterfall) return null

        val relevant = hits.filter { hit ->
            when {
                isTemple -> hit.packType in setOf("TEMPLE", "PILGRIMAGE")
                isWaterfall -> hit.packType == "WATERFALL"
                else -> false
            }
        }.ifEmpty { hits }.take(3)

        val names = relevant.map { it.name }.distinct()
        if (names.isEmpty()) return null
        val joined = joinNames(names)
        val noun = if (isTemple) "places" else "waterfalls"
        val text = if (looksHinglish(query)) {
            "$joined relevant $noun hain."
        } else {
            "$joined are relevant $noun."
        }

        return JoharAnswer(
            text = text,
            evidence = relevant,
            mode = JoharAnswerMode.DETERMINISTIC,
        )
    }

    private fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names.first()
        2 -> "${names[0]} aur ${names[1]}"
        else -> names.dropLast(1).joinToString(", ") + " aur " + names.last()
    }

    private fun looksHinglish(query: String): Boolean = containsAny(
        query,
        " kya ", " ka ", " ki ", " ke ", " hai", " me ", " mein ", " batao", " kaha", " paas", " rajya ", "jharna",
    ) || query.startsWith("kya ") || query.startsWith("rajya ")

    private fun splitFieldName(value: String): String =
        value.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ')

    private fun containsAny(text: String, vararg terms: String): Boolean = terms.any(text::contains)
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
