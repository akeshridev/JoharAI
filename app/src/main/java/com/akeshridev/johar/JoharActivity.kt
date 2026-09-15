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
                delay(typingDelayMillis(character))
            }

            delay(PROTOTYPE_BEFORE_SEND_DELAY_MILLIS)
            viewModel.sendQuery(step.question)
            demoDraft = null
            viewModel.isThinking.first { thinking -> !thinking }

            if (step.openFirstMapResult) {
                viewModel.triggerFirstMapActionForLatestPlaces()
                delay(PROTOTYPE_MAP_HOLD_MILLIS)
            } else {
                delay(PROTOTYPE_QUESTION_DELAY_MILLIS)
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
        )
    }
}

private fun typingDelayMillis(character: Char): Long = when {
    character == ' ' -> 85L
    character in ",.?" -> 150L
    else -> 48L + (character.code % 4) * 9L
}

private const val SPLASH_DURATION_MILLIS = 3_000L
private const val PROTOTYPE_START_DELAY_MILLIS = 3_000L
private const val PROTOTYPE_BEFORE_SEND_DELAY_MILLIS = 450L
private const val PROTOTYPE_QUESTION_DELAY_MILLIS = 3_000L
private const val PROTOTYPE_MAP_HOLD_MILLIS = 5_500L

private data class PrototypeStep(
    val question: String,
    val openFirstMapResult: Boolean = false,
)

private val PROTOTYPE_STEPS = listOf(
    PrototypeStep(
        question = "Lalpur ke paas ATM batao",
        openFirstMapResult = true,
    ),
    PrototypeStep("Ranchi mein peaceful family place suggest karo, parents ke saath jana hai"),
    PrototypeStep("Main Road ke paas restaurant batao"),
    PrototypeStep("Emergency hospital number"),
    PrototypeStep("Ranchi railway station se Tagore Hill kaise jaun?"),
    PrototypeStep("Dhuska kya hota hai?"),
    PrototypeStep("Kanke Dam family ke liye acha hai?"),
    PrototypeStep("Ranchi mein local market ya haat batao"),
    PrototypeStep("Aaj Kanke Dam open hai?"),
)
