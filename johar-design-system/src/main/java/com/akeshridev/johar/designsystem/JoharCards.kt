package com.akeshridev.johar.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun JoharPlaceCard(
    model: JoharPlaceCardModel,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        model.area?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted) }

        model.description?.let {
            Spacer(Modifier.height(JoharSpacing.Sm))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        if (model.metadata.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Md))
            Text(
                model.metadata.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = JoharColors.Muted,
            )
        }

        if (model.tags.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Md))
            Row {
                model.tags.take(3).forEachIndexed { index, tag ->
                    if (index > 0) Spacer(Modifier.width(JoharSpacing.Sm))
                    JoharChip(label = tag)
                }
            }
        }

        if (model.actions.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Lg))
            JoharActionRow(actions = model.actions, onAction = onAction)
        }
    }
}

@Composable
fun JoharRouteCard(
    model: JoharRouteCardModel,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(model.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(JoharSpacing.Xs))
        Text(
            "${model.durationLabel} • ${model.distanceLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = JoharColors.Muted,
        )

        Spacer(Modifier.height(JoharSpacing.Lg))
        model.stops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = if (index == 0) "●" else "↓",
                    color = if (index == 0 || index == model.stops.lastIndex) JoharColors.Green else JoharColors.Muted,
                )
                Spacer(Modifier.width(JoharSpacing.Md))
                Column(Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    stop.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted)
                    }
                }
                stop.trailingLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = JoharColors.Muted)
                }
            }
            if (index != model.stops.lastIndex) Spacer(Modifier.height(JoharSpacing.Md))
        }

        if (model.status.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Lg))
            Text(
                model.status.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = JoharColors.Muted,
            )
        }

        if (model.actions.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Lg))
            JoharActionRow(actions = model.actions, onAction = onAction)
        }
    }
}

@Composable
fun JoharItineraryCard(
    model: JoharItineraryCardModel,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(model.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        model.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted) }

        Spacer(Modifier.height(JoharSpacing.Lg))
        model.stops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stop.timeLabel ?: "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = JoharColors.Muted,
                    modifier = Modifier.width(JoharSpacing.Xxl * 3),
                )
                Column(Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    stop.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted)
                    }
                }
            }
            if (index != model.stops.lastIndex) Spacer(Modifier.height(JoharSpacing.Md))
        }

        if (model.actions.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Lg))
            JoharActionRow(actions = model.actions, onAction = onAction)
        }
    }
}

@Composable
fun JoharInfoCard(
    title: String,
    text: String,
    tone: JoharInfoTone,
    actions: List<JoharCardAction> = emptyList(),
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    val container = when (tone) {
        JoharInfoTone.LIVE, JoharInfoTone.VERIFIED -> JoharColors.SoftGreen
        JoharInfoTone.NOT_CONFIRMED, JoharInfoTone.WARNING -> JoharColors.SoftOrange
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (tone != JoharInfoTone.NORMAL) {
                    JoharStatusBadge(label = toneLabel(tone), tone = tone)
                }
            }
            Spacer(Modifier.height(JoharSpacing.Sm))
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(JoharSpacing.Lg))
                JoharActionRow(actions = actions, onAction = onAction)
            }
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
        shape = RoundedCornerShape(JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = JoharElevation.Flat),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) { content() }
    }
}

private fun toneLabel(tone: JoharInfoTone): String = when (tone) {
    JoharInfoTone.NORMAL -> ""
    JoharInfoTone.VERIFIED -> "Verified"
    JoharInfoTone.LIVE -> "Live"
    JoharInfoTone.OFFLINE -> "Offline"
    JoharInfoTone.NOT_CONFIRMED -> "Not confirmed"
    JoharInfoTone.WARNING -> "Check"
}
