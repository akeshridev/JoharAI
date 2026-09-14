package com.akeshridev.johar.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JoharPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(JoharRadius.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = JoharColors.Forest,
            contentColor = JoharColors.Cream,
        ),
    ) {
        Text(label)
    }
}

@Composable
fun JoharSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(JoharRadius.Medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(label)
    }
}

@Composable
fun JoharChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    AssistChip(
        onClick = onClick ?: {},
        modifier = modifier,
        label = { Text(label) },
        shape = RoundedCornerShape(JoharRadius.Pill),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) JoharColors.Sand else MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
fun JoharStatusBadge(
    label: String,
    tone: JoharInfoTone = JoharInfoTone.NORMAL,
    modifier: Modifier = Modifier,
) {
    val background = when (tone) {
        JoharInfoTone.LIVE, JoharInfoTone.VERIFIED -> JoharColors.SoftGreen
        JoharInfoTone.WARNING, JoharInfoTone.NOT_CONFIRMED -> JoharColors.SoftOrange
        JoharInfoTone.OFFLINE -> JoharColors.Sand
        JoharInfoTone.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(JoharRadius.Pill),
        color = background,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = JoharSpacing.Sm, vertical = JoharSpacing.Xs),
        )
    }
}

@Composable
fun JoharActionRow(
    actions: List<JoharCardAction>,
    onAction: (JoharCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm),
    ) {
        actions.take(2).forEach { action ->
            if (action.primary) {
                JoharPrimaryButton(label = action.label, onClick = { onAction(action) })
            } else {
                JoharSecondaryButton(label = action.label, onClick = { onAction(action) })
            }
        }
    }
}
