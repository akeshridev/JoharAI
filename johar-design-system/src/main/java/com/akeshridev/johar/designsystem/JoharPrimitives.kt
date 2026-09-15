package com.akeshridev.johar.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun JoharPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 70 else 120),
        label = "johar-primary-button-scale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(JoharRadius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(JoharRadius.Pill),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.95f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 17.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(JoharRadius.Pill),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f),
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
    val content = when (tone) {
        JoharInfoTone.LIVE, JoharInfoTone.VERIFIED -> JoharColors.Forest
        JoharInfoTone.WARNING, JoharInfoTone.NOT_CONFIRMED -> JoharColors.Rust
        JoharInfoTone.OFFLINE -> JoharColors.Forest
        JoharInfoTone.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(JoharRadius.Pill),
        color = background,
        contentColor = content,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
