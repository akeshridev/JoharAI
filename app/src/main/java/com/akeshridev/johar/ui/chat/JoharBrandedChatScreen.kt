package com.akeshridev.johar.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharColors
import com.akeshridev.johar.designsystem.JoharComparisonCard
import com.akeshridev.johar.designsystem.JoharInfoCard
import com.akeshridev.johar.designsystem.JoharInfoTone
import com.akeshridev.johar.designsystem.JoharItineraryCard
import com.akeshridev.johar.designsystem.JoharNagadaThinkingBubble
import com.akeshridev.johar.designsystem.JoharPlaceCarousel
import com.akeshridev.johar.designsystem.JoharPlaceCard
import com.akeshridev.johar.designsystem.JoharPreferenceChips
import com.akeshridev.johar.designsystem.JoharPrimaryButton
import com.akeshridev.johar.designsystem.JoharRanchiSatelliteEmblem
import com.akeshridev.johar.designsystem.JoharSourceRow
import com.akeshridev.johar.designsystem.JoharSuggestionCard
import com.akeshridev.johar.designsystem.JoharUtilityCard
import com.akeshridev.johar.map.RanchiMapCard
import com.akeshridev.johar.map.RanchiRouteMapCard
import com.akeshridev.johar.ui.model.JoharContent
import com.akeshridev.johar.ui.model.JoharMessageUiModel
import com.akeshridev.johar.ui.model.Sender

@Composable
fun JoharBrandedChatScreen(
    messages: List<JoharMessageUiModel>,
    isThinking: Boolean,
    onSend: (String) -> Unit,
    onAction: (JoharCardAction) -> Unit,
    modifier: Modifier = Modifier,
    demoDraft: String? = null,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val displayedInput = demoDraft ?: input
    val listState = rememberLazyListState()
    val isFreshConversation = messages.size == 1 && messages.firstOrNull()?.id == "welcome"
    val latestJoharId = messages.lastOrNull { it.sender == Sender.JOHAR }?.id

    LaunchedEffect(messages.size, isThinking) {
        val extraItem = if (isThinking) 1 else 0
        val target = messages.size + extraItem - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(JoharTestTags.ROOT),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            JoharChatHeader()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .testTag(JoharTestTags.CHAT_LIST),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    when (message.sender) {
                        Sender.USER -> UserBubble(message.content)
                        Sender.JOHAR -> JoharBubble(
                            content = message.content,
                            onAction = onAction,
                            onSend = onSend,
                            modifier = Modifier.testTag(
                                if (message.id == latestJoharId) JoharTestTags.LATEST_ANSWER else JoharTestTags.ANSWER,
                            ),
                        )
                    }
                }

                if (isFreshConversation) {
                    item(key = "starter-suggestions") {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = JoharColors.White,
                            border = BorderStroke(1.dp, JoharColors.Sand.copy(alpha = 0.85f)),
                            shadowElevation = 3.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    JoharRanchiSatelliteEmblem(size = 52.dp, animated = true)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Ask Johar about Ranchi",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = JoharColors.Forest,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "Places, local food, routes, services and everyday help.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = JoharColors.Muted,
                                        )
                                    }
                                }
                                JoharSuggestionCard(
                                    title = "Try one",
                                    suggestions = listOf(
                                        "Lalpur ke paas ATM batao",
                                        "Emergency hospital number",
                                        "Ranchi mein peaceful family place suggest karo",
                                    ),
                                    onSuggestionClick = onSend,
                                )
                            }
                        }
                    }
                }

                if (isThinking) {
                    item(key = "thinking") {
                        JoharNagadaThinkingBubble(
                            modifier = Modifier.testTag(JoharTestTags.THINKING),
                            label = "Johar is thinking…",
                        )
                    }
                }
            }

            JoharComposer(
                value = displayedInput,
                enabled = !isThinking,
                onValueChange = { if (demoDraft == null) input = it },
                onSend = {
                    val query = displayedInput.trim()
                    if (query.isNotEmpty()) {
                        onSend(query)
                        if (demoDraft == null) input = ""
                    }
                },
            )
        }
    }
}

