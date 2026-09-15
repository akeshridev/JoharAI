package com.akeshridev.johar

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshridev.johar.designsystem.JoharSplashScreen
import com.akeshridev.johar.designsystem.JoharTheme
import com.akeshridev.johar.di.JoharGraph
import com.akeshridev.johar.ui.chat.JoharBrandedChatScreen
import com.akeshridev.johar.ui.chat.JoharChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JoharActivity : ComponentActivity() {
    private val graph by lazy { JoharGraph(applicationContext) }

    private val chatViewModel by lazy {
        ViewModelProvider(
            this,
            JoharChatViewModel.Factory(graph.conversationRouter()),
        )[JoharChatViewModel::class.java]
    }

    private val isDebuggable: Boolean
        get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JoharTheme {
                JoharRoot(
                    viewModel = chatViewModel,
                    onDeveloperCrawlRanchi = if (isDebuggable) {
                        { graph.scheduleSourceCrawlUseCase() }
                    } else {
                        null
                    },
                    enablePrototypeRunner = isDebuggable,
                )
            }
        }
    }

    /** Test harness hook: reset only conversation/session state while keeping this Activity warm. */
    fun resetConversationForTesting() {
        chatViewModel.resetConversationForTesting()
    }
}

@Composable
private fun JoharRoot(
    viewModel: JoharChatViewModel,
    onDeveloperCrawlRanchi: (() -> Unit)? = null,
    enablePrototypeRunner: Boolean = false,
) {
    var showSplash by remember { mutableStateOf(true) }
    var isPrototypeRunning by remember { mutableStateOf(false) }
    var demoDraft by remember { mutableStateOf<String?>(null) }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(1100)
        showSplash = false
    }

    if (showSplash) {
        JoharSplashScreen()
    } else {
        Box(Modifier.fillMaxSize()) {
            JoharBrandedChatScreen(
                messages = messages,
                isThinking = isThinking,
                onSend = viewModel::sendQuery,
                onAction = viewModel::onAction,
                demoDraft = demoDraft,
            )

            if (onDeveloperCrawlRanchi != null || enablePrototypeRunner) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    onDeveloperCrawlRanchi?.let { onCrawl ->
                        TextButton(onClick = onCrawl) {
                            Text("DEV: Crawl Ranchi")
                        }
                    }

                    if (enablePrototypeRunner) {
                        TextButton(
                            enabled = !isPrototypeRunning && !isThinking,
                            onClick = {
                                isPrototypeRunning = true
                                scope.launch {
                                    try {
                                        PROTOTYPE_STEPS.forEach { step ->
                                            demoDraft = ""
                                            step.question.forEachIndexed { index, character ->
                                                demoDraft = step.question.substring(0, index + 1)
                                                delay(typingDelayMillis(character))
                                            }
                                            delay(PROTOTYPE_BEFORE_SEND_DELAY_MILLIS)
                                            viewModel.sendQuery(step.question)
                                            demoDraft = null
                                            viewModel.isThinking.first { thinking -> !thinking }
                                            if (step.openFirstMapResult) {
                                                viewModel.triggerFirstMapActionForLatestPlaces()
                                            }
                                            delay(PROTOTYPE_QUESTION_DELAY_MILLIS)
                                        }
                                    } finally {
                                        demoDraft = null
                                        isPrototypeRunning = false
                                    }
                                }
                            },
                        ) {
                            Text(if (isPrototypeRunning) "DEV: Demo running…" else "DEV: Run 9Q Demo")
                        }
                    }
                }
            }
        }
    }
}

private fun typingDelayMillis(character: Char): Long = when {
    character == ' ' -> 85L
    character in ",.?" -> 150L
    else -> 48L + (character.code % 4) * 9L
}

private const val PROTOTYPE_BEFORE_SEND_DELAY_MILLIS = 450L
private const val PROTOTYPE_QUESTION_DELAY_MILLIS = 3_000L

private data class PrototypeStep(
    val question: String,
    val openFirstMapResult: Boolean = false,
)

private val PROTOTYPE_STEPS = listOf(
    PrototypeStep("Ranchi mein peaceful family place suggest karo, parents ke saath jana hai"),
    PrototypeStep("Kanke Dam family ke liye acha hai?"),
    PrototypeStep("Ranchi mein waterfall kahan hai?"),
    PrototypeStep("Main Road ke paas restaurant batao"),
    PrototypeStep("Dhuska kya hota hai?"),
    PrototypeStep("RIMS kahan hai?"),
    PrototypeStep("Ranchi railway station se Tagore Hill kaise jaun?"),
    PrototypeStep(
        question = "Lalpur ke paas ATM batao",
        openFirstMapResult = true,
    ),
    PrototypeStep("Aaj Kanke Dam open hai?"),
)
