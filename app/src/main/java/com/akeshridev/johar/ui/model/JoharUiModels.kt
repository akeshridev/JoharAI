package com.akeshridev.johar.ui.model

import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharComparisonItem
import com.akeshridev.johar.designsystem.JoharInfoTone
import com.akeshridev.johar.designsystem.JoharItineraryCardModel
import com.akeshridev.johar.designsystem.JoharPlaceCardModel
import com.akeshridev.johar.designsystem.JoharRouteCardModel

sealed interface JoharContent {
    data class Map(val destination: RanchiSpatialPlace) : JoharContent

    data class Text(
        val text: String,
    ) : JoharContent

    /** Text-first grounded answer. Source rows are rendered only when evidence exists. */
    data class Grounded(
        val text: String,
        val tone: JoharInfoTone = JoharInfoTone.NORMAL,
        val sources: List<JoharSourceUiModel> = emptyList(),
    ) : JoharContent

    data class Places(
        val intro: String? = null,
        val items: List<JoharPlaceCardModel>,
    ) : JoharContent

    data class Utilities(
        val intro: String? = null,
        val items: List<JoharUtilityUiModel>,
    ) : JoharContent

    data class Route(
        val route: JoharRouteCardModel,
        val origin: RanchiSpatialPlace,
        val destination: RanchiSpatialPlace,
        val routePoints: List<RanchiCoordinate>,
    ) : JoharContent

    data class Itinerary(
        val plan: JoharItineraryCardModel,
    ) : JoharContent

    data class Comparison(
        val leftTitle: String,
        val rightTitle: String,
        val rows: List<JoharComparisonItem>,
        val recommendation: String? = null,
    ) : JoharContent

    data class Clarification(
        val prompt: String,
        val options: List<String>,
    ) : JoharContent

    data class Info(
        val title: String,
        val text: String,
        val tone: JoharInfoTone = JoharInfoTone.NORMAL,
        val actions: List<JoharCardAction> = emptyList(),
    ) : JoharContent
}

data class JoharSourceUiModel(
    val sourceName: String,
    val verified: Boolean = false,
)

data class JoharUtilityUiModel(
    val title: String,
    val subtitle: String,
    val metadata: String? = null,
    val actions: List<JoharCardAction> = emptyList(),
)

data class JoharMessageUiModel(
    val id: String,
    val sender: Sender,
    val content: JoharContent,
)

enum class Sender {
    USER,
    JOHAR,
}
