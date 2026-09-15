package com.akeshridev.johar

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
) {
    var showSplash by remember { mutableStateOf(true) }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()

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

            onDeveloperCrawlRanchi?.let { onCrawl ->
                TextButton(
                    onClick = onCrawl,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 8.dp),
                ) {
                    Text("DEV: Crawl Ranchi")
                }
            }
        }
    }
}
