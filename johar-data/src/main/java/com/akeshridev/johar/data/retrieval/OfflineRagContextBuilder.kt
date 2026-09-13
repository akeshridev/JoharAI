package com.akeshridev.johar.data.retrieval

class OfflineRagContextBuilder(
    private val retriever: OfflineKnowledgeRetriever,
) {
    fun build(
        query: String,
        maxHits: Int = 3,
    ): OfflineRagContext {
        val hits = retriever.retrieve(query, limit = maxHits)
        val evidence = hits.mapIndexed { index, hit ->
            buildString {
                append("[E${index + 1}] ")
                append(hit.name)
                hit.packType?.takeIf(String::isNotBlank)?.let { packType ->
                    append(" (type=")
                    append(packType)
                    append(')')
                }
                hit.description?.takeIf(String::isNotBlank)?.let { description ->
                    append("\nDescription: ")
                    append(description)
                }
                if (hit.facts.isNotEmpty()) {
                    append("\nFacts:")
                    hit.facts.take(3).forEach { fact ->
                        append("\n- ")
                        append(fact.field)
                        append(" = ")
                        append(fact.value)
                    }
                }
            }
        }

        val prompt = buildString {
            appendLine("You are Johar, an offline Jharkhand knowledge assistant.")
            appendLine("Answer only from the evidence below. Do not invent missing facts.")
            appendLine("If the evidence is insufficient, say that the offline knowledge pack does not have enough information.")
            appendLine("Keep the answer short, practical, and in the language/style of the user's question when possible.")
            appendLine()
            appendLine("Question: $query")
            appendLine()
            appendLine("Evidence:")
            if (evidence.isEmpty()) {
                appendLine("[none]")
            } else {
                evidence.forEach { block ->
                    appendLine(block)
                    appendLine()
                }
            }
            append("Answer:")
        }

        return OfflineRagContext(
            query = query,
            hits = hits,
            prompt = prompt,
        )
    }
}

data class OfflineRagContext(
    val query: String,
    val hits: List<OfflineKnowledgeHit>,
    val prompt: String,
)
