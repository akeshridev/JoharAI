package com.akeshridev.johar.designsystem

data class JoharPlaceCardModel(
    val name: String,
    val area: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: List<String> = emptyList(),
    val actions: List<JoharCardAction> = emptyList(),
)

data class JoharRouteCardModel(
    val title: String,
    val durationLabel: String,
    val distanceLabel: String,
    val stops: List<JoharRouteStop>,
    val status: List<String> = emptyList(),
    val actions: List<JoharCardAction> = emptyList(),
)

data class JoharRouteStop(
    val title: String,
    val subtitle: String? = null,
    val trailingLabel: String? = null,
)

data class JoharItineraryCardModel(
    val title: String,
    val summary: String? = null,
    val stops: List<JoharItineraryStop>,
    val actions: List<JoharCardAction> = emptyList(),
)

data class JoharItineraryStop(
    val timeLabel: String? = null,
    val title: String,
    val subtitle: String? = null,
)

data class JoharCardAction(
    val id: String,
    val label: String,
    val primary: Boolean = false,
)

enum class JoharInfoTone {
    NORMAL,
    VERIFIED,
    LIVE,
    OFFLINE,
    NOT_CONFIRMED,
    WARNING,
}
