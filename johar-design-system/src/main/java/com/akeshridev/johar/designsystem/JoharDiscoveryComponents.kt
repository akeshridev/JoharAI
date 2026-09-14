package com.akeshridev.johar.designsystem

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
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Spacer(Modifier.height(JoharSpacing.Xs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted)
        }
        Spacer(Modifier.height(JoharSpacing.Md))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Md),
            contentPadding = PaddingValues(end = JoharSpacing.Lg),
        ) {
            items(places) { place ->
                JoharPlaceCard(
                    model = place,
                    modifier = Modifier.width(280.dp),
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
        shape = RoundedCornerShape(JoharRadius.Large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(JoharColors.SoftGreen),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(JoharSpacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Map preview", style = MaterialTheme.typography.labelMedium, color = JoharColors.Muted)
                markerLabel?.let {
                    Spacer(Modifier.height(JoharSpacing.Xs))
                    JoharStatusBadge(label = it, tone = JoharInfoTone.VERIFIED)
                }
            }
        }
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JoharColors.Muted) }
            routeSummary?.let {
                Spacer(Modifier.height(JoharSpacing.Sm))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            action?.let {
                Spacer(Modifier.height(JoharSpacing.Lg))
                JoharActionRow(actions = listOf(it), onAction = onAction)
            }
        }
    }
}

@Composable
fun JoharSourceRow(
    sourceName: String,
    label: String = "Source",
    verified: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = JoharColors.Muted)
        Text(sourceName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (verified) JoharStatusBadge(label = "Verified", tone = JoharInfoTone.VERIFIED)
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
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(JoharSpacing.Md))
        Row(horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm)) {
            options.take(4).forEach { option ->
                JoharChip(label = option, onClick = { onOptionClick(option) })
            }
        }
    }
}

@Composable
fun JoharConstraintSummary(
    title: String = "Your preferences",
    constraints: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(JoharRadius.Large),
        color = JoharColors.Cream,
    ) {
        Column(Modifier.padding(JoharSpacing.Lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(JoharSpacing.Md))
            constraints.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(JoharSpacing.Sm)) {
                    rowItems.forEach { item -> JoharChip(label = item) }
                }
                Spacer(Modifier.height(JoharSpacing.Sm))
            }
        }
    }
}
