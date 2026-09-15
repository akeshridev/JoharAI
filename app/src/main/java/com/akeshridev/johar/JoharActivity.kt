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
                                        PROTOTYPE_QUESTIONS.forEach { question ->
                                            viewModel.sendQuery(question)
                                            viewModel.isThinking.first { thinking -> !thinking }
                                            delay(PROTOTYPE_QUESTION_DELAY_MILLIS)
                                        }
                                    } finally {
                                        isPrototypeRunning = false
                                    }
                                }
                            },
                        ) {
                            Text(if (isPrototypeRunning) "DEV: Demo running…" else "DEV: Run 8Q Demo")
                        }
                    }
                }
            }
        }
    }
}

private const val PROTOTYPE_QUESTION_DELAY_MILLIS = 3_000L

private val PROTOTYPE_QUESTIONS = listOf(
    "Ranchi mein peaceful family place suggest karo, parents ke saath jana hai",
    "Kanke Dam family ke liye acha hai?",
    "Ranchi mein waterfall kahan hai?",
    "Main Road ke paas restaurant batao",
    "Dhuska kya hota hai?",
    "RIMS kahan hai?",
    "Ranchi railway station se Tagore Hill kaise jaun?",
    "Aaj Kanke Dam open hai?",
)
