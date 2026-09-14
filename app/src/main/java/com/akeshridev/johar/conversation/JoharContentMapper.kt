package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.retrieval.OfflineKnowledgeHit
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharComparisonItem
import com.akeshridev.johar.designsystem.JoharInfoTone
import com.akeshridev.johar.designsystem.JoharItineraryCardModel
import com.akeshridev.johar.designsystem.JoharItineraryStop
import com.akeshridev.johar.designsystem.JoharPlaceCardModel
import com.akeshridev.johar.designsystem.JoharRouteCardModel
import com.akeshridev.johar.designsystem.JoharRouteStop
import com.akeshridev.johar.ui.model.JoharContent
import com.akeshridev.johar.ui.model.JoharSourceUiModel
import com.akeshridev.johar.ui.model.JoharUtilityUiModel
import java.net.URI
import java.util.Locale
import kotlin.math.roundToInt

/** Data-to-presentation boundary: no database or spatial dependency enters the design system. */
object JoharContentMapper {
    fun map(result: JoharQueryResult): JoharContent = when (result) {
        is JoharQueryResult.Grounded -> grounded(result)
        is JoharQueryResult.Places -> if (result.kind == PlaceResultKind.UTILITY) {
            JoharContent.Utilities(result.intro, result.places.map(::utilityCard))
        } else {
            JoharContent.Places(result.intro, result.places.map(::placeCard))
        }
        is JoharQueryResult.Route -> JoharContent.Route(
            route = routeCard(result),
            origin = result.origin,
            destination = result.destination,
            routePoints = result.route.points,
        )
        is JoharQueryResult.Comparison -> comparison(result)
        is JoharQueryResult.Itinerary -> itinerary(result)
        is JoharQueryResult.Clarification -> JoharContent.Clarification(result.prompt, result.options)
    }

    fun placeCard(place: RanchiSpatialPlace) = JoharPlaceCardModel(
        name = place.name,
        area = place.region?.takeIf(String::isNotBlank),
        description = place.description?.takeIf(String::isNotBlank),
        metadata = buildList {
            place.type.takeIf(String::isNotBlank)?.let { add(humanize(it)) }
            place.distanceKm?.takeIf { it.isFinite() && it >= 0 }?.let {
                add(String.format(Locale.ROOT, "%.1f km · straight-line", it))
            }
        },
        actions = listOf(JoharCardAction(mapActionId(place.id), "View on Map", primary = true)),
    )

    private fun utilityCard(place: RanchiSpatialPlace) = JoharUtilityUiModel(
        title = place.name,
        subtitle = listOfNotNull(place.region?.takeIf(String::isNotBlank), humanize(place.type).takeIf(String::isNotBlank))
            .joinToString(" · ")
            .ifBlank { "Ranchi" },
        metadata = place.distanceKm?.takeIf { it.isFinite() && it >= 0 }?.let {
            String.format(Locale.ROOT, "%.1f km straight-line", it)
        } ?: place.description?.takeIf(String::isNotBlank),
        actions = listOf(JoharCardAction(mapActionId(place.id), "View on Map", primary = true)),
    )

    private fun grounded(result: JoharQueryResult.Grounded): JoharContent.Grounded {
        val text = listOfNotNull(result.prefix?.takeIf(String::isNotBlank), result.answer.text.takeIf(String::isNotBlank))
            .joinToString(" ")
        return JoharContent.Grounded(
            text = text,
            tone = when (result.tone) {
                GroundedTone.NORMAL -> JoharInfoTone.NORMAL
                GroundedTone.OFFLINE -> JoharInfoTone.OFFLINE
                GroundedTone.NOT_CONFIRMED -> JoharInfoTone.NOT_CONFIRMED
                GroundedTone.WARNING -> JoharInfoTone.WARNING
            },
            sources = sources(result.answer.evidence),
        )
    }

    private fun sources(evidence: List<OfflineKnowledgeHit>): List<JoharSourceUiModel> = evidence
        .asSequence()
        .flatMap { hit -> hit.facts.asSequence() }
        .mapNotNull { fact -> sourceName(fact.sourceUrl) }
        .distinct()
        .take(3)
        .map { JoharSourceUiModel(sourceName = it) }
        .toList()

    private fun sourceName(url: String): String? {
        if (url.isBlank()) return null
        return runCatching {
            val host = URI(url).host?.removePrefix("www.")
            host?.takeIf(String::isNotBlank) ?: url.take(48)
        }.getOrElse { url.take(48) }
    }

    private fun comparison(result: JoharQueryResult.Comparison): JoharContent.Comparison = JoharContent.Comparison(
        leftTitle = result.left.name,
        rightTitle = result.right.name,
        rows = listOf(
            JoharComparisonItem("Type", humanize(result.left.type), humanize(result.right.type)),
            JoharComparisonItem("Area", result.left.region.orDash(), result.right.region.orDash()),
            JoharComparisonItem("Map", "Available offline", "Available offline"),
        ),
        recommendation = null,
    )

    private fun itinerary(result: JoharQueryResult.Itinerary): JoharContent.Itinerary = JoharContent.Itinerary(
        plan = JoharItineraryCardModel(
            title = result.title,
            summary = result.summary,
            stops = result.places.map { place ->
                JoharItineraryStop(
                    title = place.name,
                    subtitle = listOfNotNull(place.region?.takeIf(String::isNotBlank), humanize(place.type).takeIf(String::isNotBlank))
                        .joinToString(" · ")
                        .takeIf(String::isNotBlank),
                )
            },
        ),
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

    private fun humanize(value: String): String = value
        .replace('_', ' ')
        .lowercase(Locale.ROOT)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    private fun String?.orDash(): String = this?.takeIf(String::isNotBlank) ?: "—"
}
