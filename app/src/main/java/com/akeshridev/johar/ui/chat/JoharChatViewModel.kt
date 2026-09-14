package com.akeshridev.johar.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akeshridev.johar.conversation.JoharContentMapper
import com.akeshridev.johar.conversation.JoharQueryRouter
import com.akeshridev.johar.conversation.JoharQueryResult
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.designsystem.JoharCardAction
import com.akeshridev.johar.ui.model.JoharContent
import com.akeshridev.johar.ui.model.JoharMessageUiModel
import com.akeshridev.johar.ui.model.Sender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class JoharChatViewModel(
    private val router: JoharQueryRouter,
) : ViewModel() {

    private val mapTargets = mutableMapOf<String, RanchiSpatialPlace>()

    private val _messages = MutableStateFlow(listOf(welcomeMessage()))
    val messages: StateFlow<List<JoharMessageUiModel>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    fun sendQuery(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty() || _isThinking.value) return

        appendMessage(
            JoharMessageUiModel(
                id = UUID.randomUUID().toString(),
                sender = Sender.USER,
                content = JoharContent.Text(query),
            ),
        )
        _isThinking.value = true

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { router.answer(query) }
                if (result is JoharQueryResult.Places) {
                    result.places.forEach { mapTargets[JoharContentMapper.mapActionId(it.id)] = it }
                }
                appendAnswer(JoharContentMapper.map(result))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("JoharChat", "Query failed", error)
                appendAnswer(JoharContent.Text("Abhi answer nikalne mein dikkat aa rahi hai. Ek baar phir try karein."))
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun onAction(action: JoharCardAction) {
        // Only actions from actual resolved results can select a map destination.
        val place = mapTargets[action.id] ?: return
        appendAnswer(JoharContent.Map(place))
    }

    /**
     * Evaluation-only session reset. Keeps repositories/router dependencies warm while
     * removing UI messages, map targets, and ephemeral router clarification context.
     */
    fun resetConversationForTesting() {
        check(!_isThinking.value) { "Cannot reset Johar while a query is still running." }
        mapTargets.clear()
        router.resetConversationState()
        _messages.value = listOf(welcomeMessage())
    }

    private fun appendAnswer(content: JoharContent) {
        appendMessage(JoharMessageUiModel(UUID.randomUUID().toString(), Sender.JOHAR, content))
    }

    private fun appendMessage(message: JoharMessageUiModel) {
        _messages.value = _messages.value + message
    }

    class Factory(
        private val router: JoharQueryRouter,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(JoharChatViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return JoharChatViewModel(router) as T
        }
    }

    companion object {
        private fun welcomeMessage() = JoharMessageUiModel(
            id = "welcome",
            sender = Sender.JOHAR,
            content = JoharContent.Text("Johar! 👋 Ranchi ke baare mein kya jaana hai?"),
        )
    }
}
