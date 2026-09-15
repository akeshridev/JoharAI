package com.akeshridev.johar.data.retrieval

import com.akeshridev.johar.data.local.KnowledgeDao
import com.akeshridev.johar.data.local.KnowledgeEntityRow
import com.akeshridev.johar.data.local.SourceFactRow
import com.akeshridev.johar.data.spatial.RanchiSpatialEngine
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class ShortQueryRegressionTest {
    private val food = entity("Golgappa", "FOOD", "[\"pani puri\",\"puchka\",\"phuchka\"]")
    private val school = entity("Vidya Mandir", "FACILITY", "[]", "SCHOOL")
    private val pahari = entity("Pahari Mandir", "TOURIST_ATTRACTION", "[\"Pahari Temple\"]", "TEMPLE")
    private val foodFact = SourceFactRow(
        "fact", food.id, "https://example.org/fixture/food", "Test fixture", 1,
        "FOOD", "description", "TEXT", "A street snack.", null, null, null,
        "A street snack.", "STATIC", "KNOWN", "STATIC_OR_SLOW",
    )
    private val schoolFact = SourceFactRow(
        "school-fact", school.id, "https://example.org/fixture/school", "Test fixture", 1,
        "EDUCATION", "address", "TEXT", "Ranchi", null, null, null,
        "School address in Ranchi.", "STATIC", "KNOWN", "STATIC_OR_SLOW",
    )
    private val stairsFact = SourceFactRow(
        "stairs-fact", pahari.id, "https://example.org/fixture/temple", "Test fixture", 1,
        "ACCESS", "steps", "TEXT", "468 steps", null, null, null,
        "Visitors climb 468 steps.", "STATIC", "KNOWN", "STATIC_OR_SLOW",
    )

    @Test fun storedFoodAliasesReachScoringAndPreserveEvidence() {
        val retriever = OfflineKnowledgeRetriever(dao(listOf(food, school)))
        for (query in listOf("pani puri", "puchka", "phuchka", "golgappa", "PANI-PURI", "puchka kya hai")) {
            val hit = retriever.retrieve(query).first()
            assertEquals(query, food.id, hit.entityId)
            assertEquals(foodFact.sourceUrl, hit.facts.single().sourceUrl)
        }
    }

    @Test fun unsupportedQueriesAndPartialAliasesRemainGated() {
        val retriever = OfflineKnowledgeRetriever(dao(listOf(food)))
        for (query in listOf("write python code", "pani", "puchkax", "quantum computing")) {
            assertTrue(query, retriever.retrieve(query).isEmpty())
        }
        assertTrue(OfflineKnowledgeRetriever(dao(emptyList())).retrieve("puchka").isEmpty())
    }

    @Test fun schoolMetadataSupportsRetrievalAndSpatialDiscovery() {
        val dao = dao(listOf(school, food, school.copy(id = "outside", region = "Delhi")))
        assertEquals("SCHOOL", OfflineKnowledgeRetriever(dao).retrieve("school").first().packType)
        val places = RanchiSpatialEngine(dao).discoverPlaces(setOf("SCHOOL"))
        assertEquals(listOf(school.id), places.map { it.id })
        assertEquals("SCHOOL", places.single().type)
    }

    @Test fun broadSchoolQueryListsSupportedSchoolEvidence() {
        val answer = DeterministicJoharAnswerGenerator(OfflineKnowledgeRetriever(dao(listOf(school)))).answer("school")
        assertEquals(JoharAnswerMode.DETERMINISTIC, answer.mode)
        assertTrue(answer.text.contains("Vidya Mandir"))
        assertEquals(school.id, answer.evidence.single().entityId)
        assertEquals(schoolFact.sourceUrl, answer.evidence.single().facts.single().sourceUrl)
    }

    @Test fun namedTempleDetailUsesMatchingFactInsteadOfCategoryMix() {
        val answer = DeterministicJoharAnswerGenerator(OfflineKnowledgeRetriever(dao(listOf(pahari)))).answer(
            "Pahari Mandir mein stairs hain?",
        )
        assertEquals(JoharAnswerMode.DETERMINISTIC, answer.mode)
        assertTrue(answer.text.contains("468 steps"))
        assertEquals(listOf(pahari.id), answer.evidence.map { it.entityId })
    }

    @Test fun missingRequestedAttributeDoesNotSubstituteAnotherFact() {
        val answer = DeterministicJoharAnswerGenerator(OfflineKnowledgeRetriever(dao(listOf(pahari)))).answer(
            "Pahari Mandir wheelchair accessible hai?",
        )
        assertEquals(JoharAnswerMode.NO_ANSWER, answer.mode)
        assertTrue(answer.text.contains("source-backed information"))
        assertEquals(listOf(pahari.id), answer.evidence.map { it.entityId })
    }

    @Test fun optionalTraceDoesNotChangeResultsAndReportsGateAndAliases() {
        val dao = dao(listOf(food, school))
        var trace: OfflineRetrievalTrace? = null
        val observed = OfflineKnowledgeRetriever(dao) { trace = it }
        val ordinary = OfflineKnowledgeRetriever(dao)
        assertEquals(ordinary.retrieve("PANI-PURI", 1), observed.retrieve("PANI-PURI", 1))
        assertEquals("pani puri", trace!!.normalizedQuery)
        assertEquals(listOf("pani puri"), trace!!.matchedAliases[food.id])
        assertTrue(observed.retrieve("write python code").isEmpty())
        assertEquals("NO_SUPPORTED_INTENT_OR_CORPUS_IDENTITY", trace!!.fallbackReason)
        assertTrue(trace!!.candidates.isEmpty())
    }

    private fun dao(entities: List<KnowledgeEntityRow>): KnowledgeDao = Proxy.newProxyInstance(
        KnowledgeDao::class.java.classLoader, arrayOf(KnowledgeDao::class.java),
    ) { _, method, args ->
        when (method.name) {
            "allEnabledEntities" -> entities
            "factsForEntity" -> when (args!![0]) {
                food.id -> listOf(foodFact)
                school.id -> listOf(schoolFact)
                pahari.id -> listOf(stairsFact)
                else -> emptyList<SourceFactRow>()
            }
            else -> error("Unexpected DAO call: ${method.name}")
        }
    } as KnowledgeDao

    private fun entity(name: String, type: String, aliases: String, packType: String = type) = KnowledgeEntityRow(
        name, name, name.lowercase(), type, null, 23.34, 85.31, "Ranchi", "India", aliases,
        "{\"joharPackType\":\"$packType\"}", 1, 1, null, 0, true,
    )
}
