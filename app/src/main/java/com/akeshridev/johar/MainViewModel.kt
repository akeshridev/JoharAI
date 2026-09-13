package com.akeshridev.johar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import com.akeshridev.johar.data.retrieval.OfflineRagContextBuilder
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import com.akeshridev.johar.eval.OfflineRetrievalEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
    private val offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
    private val offlineRetrievalEvaluator: OfflineRetrievalEvaluator,
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

    fun runOfflineRetrievalEval() {
        viewModelScope.launch(Dispatchers.IO) {
            val report = offlineRetrievalEvaluator.run()
            Log.i(
                EVAL_TAG,
                "SUMMARY total=${report.total} recall@1=${formatPercent(report.recallAt1)} " +
                    "recall@3=${formatPercent(report.recallAt3)} failures@3=${report.failuresAt3.size}",
            )
            report.results.forEachIndexed { index, result ->
                val status = when {
                    result.hitAt1 -> "PASS@1"
                    result.hitAt3 -> "PASS@3"
                    else -> "FAIL"
                }
                Log.i(
                    EVAL_TAG,
                    "#${index + 1} $status category=${result.case.category.orEmpty()} " +
                        "query=\"${result.case.query}\" expected=${result.case.expected} actual=${result.actual}",
                )
            }
        }
    }

    fun crawlKnowledge() {
        scheduleSourceCrawl()
    }

    class Factory(
        private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
        private val offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
        private val offlineRetrievalEvaluator: OfflineRetrievalEvaluator,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                scheduleSourceCrawl = scheduleSourceCrawl,
                offlineKnowledgeRetriever = offlineKnowledgeRetriever,
                offlineRetrievalEvaluator = offlineRetrievalEvaluator,
            ) as T
        }
    }

    companion object {
        private const val TAG = "JoharRAG"
        private const val EVAL_TAG = "JoharEval"

        private fun formatPercent(value: Double): String = "%.1f%%".format(value * 100.0)

        private val TEST_QUERIES = listOf(
            "Rugra kya hai?",
            "Jharkhand ka state animal?",
            "Deoghar me temple?",
            "Ranchi ke paas waterfall?",
        )
    }
}
