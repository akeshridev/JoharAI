package com.akeshridev.johar.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class JoharCardDensity {
    COMPACT,
    STANDARD,
    FEATURED,
}

@Composable
fun JoharPlaceCard(
    model: JoharPlaceCardModel,
    modifier: Modifier = Modifier,
    density: JoharCardDensity = JoharCardDensity.STANDARD,
    onAction: (JoharCardAction) -> Unit = {},
) {
    val padding = when (density) {
        JoharCardDensity.COMPACT -> JoharSpacing.Md
        JoharCardDensity.STANDARD -> JoharSpacing.Lg
        JoharCardDensity.FEATURED -> JoharSpacing.Xl
    }

    JoharBaseCard(modifier = modifier, contentPadding = padding) {
        Text(
            model.name,
            style = if (density == JoharCardDensity.COMPACT) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        model.area?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        if (density != JoharCardDensity.COMPACT) {
            model.description?.let {
                Spacer(Modifier.height(JoharSpacing.Sm))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (model.metadata.isNotEmpty()) {
            Spacer(Modifier.height(if (density == JoharCardDensity.COMPACT) JoharSpacing.Sm else JoharSpacing.Md))
            Text(
                model.metadata.take(if (density == JoharCardDensity.COMPACT) 2 else 4).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (model.tags.isNotEmpty() && density != JoharCardDensity.COMPACT) {
            Spacer(Modifier.height(JoharSpacing.Md))
            Row {
                model.tags.take(3).forEachIndexed { index, tag ->
                    if (index > 0) Spacer(Modifier.width(JoharSpacing.Sm))
                    JoharChip(label = tag)
                }
            }
        }

        if (model.actions.isNotEmpty()) {
            Spacer(Modifier.height(if (density == JoharCardDensity.COMPACT) JoharSpacing.Md else JoharSpacing.Lg))
            JoharActionRow(
                actions = if (density == JoharCardDensity.COMPACT) model.actions.takeLast(1) else model.actions,
                onAction = onAction,
            )
        }
    }
}

@Composable
fun JoharRouteCard(
    model: JoharRouteCardModel,
    modifier: Modifier = Modifier,
    density: JoharCardDensity = JoharCardDensity.STANDARD,
    onAction: (JoharCardAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(model.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(JoharSpacing.Xs))
        Text(
            "${model.durationLabel} • ${model.distanceLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(if (density == JoharCardDensity.COMPACT) JoharSpacing.Md else JoharSpacing.Lg))
        model.stops.forEachIndexed { index, stop ->
            val isEndpoint = index == 0 || index == model.stops.lastIndex
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(if (isEndpoint) 12.dp else 8.dp)
                            .background(
                                color = if (isEndpoint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                    )
                    if (index != model.stops.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(if (density == JoharCardDensity.COMPACT) 22.dp else 30.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        )
                    }
                }
                Spacer(Modifier.width(JoharSpacing.Md))
                Column(Modifier.weight(1f)) {
                    Text(
                        stop.title,
                        fontWeight = if (isEndpoint) FontWeight.SemiBold else FontWeight.Medium,
                        style = if (isEndpoint) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    )
                    stop.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                stop.trailingLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (index != model.stops.lastIndex) Spacer(Modifier.height(JoharSpacing.Xs))
        }

        if (model.status.isNotEmpty()) {
            Spacer(Modifier.height(JoharSpacing.Md))
            Text(
                model.status.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    density: JoharCardDensity = JoharCardDensity.STANDARD,
    onAction: (JoharCardAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(model.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        model.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        Spacer(Modifier.height(if (density == JoharCardDensity.COMPACT) JoharSpacing.Md else JoharSpacing.Lg))
        model.stops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stop.timeLabel ?: "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(68.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    if (density != JoharCardDensity.COMPACT) {
                        stop.subtitle?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (index != model.stops.lastIndex) Spacer(Modifier.height(if (density == JoharCardDensity.COMPACT) JoharSpacing.Sm else JoharSpacing.Md))
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
    compact: Boolean = false,
    onAction: (JoharCardAction) -> Unit = {},
) {
    val container = when (tone) {
        JoharInfoTone.LIVE, JoharInfoTone.VERIFIED -> JoharColors.SoftGreen
        JoharInfoTone.NOT_CONFIRMED, JoharInfoTone.WARNING -> JoharColors.SoftOrange
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) JoharRadius.Medium else JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)),
    ) {
        Column(Modifier.padding(if (compact) JoharSpacing.Md else JoharSpacing.Lg)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
    contentPadding: androidx.compose.ui.unit.Dp = JoharSpacing.Lg,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = JoharElevation.Flat),
    ) {
        Column(Modifier.padding(contentPadding)) { content() }
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
