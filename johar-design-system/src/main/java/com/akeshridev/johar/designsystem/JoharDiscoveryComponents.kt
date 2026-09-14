package com.akeshridev.johar.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
fun JoharPlaceCarousel(
    title: String,
    subtitle: String? = null,
    places: List<JoharPlaceCardModel>,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        subtitle?.let {
            Spacer(Modifier.height(JoharSpacing.Xs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(JoharSpacing.Md))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Md),
            contentPadding = PaddingValues(end = JoharSpacing.Xl),
        ) {
            items(places) { place ->
                JoharPlaceCard(
                    model = place,
                    modifier = Modifier.width(248.dp),
                    density = JoharCardDensity.COMPACT,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
fun JoharMapPreviewCard(
    title: String,
    subtitle: String? = null,
    markerLabel: String? = null,
    routeSummary: String? = null,
    action: JoharCardAction? = null,
    modifier: Modifier = Modifier,
    onAction: (JoharCardAction) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.ExtraLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(190.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), RoundedCornerShape(JoharRadius.Pill)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .height(88.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f), RoundedCornerShape(JoharRadius.Pill)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(18.dp)
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(top = 62.dp),
                shape = RoundedCornerShape(JoharRadius.Pill),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    markerLabel ?: "Selected place",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = JoharSpacing.Md, vertical = 6.dp),
                )
            }
            Text(
                "Ranchi • offline map preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart).padding(JoharSpacing.Md),
            )
        }
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            routeSummary?.let {
                Spacer(Modifier.height(JoharSpacing.Sm))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            action?.let {
                Spacer(Modifier.height(JoharSpacing.Md))
                JoharActionRow(actions = listOf(it), onAction = onAction)
            }
        }
    }
}

@Composable
fun JoharSourceRow(
    sourceName: String,
    label: String = "Grounded in",
    verified: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = JoharSpacing.Md, vertical = JoharSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(sourceName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            if (verified) JoharStatusBadge(label = "Verified", tone = JoharInfoTone.VERIFIED)
        }
    }
}

@Composable
fun JoharPreferenceChips(
    title: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onOptionClick: (String) -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(JoharSpacing.Sm))
        options.chunked(2).forEachIndexed { index, rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm)) {
                rowOptions.forEach { option ->
                    JoharChip(label = option, onClick = { onOptionClick(option) })
                }
            }
            if (index != options.chunked(2).lastIndex) Spacer(Modifier.height(JoharSpacing.Sm))
        }
    }
}

@Composable
fun JoharConstraintSummary(
    title: String = "Johar understood",
    constraints: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.ExtraLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text(
                "YOUR PLAN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(JoharSpacing.Xs))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(JoharSpacing.Md))
            constraints.chunked(2).forEachIndexed { index, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm)) {
                    rowItems.forEach { item -> JoharChip(label = item, selected = true) }
                }
                if (index != constraints.chunked(2).lastIndex) Spacer(Modifier.height(JoharSpacing.Sm))
            }
        }
    }
}
