package com.akeshridev.johar.domain.source

data class SourceFact(
    val entityId: String,
    val sourceUrl: String,
    val publisher: String,
    val retrievedAtEpochMillis: Long,
    val domain: KnowledgeDomain,
    val field: String,
    val value: FactValue,
    val evidenceText: String,
    val factType: FactType = FactType.SOURCE_FACT,
    val state: FactState = FactState.KNOWN,
    val freshness: Freshness,
)

enum class KnowledgeDomain {
    TOURISM,
    FAMILY_ACCESSIBILITY,
    SAFETY_EMERGENCY,
    HISTORY_CULTURE,
    FOOD,
    TRAVEL_LOGISTICS,
    WEATHER_SEASON_CONTEXT,
    FACILITIES,
    GEOGRAPHY,
}

sealed interface FactValue {
    data class Text(val value: String) : FactValue

    data class Number(
        val value: Double,
        val unit: String? = null,
    ) : FactValue

    data class BooleanValue(val value: Boolean) : FactValue
}

enum class FactType {
    SOURCE_FACT,
}

enum class FactState {
    KNOWN,
    UNKNOWN,
}

enum class Freshness {
    EVERGREEN,
    SEASONAL,
    LIVE,
}
