package com.akeshridev.johar.ui.model

sealed interface JoharContent {
    data class Text(
        val text: String,
    ) : JoharContent

    data class Places(
        val intro: String? = null,
        val items: List<PlaceUiModel>,
    ) : JoharContent

    data class Route(
        val route: RouteUiModel,
    ) : JoharContent

    data class Itinerary(
        val plan: ItineraryUiModel,
    ) : JoharContent

    data class Info(
        val title: String,
        val text: String,
        val status: InfoStatus = InfoStatus.NORMAL,
        val actions: List<JoharAction> = emptyList(),
    ) : JoharContent
}

data class PlaceUiModel(
    val id: String,
    val name: String,
    val area: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val distanceLabel: String? = null,
    val etaLabel: String? = null,
    val parkingLabel: String? = null,
    val statusLabel: String? = null,
    val actions: List<JoharAction> = emptyList(),
)

data class RouteUiModel(
    val title: String,
    val distanceLabel: String,
    val durationLabel: String,
    val stops: List<RouteStopUiModel>,
    val isOffline: Boolean = true,
    val trafficAvailable: Boolean = false,
    val actions: List<JoharAction> = emptyList(),
)

data class RouteStopUiModel(
    val title: String,
    val subtitle: String? = null,
    val detourLabel: String? = null,
)

data class ItineraryUiModel(
    val title: String,
    val summary: String? = null,
    val stops: List<ItineraryStopUiModel>,
    val actions: List<JoharAction> = emptyList(),
)

data class ItineraryStopUiModel(
    val timeLabel: String? = null,
    val title: String,
    val subtitle: String? = null,
)

data class JoharAction(
    val id: String,
    val label: String,
    val style: ActionStyle = ActionStyle.SECONDARY,
)

enum class ActionStyle {
    PRIMARY,
    SECONDARY,
}

enum class InfoStatus {
    NORMAL,
    VERIFIED,
    LIVE,
    OFFLINE,
    NOT_CONFIRMED,
    WARNING,
}

data class JoharMessageUiModel(
    val id: String,
    val sender: Sender,
    val content: JoharContent,
)

enum class Sender {
    USER,
    JOHAR,
}
