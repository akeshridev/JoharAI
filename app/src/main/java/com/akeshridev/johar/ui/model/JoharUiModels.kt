package com.akeshridev.johar.ui.model

import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharInfoTone
import com.akeshridev.johar.designsystem.JoharItineraryCardModel
import com.akeshridev.johar.designsystem.JoharPlaceCardModel
import com.akeshridev.johar.designsystem.JoharRouteCardModel

sealed interface JoharContent {
    data class Map(val destination: RanchiSpatialPlace) : JoharContent

    data class Text(
        val text: String,
    ) : JoharContent

    data class Places(
        val intro: String? = null,
        val items: List<JoharPlaceCardModel>,
    ) : JoharContent

    data class Route(
        val route: JoharRouteCardModel,
    ) : JoharContent

    data class Itinerary(
        val plan: JoharItineraryCardModel,
    ) : JoharContent

    data class Info(
        val title: String,
        val text: String,
        val tone: JoharInfoTone = JoharInfoTone.NORMAL,
        val actions: List<JoharCardAction> = emptyList(),
    ) : JoharContent
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
