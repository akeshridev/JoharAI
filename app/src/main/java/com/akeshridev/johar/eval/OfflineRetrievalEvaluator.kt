package com.akeshridev.johar.eval

import android.content.Context
import com.akeshridev.johar.data.retrieval.OfflineKnowledgeRetriever
import org.json.JSONObject

class OfflineRetrievalEvaluator(
    context: Context,
    private val retriever: OfflineKnowledgeRetriever,
) {
    private val appContext = context.applicationContext

    fun run(assetName: String = "eval.jsonl"): RetrievalEvalReport {
        val cases = loadCases(assetName)
        val results = cases.map { case -> evaluate(case) }
        return RetrievalEvalReport(
            total = results.size,
            recallAt1 = fraction(results.count(RetrievalEvalResult::hitAt1), results.size),
            recallAt3 = fraction(results.count(RetrievalEvalResult::hitAt3), results.size),
            results = results,
        )
    }

    private fun evaluate(case: RetrievalEvalCase): RetrievalEvalResult {
        val hits = retriever.retrieve(case.query, limit = 3)
        val names = hits.map { it.name }
        val expected = case.expected.map(::normalizeName).toSet()
        val normalizedHits = names.map(::normalizeName)
        val noAnswerCorrect = case.expectNoAnswer && names.isEmpty()
        return RetrievalEvalResult(
            case = case,
            hitAt1 = if (case.expectNoAnswer) noAnswerCorrect else normalizedHits.take(1).any(expected::contains),
            hitAt3 = if (case.expectNoAnswer) noAnswerCorrect else normalizedHits.take(3).any(expected::contains),
            actual = names,
        )
    }

    private fun loadCases(assetName: String): List<RetrievalEvalCase> =
        appContext.assets.open(assetName).bufferedReader().useLines { lines ->
            lines
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(::parseCase)
                .toList()
        }

    private fun parseCase(line: String): RetrievalEvalCase {
        val json = JSONObject(line)
        val expectedJson = json.optJSONArray("expected")
        val expected = buildList {
            if (expectedJson != null) {
                for (index in 0 until expectedJson.length()) {
                    add(expectedJson.getString(index))
                }
            }
        }
        return RetrievalEvalCase(
            query = json.getString("query"),
            expected = expected,
            category = json.optString("category").takeIf(String::isNotBlank),
            expectNoAnswer = json.optBoolean("expectNoAnswer", false),
        )
    }

    private fun normalizeName(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun fraction(count: Int, total: Int): Double =
        if (total == 0) 0.0 else count.toDouble() / total
}

data class RetrievalEvalCase(
    val query: String,
    val expected: List<String>,
    val category: String?,
    val expectNoAnswer: Boolean = false,
)

data class RetrievalEvalResult(
    val case: RetrievalEvalCase,
    val hitAt1: Boolean,
    val hitAt3: Boolean,
    val actual: List<String>,
)

data class RetrievalEvalReport(
    val total: Int,
    val recallAt1: Double,
    val recallAt3: Double,
    val results: List<RetrievalEvalResult>,
) {
    val failuresAt3: List<RetrievalEvalResult>
        get() = results.filterNot(RetrievalEvalResult::hitAt3)
}
