package com.akeshridev.johar.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun JoharLocalPickCard(
    title: String,
    subtitle: String,
    detail: String? = null,
    action: JoharCardAction? = null,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = JoharColors.SoftOrange),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            JoharStatusBadge(label = "Local Pick", tone = JoharInfoTone.NORMAL)
            Spacer(Modifier.height(JoharSpacing.Md))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            detail?.let {
                Spacer(Modifier.height(JoharSpacing.Sm))
                Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted)
            }
            action?.let {
                Spacer(Modifier.height(JoharSpacing.Lg))
                JoharActionRow(actions = listOf(it), onAction = onAction)
            }
        }
    }
}

@Composable
fun JoharUtilityCard(
    title: String,
    subtitle: String,
    metadata: String? = null,
    actions: List<JoharCardAction> = emptyList(),
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Medium),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = JoharElevation.Flat,
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted)
            metadata?.let {
                Spacer(Modifier.height(JoharSpacing.Sm))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(JoharSpacing.Md))
                JoharActionRow(actions = actions, onAction = onAction)
            }
        }
    }
}

data class JoharComparisonItem(
    val label: String,
    val leftValue: String,
    val rightValue: String,
)

@Composable
fun JoharComparisonCard(
    leftTitle: String,
    rightTitle: String,
    rows: List<JoharComparisonItem>,
    recommendation: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Row(Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(0.8f))
                Text(leftTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(rightTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(JoharSpacing.Md))
            rows.forEach { item ->
                Row(Modifier.fillMaxWidth()) {
                    Text(item.label, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted, modifier = Modifier.weight(0.8f))
                    Text(item.leftValue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(item.rightValue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(JoharSpacing.Sm))
            }
            recommendation?.let {
                Spacer(Modifier.height(JoharSpacing.Md))
                Text("Johar pick", style = MaterialTheme.typography.labelSmall, color = JoharColors.Muted)
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun JoharSuggestionCard(
    title: String = "Try asking Johar",
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(JoharSpacing.Md))
        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(JoharRadius.Medium),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = JoharSpacing.Lg, vertical = JoharSpacing.Md),
                )
            }
            Spacer(Modifier.height(JoharSpacing.Sm))
        }
    }
}
