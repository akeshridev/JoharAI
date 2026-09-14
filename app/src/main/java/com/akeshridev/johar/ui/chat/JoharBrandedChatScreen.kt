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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharColors
import com.akeshridev.johar.designsystem.JoharDrummerMascot
import com.akeshridev.johar.designsystem.JoharInfoCard
import com.akeshridev.johar.designsystem.JoharItineraryCard
import com.akeshridev.johar.designsystem.JoharNagadaThinkingBubble
import com.akeshridev.johar.designsystem.JoharPlaceCard
import com.akeshridev.johar.designsystem.JoharPrimaryButton
import com.akeshridev.johar.designsystem.JoharRouteCard
import com.akeshridev.johar.designsystem.JoharSuggestionCard
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
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isFreshConversation = messages.size == 1 && messages.firstOrNull()?.id == "welcome"

    LaunchedEffect(messages.size, isThinking) {
        val extraItem = if (isThinking) 1 else 0
        val target = messages.size + extraItem - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            JoharChatHeader()

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    when (message.sender) {
                        Sender.USER -> UserBubble(message.content)
                        Sender.JOHAR -> JoharBubble(message.content, onAction)
                    }
                }

                if (isFreshConversation) {
                    item(key = "starter-suggestions") {
                        Column(
                            modifier = Modifier.padding(top = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Start with something local",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            JoharSuggestionCard(
                                title = "Try asking Johar",
                                suggestions = listOf(
                                    "Parents ke saath kam walking wali jagah?",
                                    "Lalpur ke paas achha local food kya hai?",
                                    "Ranchi ke paas peaceful waterfall batao",
                                ),
                                onSuggestionClick = onSend,
                            )
                        }
                    }
                }

                if (isThinking) {
                    item(key = "thinking") {
                        JoharNagadaThinkingBubble(label = "Johar is thinking…")
                    }
                }
            }

            JoharComposer(
                value = input,
                enabled = !isThinking,
                onValueChange = { input = it },
                onSend = {
                    val query = input.trim()
                    if (query.isNotEmpty()) {
                        onSend(query)
                        input = ""
                    }
                },
            )
        }
    }
}

@Composable
private fun JoharChatHeader() {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = JoharColors.SoftOrange,
            ) {
                JoharDrummerMascot(
                    modifier = Modifier.padding(6.dp),
                    size = 42.dp,
                    animated = false,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Johar",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
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
                    text = "Local • grounded • offline-first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything about Ranchi…") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                ),
            )
            JoharPrimaryButton(
                label = "Send",
                enabled = value.isNotBlank() && enabled,
                onClick = onSend,
            )
        }
    }
}

@Composable
private fun UserBubble(content: JoharContent) {
    val text = (content as? JoharContent.Text)?.text ?: return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun JoharBubble(content: JoharContent, onAction: (JoharCardAction) -> Unit) {
    Column(Modifier.fillMaxWidth(0.94f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (content) {
            is JoharContent.Text -> Text(
                text = content.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            is JoharContent.Places -> {
                content.intro?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                content.items.forEach {
                    JoharPlaceCard(model = it, onAction = onAction)
                }
            }
            is JoharContent.Route -> JoharRouteCard(model = content.route, onAction = onAction)
            is JoharContent.Itinerary -> JoharItineraryCard(model = content.plan, onAction = onAction)
            is JoharContent.Info -> JoharInfoCard(
                title = content.title,
                text = content.text,
                tone = content.tone,
                actions = content.actions,
                onAction = onAction,
            )
        }
    }
}
