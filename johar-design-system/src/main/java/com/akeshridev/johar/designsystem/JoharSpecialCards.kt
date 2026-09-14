package com.akeshridev.johar.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
        shape = RoundedCornerShape(JoharRadius.ExtraLarge),
        colors = CardDefaults.cardColors(
            containerColor = JoharColors.SoftOrange,
            contentColor = JoharColors.Forest,
        ),
        border = BorderStroke(1.dp, JoharColors.Rust.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(JoharSpacing.Xl)) {
            Text(
                "LOCAL RANCHI",
                style = MaterialTheme.typography.labelSmall,
                color = JoharColors.Rust,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(JoharSpacing.Xs))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
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
        shape = RoundedCornerShape(JoharRadius.Large),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = JoharElevation.Flat,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            metadata?.let {
                Spacer(Modifier.height(JoharSpacing.Sm))
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
        shape = RoundedCornerShape(JoharRadius.ExtraLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text("WHICH ONE FITS?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(JoharSpacing.Md))
            Row(Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(0.72f))
                Text(leftTitle, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(rightTitle, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(JoharSpacing.Md))
            rows.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.72f))
                    Text(item.leftValue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(item.rightValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(JoharSpacing.Sm))
            }
            recommendation?.let {
                Spacer(Modifier.height(JoharSpacing.Md))
                Surface(
                    shape = RoundedCornerShape(JoharRadius.Large),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                ) {
                    Column(Modifier.padding(JoharSpacing.Lg)) {
                        Text("JOHAR PICK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(JoharSpacing.Xs))
                        Text(it, style = MaterialTheme.typography.titleSmall)
                    }
                }
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
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(JoharSpacing.Md))
        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(min = 0.dp),
                onClick = { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(JoharRadius.Large),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.68f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = JoharSpacing.Lg, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text("→", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(JoharSpacing.Sm))
        }
    }
}
