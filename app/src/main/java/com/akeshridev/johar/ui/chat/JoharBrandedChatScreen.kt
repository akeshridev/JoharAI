package com.akeshridev.johar.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
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
import kotlinx.coroutines.delay

@Composable
fun JoharBrandedChatScreen(
    messages: List<JoharMessageUiModel>,
    isThinking: Boolean,
    onSend: (String) -> Unit,
    onAction: (JoharCardAction) -> Unit,
    modifier: Modifier = Modifier,
    demoDraft: String? = null,
    demoSendPressToken: Int = 0,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val displayedInput = demoDraft ?: input
    val listState = rememberLazyListState()

    // The ViewModel keeps a synthetic welcome message for test/session compatibility. It is an
    // application-state marker, not a real chat turn, so never render it as another answer bubble.
    val visibleMessages = messages.filterNot { it.id == "welcome" }
    val isFreshConversation = visibleMessages.isEmpty()
    val latestJoharId = visibleMessages.lastOrNull { it.sender == Sender.JOHAR }?.id

    // Let new content settle for a beat, then follow it with Compose's smooth list animation.
    // This feels closer to a person reading a chat than instantly snapping to the newest item.
    LaunchedEffect(visibleMessages.size, isThinking) {
        val extraItem = if (isThinking) 1 else 0
        val heroItem = if (isFreshConversation) 1 else 0
        val target = visibleMessages.size + extraItem + heroItem - 1
        if (target >= 0) {
            delay(90L)
            listState.animateScrollToItem(target)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(JoharTestTags.ROOT),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            JoharChatHeader(compactIdentity = !isFreshConversation)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .testTag(JoharTestTags.CHAT_LIST),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (isFreshConversation) {
                    item(key = "welcome-hero") {
                        JoharWelcomeHero(onSend = onSend)
                    }
                }

                items(visibleMessages, key = { it.id }) { message ->
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
                demoSendPressToken = demoSendPressToken,
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
private fun JoharChatHeader(compactIdentity: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = if (compactIdentity) 9.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (compactIdentity) {
                JoharRanchiSatelliteEmblem(
                    size = 44.dp,
                    animated = true,
                )
                Spacer(Modifier.width(10.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = "Johar AI",
                    style = if (compactIdentity) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (compactIdentity) "Ranchi • offline-first" else "offline-first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun JoharWelcomeHero(onSend: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            JoharRanchiSatelliteEmblem(size = 86.dp, animated = true)

            Text(
                text = "Johar! 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ranchi ke baare mein kya jaana hai?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Places • Food • Routes • Local help",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            JoharSuggestionCard(
                title = "Try asking",
                suggestions = listOf(
                    "Lalpur ke paas ATM batao",
                    "Ranchi mein kids ke liye park suggest karo",
                    "Emergency hospital number",
                ),
                onSuggestionClick = onSend,
            )
        }
    }
}

@Composable
private fun JoharComposer(
    value: String,
    enabled: Boolean,
    demoSendPressToken: Int,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val sendInteractionSource = remember { MutableInteractionSource() }
    var sendButtonSize by remember { mutableStateOf(IntSize.Zero) }

    // Demo-only visual tap. It drives the very same Material interaction source as a finger press,
    // so ripple, elevation and press-scale are captured naturally by screen recording.
    LaunchedEffect(demoSendPressToken) {
        if (demoSendPressToken <= 0 || !enabled || value.isBlank()) return@LaunchedEffect
        val size = sendButtonSize
        val position = if (size.width > 0 && size.height > 0) {
            Offset(size.width / 2f, size.height / 2f)
        } else {
            Offset.Zero
        }
        val press = PressInteraction.Press(position)
        sendInteractionSource.emit(press)
        delay(145L)
        sendInteractionSource.emit(PressInteraction.Release(press))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                JoharPrimaryButton(
                    label = "Send",
                    modifier = Modifier
                        .testTag(JoharTestTags.SEND_BUTTON)
                        .onSizeChanged { sendButtonSize = it },
                    enabled = value.isNotBlank() && enabled,
                    interactionSource = sendInteractionSource,
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
