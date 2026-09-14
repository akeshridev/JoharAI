package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharPlaceCardModel
import com.akeshridev.johar.designsystem.JoharRouteCardModel
import com.akeshridev.johar.designsystem.JoharRouteStop
import com.akeshridev.johar.ui.model.JoharContent
import java.util.Locale
import kotlin.math.roundToInt

/** Data-to-presentation boundary: no database or spatial dependency enters the design system. */
object JoharContentMapper {
    fun map(result: JoharQueryResult): JoharContent = when (result) {
        is JoharQueryResult.Text -> JoharContent.Text(result.text)
        is JoharQueryResult.Places -> JoharContent.Places(result.intro, result.places.map(::placeCard))
        is JoharQueryResult.Route -> JoharContent.Route(
            route = routeCard(result),
            origin = result.origin,
            destination = result.destination,
            routePoints = result.route.points,
        )
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

    private fun routeCard(result: JoharQueryResult.Route): JoharRouteCardModel {
        val distanceKm = result.route.distanceMeters / 1_000.0
        val durationMinutes = (result.route.durationSeconds / 60.0).roundToInt().coerceAtLeast(0)
        return JoharRouteCardModel(
            title = "${result.origin.name} → ${result.destination.name}",
            durationLabel = if (durationMinutes == 0) "< 1 min" else "$durationMinutes min",
            distanceLabel = String.format(Locale.ROOT, "%.1f km", distanceKm),
            stops = listOf(
                JoharRouteStop(title = result.origin.name, subtitle = "Start"),
                JoharRouteStop(title = result.destination.name, subtitle = "Destination"),
            ),
            status = listOf("Offline road route"),
        )
    }

    fun mapActionId(placeId: String) = "map:$placeId"
}
