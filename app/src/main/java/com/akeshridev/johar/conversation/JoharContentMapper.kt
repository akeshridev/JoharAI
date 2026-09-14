package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharPlaceCardModel
import com.akeshridev.johar.ui.model.JoharContent
import java.util.Locale

/** Data-to-presentation boundary: no database or spatial dependency enters the design system. */
object JoharContentMapper {
    fun map(result: JoharQueryResult): JoharContent = when (result) {
        is JoharQueryResult.Text -> JoharContent.Text(result.text)
        is JoharQueryResult.Places -> JoharContent.Places(result.intro, result.places.map(::placeCard))
    }

    fun placeCard(place: RanchiSpatialPlace) = JoharPlaceCardModel(
        name = place.name,
        area = place.region?.takeIf(String::isNotBlank),
        description = place.description?.takeIf(String::isNotBlank),
        metadata = buildList {
            place.type.takeIf(String::isNotBlank)?.let { add(it.replace('_', ' ').lowercase(Locale.ROOT)) }
            place.distanceKm?.takeIf { it.isFinite() && it >= 0 }?.let {
                add(String.format(Locale.ROOT, "%.1f km · straight-line", it))
            }
        },
        actions = listOf(JoharCardAction(mapActionId(place.id), "View on Map", primary = true)),
    )

    fun mapActionId(placeId: String) = "map:$placeId"
}
