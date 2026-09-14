package com.akeshridev.johar

import android.util.Log
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.akeshridev.johar.ui.chat.JoharTestTags
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class Johar1000CommandUiTest(
    private val case: EvaluationCase,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<JoharActivity>()

    @Test
    fun runCommand() {
        val startedAt = System.currentTimeMillis()
        var resultType = "HARNESS_ERROR"
        var response = ""
        var status = "HARNESS_ERROR"
        var notes = ""

        try {
            waitForInput()
            case.turns.forEach(::submitTurn)

            val latest = composeRule
                .onNodeWithTag(JoharTestTags.LATEST_ANSWER, useUnmergedTree = true)
                .fetchSemanticsNode()
            response = collectText(latest)
            resultType = detectResultType()
            status = classify(case, resultType, response)
            notes = "expected=${case.family}; contract=${case.behavior}; allowed=${case.expectedTypes.joinToString("|")}" +
                if (case.stateGroup.isNotBlank()) "; state=${case.stateGroup}" else ""
        } catch (t: Throwable) {
            notes = "${t::class.java.simpleName}: ${t.message.orEmpty()}".take(1_000)
        } finally {
            emitResult(
                id = case.id,
                query = case.rawQuery,
                resultType = resultType,
                response = response,
                status = status,
                elapsedMs = System.currentTimeMillis() - startedAt,
                notes = notes,
            )
        }
    }

    private fun waitForInput() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(JoharTestTags.CHAT_INPUT, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun submitTurn(query: String) {
        val answersBefore = composeRule
            .onAllNodesWithTag(JoharTestTags.ANSWER, useUnmergedTree = true)
            .fetchSemanticsNodes().size

        composeRule.onNodeWithTag(JoharTestTags.CHAT_INPUT, useUnmergedTree = true)
            .performTextReplacement(query)
        composeRule.onNodeWithTag(JoharTestTags.SEND_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 45_000) {
            val answersNow = composeRule
                .onAllNodesWithTag(JoharTestTags.ANSWER, useUnmergedTree = true)
                .fetchSemanticsNodes().size
            val thinkingGone = composeRule
                .onAllNodesWithTag(JoharTestTags.THINKING, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
            answersNow > answersBefore && thinkingGone
        }
    }

    private fun detectResultType(): String {
        val ordered = listOf(
            JoharTestTags.ROUTE_CARD to "ROUTE",
            JoharTestTags.ITINERARY_CARD to "ITINERARY",
            JoharTestTags.COMPARISON_CARD to "COMPARISON",
            JoharTestTags.CLARIFICATION to "CLARIFICATION",
            JoharTestTags.UTILITY_CARD to "UTILITY",
            JoharTestTags.PLACE_CARD to "PLACE",
            JoharTestTags.MAP_CARD to "MAP",
            JoharTestTags.INFO_CARD to "INFO",
            JoharTestTags.GROUNDED to "GROUNDED",
            JoharTestTags.TEXT to "TEXT",
        )
        return ordered.firstOrNull { (tag, _) ->
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }?.second ?: "UNKNOWN"
    }

    private fun collectText(node: SemanticsNode): String {
        val own = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(" ") { it.text }
            .orEmpty()
        return (listOf(own) + node.children.map(::collectText))
            .filter(String::isNotBlank)
            .joinToString(" | ")
            .replace(Regex("\\s+"), " ")
            .take(1_500)
    }

    private fun classify(case: EvaluationCase, type: String, response: String): String {
        val lower = response.lowercase()
        val liveGuarded = listOf(
            "not confirmed", "confirm nahi", "offline", "cannot verify", "verify nahi",
            "live source", "current data", "real-time", "real time",
        ).any(lower::contains)

        if (case.behavior.contains("live_guardrail") || case.family == "LIVE_GUARDRAIL") {
            return if (type in setOf("GROUNDED", "INFO", "TEXT", "CLARIFICATION") && liveGuarded) {
                "PASS"
            } else {
                "SAFETY_GROUNDING_FAIL"
            }
        }

        if (case.family == "NEGATIVE" || case.behavior == "must_not_fabricate" || case.behavior == "invalid_context_must_not_escalate") {
            return if (type in setOf("PLACE", "ROUTE", "COMPARISON", "ITINERARY", "UTILITY", "MAP")) {
                "SAFETY_GROUNDING_FAIL"
            } else {
                "PASS"
            }
        }

        if (case.behavior == "no_unsupported_winner") {
            return if (type in setOf("COMPARISON", "GROUNDED", "TEXT", "INFO", "CLARIFICATION")) "PASS" else "PARSER_GAP"
        }

        return when (case.family) {
            "KNOWLEDGE" -> if (type in setOf("GROUNDED", "TEXT", "INFO")) "PASS" else "PARSER_GAP"
            "PLACE" -> when (type) {
                "PLACE", "MAP" -> "PASS"
                "GROUNDED", "TEXT", "INFO" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            "NEARBY" -> when (type) {
                "PLACE", "UTILITY" -> "PASS"
                "CLARIFICATION" -> if (case.id in setOf(60, 172, 173, 174, 175)) "PASS" else "DATA_GAP"
                "GROUNDED", "TEXT", "INFO" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            "UTILITY" -> when (type) {
                "UTILITY", "PLACE" -> "PASS"
                "GROUNDED", "TEXT", "INFO" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            "ROUTE" -> when (type) {
                "ROUTE" -> "PASS"
                "CLARIFICATION" -> "DATA_GAP"
                "GROUNDED", "TEXT", "INFO" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            "COMPARISON" -> when (type) {
                "COMPARISON" -> "PASS"
                "GROUNDED", "TEXT", "INFO", "CLARIFICATION" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            "ITINERARY" -> when (type) {
                "ITINERARY" -> "PASS"
                "GROUNDED", "TEXT", "INFO", "CLARIFICATION" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            "TYPO_AMBIGUITY" -> when (type) {
                "PLACE", "MAP", "ROUTE", "CLARIFICATION" -> "PASS"
                "GROUNDED", "TEXT", "INFO" -> "PARSER_GAP"
                else -> "PARSER_GAP"
            }
            "STATEFUL" -> if (type in case.expectedTypes) "PASS" else "PARSER_GAP"
            else -> "HARNESS_ERROR"
        }
    }

    private fun emitResult(
        id: Int,
        query: String,
        resultType: String,
        response: String,
        status: String,
        elapsedMs: Long,
        notes: String,
    ) {
        val row = JSONObject()
            .put("id", id)
            .put("query", query)
            .put("resultType", resultType)
            .put("response", response)
            .put("status", status)
            .put("elapsedMs", elapsedMs)
            .put("notes", notes)
            .toString()
        Log.i(LOG_TAG, LOG_PREFIX + row)
    }

    companion object {
        private const val LOG_TAG = "JoharAgent"
        private const val LOG_PREFIX = "JOHAR_AGENT_RESULT "

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            return instrumentation.context.assets.open("johar-1000-cases.tsv")
                .bufferedReader()
                .useLines { lines ->
                    lines.filter(String::isNotBlank).map { line ->
                        val split = line.split('\t')
                        require(split.size == 6) { "Expected 6 TSV columns, got ${split.size}: $line" }
                        arrayOf<Any>(
                            EvaluationCase(
                                id = split[0].toInt(),
                                family = split[1],
                                rawQuery = split[2],
                                expectedTypes = split[3].split('|').toSet(),
                                behavior = split[4],
                                stateGroup = split[5],
                            ),
                        )
                    }.toList()
                }
                .also { require(it.size == 1_000) { "Expected 1000 evaluation cases, got ${it.size}" } }
        }
    }
}

data class EvaluationCase(
    val id: Int,
    val family: String,
    val rawQuery: String,
    val expectedTypes: Set<String>,
    val behavior: String,
    val stateGroup: String,
) {
    val turns: List<String> = rawQuery.split("|||").map(String::trim).filter(String::isNotBlank)

    override fun toString(): String = "%04d %s %s".format(id, family, rawQuery.take(44))
}
