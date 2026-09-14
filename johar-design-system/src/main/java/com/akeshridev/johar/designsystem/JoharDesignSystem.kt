package com.akeshridev.johar.designsystem

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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val JoharCream = Color(0xFFFFFBF0)
val JoharForest = Color(0xFF122A1E)
val JoharRust = Color(0xFF8B2500)
val JoharSand = Color(0xFFE8D5B5)
val JoharGreen = Color(0xFF2E7D32)
val JoharMuted = Color(0xFF665F54)
val JoharSoftGreen = Color(0xFFE8F5E9)
val JoharSoftOrange = Color(0xFFFFF0E5)

private val JoharLightColors = lightColorScheme(
    primary = JoharForest,
    onPrimary = JoharCream,
    secondary = JoharRust,
    onSecondary = Color.White,
    background = JoharCream,
    onBackground = JoharForest,
    surface = Color.White,
    onSurface = JoharForest,
    surfaceVariant = JoharSand,
    onSurfaceVariant = JoharForest,
    outline = JoharSand,
)

@Composable
fun JoharTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JoharLightColors, content = content)
}

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

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun JoharPlaceCard(
    model: JoharPlaceCardModel,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    JoharBaseCard(modifier) {
        Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        model.area?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted) }
        model.description?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        if (model.metadata.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(model.metadata.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = JoharMuted)
        }
        if (model.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                model.tags.take(3).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
            }
        }
        JoharActions(model.actions, onAction)
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
        Spacer(Modifier.height(4.dp))
        Text("${model.durationLabel} • ${model.distanceLabel}", style = MaterialTheme.typography.bodyMedium, color = JoharMuted)
        Spacer(Modifier.height(14.dp))
        model.stops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                Text(if (index == 0) "●" else "↓", color = if (index == 0 || index == model.stops.lastIndex) JoharGreen else JoharMuted)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    stop.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted) }
                }
                stop.trailingLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = JoharMuted) }
            }
            if (index != model.stops.lastIndex) Spacer(Modifier.height(10.dp))
        }
        if (model.status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(model.status.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = JoharMuted)
        }
        JoharActions(model.actions, onAction)
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
        model.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted) }
        Spacer(Modifier.height(14.dp))
        model.stops.forEachIndexed { index, stop ->
            Row(Modifier.fillMaxWidth()) {
                Text(stop.timeLabel ?: "${index + 1}", style = MaterialTheme.typography.labelMedium, color = JoharMuted, modifier = Modifier.width(68.dp))
                Column(Modifier.weight(1f)) {
                    Text(stop.title, fontWeight = FontWeight.Medium)
                    stop.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharMuted) }
                }
            }
            if (index != model.stops.lastIndex) Spacer(Modifier.height(12.dp))
        }
        JoharActions(model.actions, onAction)
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
        JoharInfoTone.LIVE, JoharInfoTone.VERIFIED -> JoharSoftGreen
        JoharInfoTone.NOT_CONFIRMED, JoharInfoTone.WARNING -> JoharSoftOrange
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (tone != JoharInfoTone.NORMAL) {
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)) {
                        Text(toneLabel(tone), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
private fun JoharBaseCard(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun JoharActions(actions: List<JoharCardAction>, onAction: (JoharCardAction) -> Unit) {
    if (actions.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.take(2).forEach { action ->
            if (action.primary) Button(onClick = { onAction(action) }) { Text(action.label) }
            else OutlinedButton(onClick = { onAction(action) }) { Text(action.label) }
        }
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
