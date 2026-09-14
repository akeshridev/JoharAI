package com.akeshridev.johar.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akeshridev.johar.data.retrieval.DeterministicJoharAnswerGenerator
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import com.akeshridev.johar.ui.model.JoharContent
import com.akeshridev.johar.ui.model.JoharMessageUiModel
import com.akeshridev.johar.ui.model.Sender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class JoharChatViewModel(
    offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
) : ViewModel() {

    private val answerGenerator = DeterministicJoharAnswerGenerator(offlineKnowledgeRetriever)

    private val _messages = MutableStateFlow(
        listOf(
            JoharMessageUiModel(
                id = "welcome",
                sender = Sender.JOHAR,
                content = JoharContent.Text("Johar! 👋 Ranchi ke baare mein kya jaana hai?"),
            ),
        ),
    )
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

        viewModelScope.launch(Dispatchers.IO) {
            val content = runCatching {
                val answer = answerGenerator.answer(query)
                JoharContent.Text(answer.text)
            }.getOrElse {
                JoharContent.Text("Abhi answer nikalne mein dikkat aa rahi hai. Ek baar phir try karein.")
            }

            appendMessage(
                JoharMessageUiModel(
                    id = UUID.randomUUID().toString(),
                    sender = Sender.JOHAR,
                    content = content,
                ),
            )
            _isThinking.value = false
        }
    }

    private fun appendMessage(message: JoharMessageUiModel) {
        _messages.value = _messages.value + message
    }

    class Factory(
        private val offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(JoharChatViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return JoharChatViewModel(offlineKnowledgeRetriever) as T
        }
    }
}
