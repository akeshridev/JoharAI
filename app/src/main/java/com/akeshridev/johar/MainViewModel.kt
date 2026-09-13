package com.akeshridev.johar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import com.akeshridev.johar.data.retrieval.OfflineRagContextBuilder
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
    private val offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
) : ViewModel() {

    private val ragContextBuilder = OfflineRagContextBuilder(offlineKnowledgeRetriever)

    fun testOfflineRetrieval() {
        viewModelScope.launch(Dispatchers.IO) {
            TEST_QUERIES.forEach { query ->
                val context = ragContextBuilder.build(query)
                val hits = context.hits
                Log.i(TAG, "query=\"$query\" hits=${hits.size}")
                hits.forEachIndexed { index, hit ->
                    val factSummary = hit.facts
                        .take(3)
                        .joinToString(" | ") { fact -> "${fact.field}=${fact.value}" }
                    Log.i(
                        TAG,
                        "  #${index + 1} score=${hit.score} name=${hit.name} type=${hit.type} " +
                            "packType=${hit.packType.orEmpty()} description=${hit.description.orEmpty()} " +
                            "facts=[$factSummary]",
                    )
                }
                Log.i(TAG, "RAG_CONTEXT_BEGIN query=\"$query\"")
                Log.i(TAG, context.prompt)
                Log.i(TAG, "RAG_CONTEXT_END query=\"$query\"")
            }
        }
    }

    fun crawlKnowledge() {
        scheduleSourceCrawl()
    }

    class Factory(
        private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
        private val offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(scheduleSourceCrawl, offlineKnowledgeRetriever) as T
        }
    }

    companion object {
        private const val TAG = "JoharRAG"

        private val TEST_QUERIES = listOf(
            "Rugra kya hai?",
            "Jharkhand ka state animal?",
            "Deoghar me temple?",
            "Ranchi ke paas waterfall?",
        )
    }
}
