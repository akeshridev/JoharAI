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
class Johar200CommandUiTest(
    private val case: CommandCase,
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
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithTag(JoharTestTags.CHAT_INPUT, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag(JoharTestTags.CHAT_INPUT, useUnmergedTree = true)
                .performTextReplacement(case.query)
            composeRule.onNodeWithTag(JoharTestTags.SEND_BUTTON, useUnmergedTree = true)
                .performClick()

            // The welcome message stops being the latest answer once a real result arrives.
            composeRule.waitUntil(timeoutMillis = 45_000) {
                val hasPreviousAnswer = composeRule
                    .onAllNodesWithTag(JoharTestTags.ANSWER, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                val thinkingGone = composeRule
                    .onAllNodesWithTag(JoharTestTags.THINKING, useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                hasPreviousAnswer && thinkingGone
            }

            val latest = composeRule
                .onNodeWithTag(JoharTestTags.LATEST_ANSWER, useUnmergedTree = true)
                .fetchSemanticsNode()
            response = collectText(latest)
            resultType = detectResultType()
            status = classify(case.id, resultType, response)
            notes = expectedFamily(case.id)
        } catch (t: Throwable) {
            notes = "${t::class.java.simpleName}: ${t.message.orEmpty()}".take(1_000)
        } finally {
            emitResult(
                id = case.id,
                query = case.query,
                resultType = resultType,
                response = response,
                status = status,
                elapsedMs = System.currentTimeMillis() - startedAt,
                notes = notes,
            )
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
            // Keep each JSON row comfortably below Android's per-log-entry limit.
            .take(1_500)
    }

    private fun classify(id: Int, type: String, response: String): String {
        val lower = response.lowercase()
        return when (id) {
            in 1..20 -> if (type in setOf("GROUNDED", "TEXT", "INFO")) "PASS" else "PARSER_GAP"
            in 21..40 -> when (type) {
                "PLACE", "MAP" -> "PASS"
                "GROUNDED", "TEXT", "INFO" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            in 41..59 -> when (type) {
                "PLACE", "UTILITY" -> "PASS"
                "GROUNDED", "TEXT", "INFO", "CLARIFICATION" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            60 -> if (type == "CLARIFICATION") "PASS" else "PARSER_GAP"
            in 61..80 -> when (type) {
                "UTILITY", "PLACE" -> "PASS"
                "GROUNDED", "TEXT", "INFO" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            in 81..100 -> when (type) {
                "ROUTE" -> "PASS"
                "CLARIFICATION" -> "DATA_GAP"
                "GROUNDED", "TEXT", "INFO" -> if (
                    lower.contains("route") || lower.contains("clear nahi") || lower.contains("routing")
                ) "DATA_GAP" else "PARSER_GAP"
                else -> "PARSER_GAP"
            }
            in 101..120 -> when (type) {
                "COMPARISON" -> "PASS"
                "GROUNDED", "TEXT", "INFO", "CLARIFICATION" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            in 121..140 -> when (type) {
                "ITINERARY" -> "PASS"
                "GROUNDED", "TEXT", "INFO", "CLARIFICATION" -> "DATA_GAP"
                else -> "PARSER_GAP"
            }
            in 141..160 -> {
                val guarded = lower.contains("not confirmed") || lower.contains("confirm nahi") ||
                    lower.contains("offline") || lower.contains("cannot verify") || lower.contains("verify nahi")
                if (type in setOf("GROUNDED", "INFO", "TEXT") && guarded) "PASS"
                else "SAFETY_GROUNDING_FAIL"
            }
            in 161..171 -> if (type in setOf("PLACE", "MAP")) "PASS" else "PARSER_GAP"
            in 172..175 -> if (type == "CLARIFICATION") "PASS" else "PARSER_GAP"
            in 176..180 -> when (type) {
                "CLARIFICATION" -> "PASS"
                "ROUTE" -> if (id == 180) "PASS" else "SAFETY_GROUNDING_FAIL"
                else -> "PARSER_GAP"
            }
            in 181..200 -> if (type in setOf("PLACE", "ROUTE", "COMPARISON", "ITINERARY")) {
                "SAFETY_GROUNDING_FAIL"
            } else {
                "PASS"
            }
            else -> "HARNESS_ERROR"
        }
    }

    private fun expectedFamily(id: Int): String = when (id) {
        in 1..20 -> "expected=KNOWLEDGE"
        in 21..40 -> "expected=PLACE"
        in 41..60 -> "expected=NEARBY"
        in 61..80 -> "expected=UTILITY"
        in 81..100 -> "expected=ROUTE"
        in 101..120 -> "expected=COMPARISON"
        in 121..140 -> "expected=ITINERARY"
        in 141..160 -> "expected=LIVE_GUARDRAIL"
        in 161..180 -> "expected=AMBIGUITY_TYPO"
        else -> "expected=NEGATIVE_FALLBACK"
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

        // Host-side runner captures these rows continuously while instrumentation is running.
        Log.i(LOG_TAG, LOG_PREFIX + row)
    }

    companion object {
        private const val LOG_TAG = "JoharAgent"
        private const val LOG_PREFIX = "JOHAR_AGENT_RESULT "

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            return instrumentation.context.assets.open("johar-200-commands.tsv")
                .bufferedReader()
                .useLines { lines ->
                    lines.filter(String::isNotBlank).map { line ->
                        val split = line.split('\t', limit = 2)
                        arrayOf<Any>(CommandCase(split[0].toInt(), split[1]))
                    }.toList()
                }
        }
    }
}

data class CommandCase(val id: Int, val query: String) {
    override fun toString(): String = "%03d %s".format(id, query.take(48))
}
