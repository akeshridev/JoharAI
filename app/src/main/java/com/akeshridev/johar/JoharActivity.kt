package com.akeshridev.johar

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshridev.johar.designsystem.JoharSplashScreen
import com.akeshridev.johar.designsystem.JoharTheme
import com.akeshridev.johar.di.JoharGraph
import com.akeshridev.johar.ui.chat.JoharBrandedChatScreen
import com.akeshridev.johar.ui.chat.JoharChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

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
                    autoRunPrototype = isDebuggable,
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
    autoRunPrototype: Boolean = false,
) {
    var showSplash by remember { mutableStateOf(true) }
    var demoDraft by remember { mutableStateOf<String?>(null) }
    var demoSendPressToken by remember { mutableStateOf(0) }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MILLIS)
        showSplash = false
    }

    LaunchedEffect(showSplash, autoRunPrototype) {
        if (showSplash || !autoRunPrototype) return@LaunchedEffect
        delay(PROTOTYPE_START_DELAY_MILLIS)

        PROTOTYPE_STEPS.forEach { step ->
            demoDraft = ""
            step.question.forEachIndexed { index, character ->
                demoDraft = step.question.substring(0, index + 1)
                delay(typingDelayMillis(character, index))
            }

            delay(PROTOTYPE_BEFORE_SEND_DELAY_MILLIS)
            demoSendPressToken += 1
            delay(PROTOTYPE_SEND_PRESS_DURATION_MILLIS)

            viewModel.sendQuery(step.question)
            demoDraft = null
            viewModel.isThinking.first { thinking -> !thinking }

            if (step.openFirstMapResult) {
                delay(PROTOTYPE_BEFORE_ACTION_DELAY_MILLIS)
                viewModel.triggerFirstMapActionForLatestPlaces()
                delay(PROTOTYPE_MAP_HOLD_MILLIS)
            } else {
                delay(step.holdMillis ?: readingPauseMillis(step.question))
            }
        }
    }

    if (showSplash) {
        JoharSplashScreen(
            title = "Johar AI",
            subtitle = "Jharkhand in Your Hands",
            tagline = "Local knowledge • Offline-first • Built for Jharkhand",
        )
    } else {
        JoharBrandedChatScreen(
            messages = messages,
            isThinking = isThinking,
            onSend = viewModel::sendQuery,
            onAction = viewModel::onAction,
            demoDraft = demoDraft,
            demoSendPressToken = demoSendPressToken,
        )
    }
}

private fun typingDelayMillis(character: Char, index: Int): Long = when {
    character == ' ' -> 78L + (index % 3) * 9L
    character in ",.?" -> 155L + (index % 2) * 35L
    index > 0 && index % 14 == 0 -> 118L
    else -> 46L + (character.code % 5) * 8L
}

private fun readingPauseMillis(question: String): Long =
    (2_350L + question.length * 18L).coerceIn(2_600L, 3_650L)

private const val SPLASH_DURATION_MILLIS = 3_000L
private const val PROTOTYPE_START_DELAY_MILLIS = 3_000L
private const val PROTOTYPE_BEFORE_SEND_DELAY_MILLIS = 460L
private const val PROTOTYPE_SEND_PRESS_DURATION_MILLIS = 240L
private const val PROTOTYPE_BEFORE_ACTION_DELAY_MILLIS = 1_100L
private const val PROTOTYPE_MAP_HOLD_MILLIS = 4_200L

private data class PrototypeStep(
    val question: String,
    val openFirstMapResult: Boolean = false,
    val holdMillis: Long? = null,
)

private val PROTOTYPE_STEPS = listOf(
    PrototypeStep(
        question = "Lalpur ke paas ATM batao",
        openFirstMapResult = true,
    ),
    PrototypeStep(
        question = "Ranchi mein kids ke liye park suggest karo",
        holdMillis = 3_000L,
    ),
    PrototypeStep(
        question = "Emergency hospital number",
        holdMillis = 3_300L,
    ),
    PrototypeStep(
        question = "Ranchi railway station se Tagore Hill kaise jaun?",
        holdMillis = 4_000L,
    ),
    PrototypeStep(
        question = "Dhuska kya hota hai?",
        holdMillis = 3_200L,
    ),
    PrototypeStep(
        question = "Aaj Kanke Dam open hai?",
        holdMillis = 3_600L,
    ),
)
