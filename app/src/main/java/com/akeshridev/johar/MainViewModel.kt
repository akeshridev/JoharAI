package com.akeshridev.johar

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akeshridev.johar.data.retrieval.DeterministicJoharAnswerGenerator
import com.akeshridev.johar.data.retrieval.LiteRtLmAnswerSynthesizer
import com.akeshridev.johar.data.retrieval.LocalModelStore
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import com.akeshridev.johar.data.retrieval.OfflineRagContextBuilder
import com.akeshridev.johar.data.spatial.RanchiSpatialEngine
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import com.akeshridev.johar.eval.OfflineRetrievalEvaluator
import com.akeshridev.johar.eval.RanchiCoverageEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
    private val offlineKnowledgeRetriever: OfflineKnowledgeRetriever,
    private val ranchiSpatialEngine: RanchiSpatialEngine,
    private val offlineRetrievalEvaluator: OfflineRetrievalEvaluator,
    private val ranchiCoverageEvaluator: RanchiCoverageEvaluator,
    private val localModelStore: LocalModelStore,
) : ViewModel() {

    private val ragContextBuilder = OfflineRagContextBuilder(offlineKnowledgeRetriever)
    private val synthesizer = LiteRtLmAnswerSynthesizer(localModelStore.modelFile.absolutePath)
    private val answerGenerator = DeterministicJoharAnswerGenerator(offlineKnowledgeRetriever)

    override fun onCleared() {
        synthesizer.close()
        super.onCleared()
    }

    fun testOfflineRetrieval() {
        viewModelScope.launch(Dispatchers.IO) {
            TEST_QUERIES.forEach { query ->
                val context = ragContextBuilder.build(query)
                val hits = context.hits
                Log.i(TAG, "query=\"$query\" hits=${hits.size}")
                hits.forEachIndexed { index, hit ->
                    val factSummary = hit.facts.take(3).joinToString(" | ") { "${it.field}=${it.value}" }
                    Log.i(
                        TAG,
                        "  #${index + 1} score=${hit.score} name=${hit.name} type=${hit.type} " +
                            "packType=${hit.packType.orEmpty()} description=${hit.description.orEmpty()} facts=[$factSummary]",
                    )
                }
                Log.i(TAG, "RAG_CONTEXT_BEGIN query=\"$query\"")
                Log.i(TAG, context.prompt)
                Log.i(TAG, "RAG_CONTEXT_END query=\"$query\"")
            }
        }
    }

    fun testRanchiSpatial() {
        viewModelScope.launch(Dispatchers.IO) {
            SPATIAL_TEST_QUERIES.forEach { query ->
                val places = ranchiSpatialEngine.resolvePlace(query)
                Log.i(SPATIAL_TAG, "query=\"$query\" matches=${places.size}")
                places.forEachIndexed { index, place ->
                    Log.i(
                        SPATIAL_TAG,
                        "  #${index + 1} name=${place.name} type=${place.type} " +
                            "lat=${place.coordinate.latitude} lon=${place.coordinate.longitude} region=${place.region.orEmpty()}",
                    )
                }
            }
        }
    }

    fun testDeterministicAnswers() {
        viewModelScope.launch(Dispatchers.IO) {
            ANSWER_TEST_QUERIES.forEach { query ->
                val answer = answerGenerator.answer(query)
                Log.i(
                    ANSWER_TAG,
                    "query=\"$query\" mode=${answer.mode} answer=\"${answer.text}\" evidence=${answer.evidence.map { it.name }}",
                )
            }
        }
    }

    fun testOnDeviceLlm(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val context = ragContextBuilder.build(query)
            Log.i(LLM_TAG, "QUERY query=\"$query\" hits=${context.hits.size}")
            context.hits.forEachIndexed { index, hit ->
                val factSummary = hit.facts.joinToString(" | ") { "${it.field}=${it.value}" }
                Log.i(
                    LLM_TAG,
                    "RETRIEVAL #${index + 1} score=${hit.score} name=${hit.name} " +
                        "type=${hit.type} packType=${hit.packType.orEmpty()} facts=[$factSummary]",
                )
            }

            val startedAt = SystemClock.elapsedRealtime()
            try {
                val answer = synthesizer.synthesize(context)
                val latencyMs = SystemClock.elapsedRealtime() - startedAt
                Log.i(LLM_TAG, "SUCCESS latencyMs=$latencyMs query=\"$query\" answer=\"$answer\"")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (unavailable: LinkageError) {
                Log.e(LLM_TAG, "FAILED kind=RUNTIME_UNAVAILABLE reason=${unavailable.message}", unavailable)
            } catch (throwable: Exception) {
                val latencyMs = SystemClock.elapsedRealtime() - startedAt
                Log.e(
                    LLM_TAG,
                    "FAILED latencyMs=$latencyMs path=${localModelStore.modelFile.absolutePath} error=${throwable::class.java.simpleName}: ${throwable.message}",
                    throwable,
                )
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

    fun runRanchiCoverageEval() {
        viewModelScope.launch(Dispatchers.IO) {
            val report = ranchiCoverageEvaluator.run()
            Log.i(
                RANCHI_EVAL_TAG,
                "SUMMARY total=${report.total} pass=${report.pass} partial=${report.partial} " +
                    "gap=${report.gap} gapCounts=${report.gapCounts}",
            )
            Log.i(RANCHI_EVAL_TAG, "CATEGORY_COUNTS ${report.categoryCounts}")
            Log.i(RANCHI_EVAL_TAG, "PERSONA_COUNTS ${report.personaCounts}")
            report.results.forEach { result ->
                Log.i(
                    RANCHI_EVAL_TAG,
                    "${result.case.id} status=${result.status} persona=${result.case.persona} " +
                        "category=${result.case.category} query=\"${result.case.query}\" " +
                        "effectiveQuery=\"${result.effectiveQuery}\" gaps=${result.gaps} " +
                        "actual=${result.actual} packTypes=${result.actualPackTypes}",
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
        private val ranchiSpatialEngine: RanchiSpatialEngine,
        private val offlineRetrievalEvaluator: OfflineRetrievalEvaluator,
        private val ranchiCoverageEvaluator: RanchiCoverageEvaluator,
        private val localModelStore: LocalModelStore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                scheduleSourceCrawl = scheduleSourceCrawl,
                offlineKnowledgeRetriever = offlineKnowledgeRetriever,
                ranchiSpatialEngine = ranchiSpatialEngine,
                offlineRetrievalEvaluator = offlineRetrievalEvaluator,
                ranchiCoverageEvaluator = ranchiCoverageEvaluator,
                localModelStore = localModelStore,
            ) as T
        }
    }

    companion object {
        private const val TAG = "JoharRAG"
        private const val EVAL_TAG = "JoharEval"
        private const val RANCHI_EVAL_TAG = "JoharRanchiEval"
        private const val SPATIAL_TAG = "JoharSpatial"
        private const val ANSWER_TAG = "JoharAnswer"
        private const val LLM_TAG = "JoharLLM"

        private fun formatPercent(value: Double): String = "%.1f%%".format(value * 100.0)

        private val TEST_QUERIES = listOf(
            "Ranchi ke bazar",
            "Ranchi me mutton kahan milega?",
            "Lalpur ke paas mandir",
            "parents ke saath Ranchi me kam walking wali jagah",
            "Rugra kahan milega Ranchi me?",
            "Ranchi ke paas waterfall?",
            "Ranchi me emergency hospital kahan hai?",
            "Ranchi me major colleges kaun se hain?",
            "Ranchi ke major business areas kaun se hain?",
        )

        private val SPATIAL_TEST_QUERIES = listOf(
            "Tagore Hill",
            "Ranchi railway station",
            "Pahari Mandir",
            "Lalpur",
            "Bariatu",
        )

        private val ANSWER_TEST_QUERIES = listOf(
            "Rugra kya hai?",
            "Deori Mandir kahan hai?",
            "Ranchi ke paas waterfall?",
            "Lalpur ke paas mandir",
            "What is the capital of Australia?",
        )
    }
}