@Composable
private fun JoharChatHeader() {
    Surface(
        color = JoharColors.White,
        border = BorderStroke(1.dp, JoharColors.Sand.copy(alpha = 0.65f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JoharRanchiSatelliteEmblem(
                size = 54.dp,
                animated = true,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Johar AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = JoharColors.Forest,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = JoharColors.SoftGreen,
                    ) {
                        Text(
                            text = "RANCHI AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = JoharColors.Forest,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    text = "Jharkhand in Your Hands • offline-first",
                    style = MaterialTheme.typography.bodySmall,
                    color = JoharColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun JoharComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = JoharColors.SoftGreen,
        border = BorderStroke(1.dp, JoharColors.Green.copy(alpha = 0.30f)),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "Ask Johar • local answers, even offline",
                style = MaterialTheme.typography.labelMedium,
                color = JoharColors.Forest.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(JoharTestTags.CHAT_INPUT),
                    placeholder = { Text("Ask anything about Ranchi…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = JoharColors.White,
                        focusedContainerColor = JoharColors.White,
                        disabledContainerColor = JoharColors.White.copy(alpha = 0.90f),
                        unfocusedTextColor = JoharColors.Forest,
                        focusedTextColor = JoharColors.Forest,
                        disabledTextColor = JoharColors.Forest.copy(alpha = 0.75f),
                        unfocusedPlaceholderColor = JoharColors.Muted,
                        focusedPlaceholderColor = JoharColors.Muted,
                        unfocusedBorderColor = JoharColors.Green.copy(alpha = 0.38f),
                        focusedBorderColor = JoharColors.Forest,
                    ),
                )
                JoharPrimaryButton(
                    label = "Send",
                    modifier = Modifier.testTag(JoharTestTags.SEND_BUTTON),
                    enabled = value.isNotBlank() && enabled,
                    onClick = onSend,
                )
            }
        }
    }
}

@Composable
private fun UserBubble(content: JoharContent) {
    val text = when (content) {
        is JoharContent.Text -> content.text
        is JoharContent.Grounded -> content.text
        is JoharContent.Info -> content.text
        else -> return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = JoharColors.SoftOrange,
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = JoharColors.Forest,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun JoharBubble(
    content: JoharContent,
    onAction: (JoharCardAction) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (content) {
            is JoharContent.Text -> JoharInfoCard(title = "Johar", text = content.text, tone = JoharInfoTone.NORMAL)
            is JoharContent.Grounded -> {
                JoharInfoCard(title = "Johar", text = content.text, tone = content.tone)
                content.sources.forEach { source ->
                    JoharSourceRow(sourceName = source.sourceName, verified = source.verified)
                }
            }
            is JoharContent.Places -> {
                content.intro?.let { JoharInfoCard(title = "Johar", text = it, tone = JoharInfoTone.NORMAL) }
                if (content.items.size == 1) {
                    JoharPlaceCard(model = content.items.single(), onAction = onAction)
                } else {
                    JoharPlaceCarousel(places = content.items, title = "Places", onAction = onAction)
                }
            }
            is JoharContent.Utilities -> {
                content.intro?.let { JoharInfoCard(title = "Johar", text = it, tone = JoharInfoTone.NORMAL) }
                content.items.forEach { item ->
                    JoharUtilityCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        metadata = item.metadata,
                        actions = item.actions,
                        onAction = onAction,
                    )
                }
            }
            is JoharContent.Route -> RanchiRouteMapCard(
                model = content.route,
                origin = content.origin,
                destination = content.destination,
                route = content.routePoints,
            )
            is JoharContent.Itinerary -> JoharItineraryCard(model = content.plan, onAction = onAction)
            is JoharContent.Comparison -> JoharComparisonCard(
                leftTitle = content.leftTitle,
                rightTitle = content.rightTitle,
                rows = content.rows,
                recommendation = content.recommendation,
            )
            is JoharContent.Clarification -> {
                JoharInfoCard(title = "Johar", text = content.prompt, tone = JoharInfoTone.NORMAL)
                JoharPreferenceChips(title = "Choose one", options = content.options, onOptionClick = onSend)
            }
            is JoharContent.Info -> JoharInfoCard(
                title = content.title,
                text = content.text,
                tone = content.tone,
                actions = content.actions,
                onAction = onAction,
            )
            is JoharContent.Map -> RanchiMapCard(destination = content.destination)
        }
    }
}
