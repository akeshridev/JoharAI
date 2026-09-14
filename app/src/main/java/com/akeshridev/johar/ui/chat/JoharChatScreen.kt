package com.akeshridev.johar.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.akeshridev.johar.designsystem.JoharCream
import com.akeshridev.johar.designsystem.JoharForest
import com.akeshridev.johar.designsystem.JoharInfoCard
import com.akeshridev.johar.designsystem.JoharItineraryCard
import com.akeshridev.johar.designsystem.JoharPlaceCard
import com.akeshridev.johar.designsystem.JoharRouteCard
import com.akeshridev.johar.ui.model.JoharContent
import com.akeshridev.johar.ui.model.JoharMessageUiModel
import com.akeshridev.johar.ui.model.Sender

@Composable
fun JoharChatScreen(
    messages: List<JoharMessageUiModel>,
    onSend: (String) -> Unit,
    onAction: (JoharCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }

    Surface(modifier = modifier.fillMaxSize(), color = JoharCream) {
        Column(Modifier.fillMaxSize()) {
            JoharTopBar()

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    JoharMessage(message = message, onAction = onAction)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask anything about Ranchi…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                )
                Button(
                    enabled = input.isNotBlank(),
                    onClick = {
                        val query = input.trim()
                        if (query.isNotEmpty()) {
                            onSend(query)
                            input = ""
                        }
                    },
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun JoharTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .background(JoharForest, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text("जो", color = JoharCream, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(
                "Johar — Ranchi AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Ask anything about Ranchi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun JoharMessage(
    message: JoharMessageUiModel,
    onAction: (JoharCardAction) -> Unit,
) {
    when (message.sender) {
        Sender.USER -> UserMessage(message.content)
        Sender.JOHAR -> JoharResponse(message.content, onAction)
    }
}

@Composable
private fun UserMessage(content: JoharContent) {
    val text = (content as? JoharContent.Text)?.text ?: return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = JoharForest,
            contentColor = JoharCream,
            shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun JoharResponse(
    content: JoharContent,
    onAction: (JoharCardAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.92f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (content) {
            is JoharContent.Text -> AssistantText(content.text)
            is JoharContent.Places -> {
                content.intro?.let { AssistantText(it) }
                content.items.forEach { place ->
                    JoharPlaceCard(model = place, onAction = onAction)
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

@Composable
private fun AssistantText(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
