package com.akeshridev.johar.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.designsystem.JoharInfoCard
import com.akeshridev.johar.designsystem.JoharItineraryCard
import com.akeshridev.johar.designsystem.JoharNagadaThinkingBubble
import com.akeshridev.johar.designsystem.JoharPlaceCard
import com.akeshridev.johar.designsystem.JoharPrimaryButton
import com.akeshridev.johar.designsystem.JoharRouteCard
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
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Johar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Ranchi AI - Ask anything about Ranchi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    when (message.sender) {
                        Sender.USER -> UserBubble(message.content)
                        Sender.JOHAR -> JoharBubble(message.content, onAction)
                    }
                }
                if (isThinking) {
                    item(key = "thinking") {
                        JoharNagadaThinkingBubble(label = "Johar is thinking...")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask anything about Ranchi...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                )
                JoharPrimaryButton(
                    label = "Send",
                    enabled = input.isNotBlank() && !isThinking,
                    onClick = {
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
            Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun JoharBubble(content: JoharContent, onAction: (JoharCardAction) -> Unit) {
    Column(Modifier.fillMaxWidth(0.92f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (content) {
            is JoharContent.Text -> Text(content.text, style = MaterialTheme.typography.bodyLarge)
            is JoharContent.Places -> {
                content.intro?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                content.items.forEach { JoharPlaceCard(model = it, onAction = onAction) }
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
