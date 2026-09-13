package com.akeshridev.johar.domain.source

data class KnowledgeSource(
    val id: String,
    val url: String,
    val publisher: String,
    val retrievedAtEpochMillis: Long,
    val kind: SourceKind,
    val license: String? = null,
    val attribution: String? = null,
)

enum class SourceKind {
    GOVERNMENT,
    WIKIDATA,
    OPENSTREETMAP,
    WIKIPEDIA,
    WIKIVOYAGE,
    WIKIMEDIA_COMMONS,
    WEATHER,
    OTHER,
}
