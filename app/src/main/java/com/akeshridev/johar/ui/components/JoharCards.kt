package com.akeshridev.johar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akeshridev.johar.ui.model.ActionStyle
import com.akeshridev.johar.ui.model.InfoStatus
import com.akeshridev.johar.ui.model.ItineraryUiModel
import com.akeshridev.johar.ui.model.JoharAction
import com.akeshridev.johar.ui.model.PlaceUiModel
import com.akeshridev.johar.ui.model.RouteUiModel
import com.akeshridev.johar.ui.theme.JoharGreen
import com.akeshridev.johar.ui.theme.JoharMuted
import com.akeshridev.johar.ui.theme.JoharSoftGreen
import com.akeshridev.johar.ui.theme.JoharSoftOrange

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun JoharPlaceCard(
    place: PlaceUiModel,
    modifier: Modifier = Modifier,
    onAction: (JoharAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(
            text = place.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        place.area?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = JoharMuted)
        }
        place.description?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        val metadata = buildList {
            place.distanceLabel?.let(::add)
            place.etaLabel?.let(::add)
            place.parkingLabel?.let(::add)
            place.statusLabel?.let(::add)
        }
        if (metadata.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = metadata.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = JoharMuted,
            )
        }

        if (place.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                place.tags.take(3).forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }

        JoharActions(place.actions, onAction)
    }
}

@Composable
fun JoharRouteCard(
    route: RouteUiModel,
    modifier: Modifier = Modifier,
    onAction: (JoharAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(
            text = route.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${route.durationLabel} • ${route.distanceLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = JoharMuted,
        )

        Spacer(Modifier.height(14.dp))
        route.stops.forEachIndexed { index, stop ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (index == 0) "●" else "↓",
                    color = if (index == 0 || index == route.stops.lastIndex) JoharGreen else JoharMuted,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    stop.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted)
                    }
                }
                stop.detourLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = JoharMuted)
                }
            }
            if (index != route.stops.lastIndex) Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))
        val routeStatus = buildList {
            if (route.isOffline) add("Offline route")
            if (!route.trafficAvailable) add("Live traffic unavailable")
        }
        if (routeStatus.isNotEmpty()) {
            Text(
                routeStatus.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = JoharMuted,
            )
        }

        JoharActions(route.actions, onAction)
    }
}

@Composable
fun JoharItineraryCard(
    plan: ItineraryUiModel,
    modifier: Modifier = Modifier,
    onAction: (JoharAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(
            text = plan.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        plan.summary?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted)
        }

        Spacer(Modifier.height(14.dp))
        plan.stops.forEachIndexed { index, stop ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stop.timeLabel ?: "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = JoharMuted,
                    modifier = Modifier.width(68.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    stop.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted)
                    }
                }
            }
            if (index != plan.stops.lastIndex) Spacer(Modifier.height(12.dp))
        }

        JoharActions(plan.actions, onAction)
    }
}

@Composable
fun JoharInfoCard(
    title: String,
    text: String,
    status: InfoStatus,
    actions: List<JoharAction> = emptyList(),
    modifier: Modifier = Modifier,
    onAction: (JoharAction) -> Unit = {},
) {
    val containerColor = when (status) {
        InfoStatus.LIVE, InfoStatus.VERIFIED -> JoharSoftGreen
        InfoStatus.NOT_CONFIRMED, InfoStatus.WARNING -> JoharSoftOrange
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (status != InfoStatus.NORMAL) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    ) {
                        Text(
                            text = statusLabel(status),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
            JoharActions(actions, onAction)
        }
    }
}

@Composable
private fun JoharBaseCard(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(16.dp), content = { content() })
    }
}

@Composable
private fun JoharActions(
    actions: List<JoharAction>,
    onAction: (JoharAction) -> Unit,
) {
    if (actions.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.take(2).forEach { action ->
            when (action.style) {
                ActionStyle.PRIMARY -> Button(onClick = { onAction(action) }) { Text(action.label) }
                ActionStyle.SECONDARY -> OutlinedButton(onClick = { onAction(action) }) { Text(action.label) }
            }
        }
    }
}

private fun statusLabel(status: InfoStatus): String = when (status) {
    InfoStatus.NORMAL -> ""
    InfoStatus.VERIFIED -> "Verified"
    InfoStatus.LIVE -> "Live"
    InfoStatus.OFFLINE -> "Offline"
    InfoStatus.NOT_CONFIRMED -> "Not confirmed"
    InfoStatus.WARNING -> "Check"
}
