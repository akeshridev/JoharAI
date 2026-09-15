package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.retrieval.JoharAnswer
import com.akeshridev.johar.data.retrieval.JoharAnswerMode
import com.akeshridev.johar.data.routing.RanchiRouteResult
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrototypeRouterRegressionTest {

    @Test
    fun longFamilyRecommendationUsesPlaceDiscoveryInsteadOfGenericKnowledge() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val rockGarden = place("rock-garden", "Rock Garden", "PARK")
        var knowledgeCalls = 0
        val router = JoharQueryRouter(
            resolvePlace = { emptyList() },
            nearby = { _, _ -> emptyList() },
            discoverPlaces = { types ->
                if ("TOURIST_ATTRACTION" in types) listOf(tagoreHill, rockGarden) else emptyList()
            },
            knowledgeAnswer = {
                knowledgeCalls += 1
                knowledge(it)
            },
        )

        val result = router.answer("Ranchi mein peaceful family place suggest karo, parents ke saath jana hai")

        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(listOf(tagoreHill, rockGarden), result.places)
        assertEquals(0, knowledgeCalls)
    }

    @Test
    fun kidsParkRecommendationUsesKidsSpecificCaveat() {
        val rockGarden = place("rock-garden", "Rock Garden", "PARK")
        val router = JoharQueryRouter(
            resolvePlace = { emptyList() },
            nearby = { _, _ -> emptyList() },
            discoverPlaces = { types -> if ("PARK" in types) listOf(rockGarden) else emptyList() },
            knowledgeAnswer = { knowledge(it) },
        )

        val result = router.answer("Ranchi mein kids ke liye park suggest karo")

        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertTrue(result.intro.contains("Kids-friendly", ignoreCase = true))
        assertTrue(!result.intro.contains("Peaceful/family", ignoreCase = true))
    }

    @Test
    fun newExplicitCategoryDoesNotConsumeStaleNearbyClarification() {
        var lastKnowledgeQuery: String? = null
        val router = JoharQueryRouter(
            resolvePlace = { emptyList() },
            nearby = { _, _ -> emptyList() },
            knowledgeAnswer = {
                lastKnowledgeQuery = it
                knowledge(it)
            },
        )

        val clarification = router.answer("Main Road ke paas restaurant batao")
        assertTrue(clarification is JoharQueryResult.Clarification)

        val emergency = router.answer("Emergency hospital number")

        assertTrue(emergency is JoharQueryResult.Grounded)
        assertEquals("Emergency hospital number", lastKnowledgeQuery)
    }

    @Test
    fun namedPlaceInsideSuitabilityQuestionResolvesBeforeGenericFallback() {
        val kankeDam = place("kanke-dam", "Kanke Dam", "TOURIST_ATTRACTION")
        val router = JoharQueryRouter(
            resolvePlace = { query ->
                if (query.equals("kanke dam", ignoreCase = true)) listOf(kankeDam) else emptyList()
            },
            nearby = { _, _ -> emptyList() },
            knowledgeAnswer = { knowledge(it) },
        )

        val result = router.answer("Kanke Dam family ke liye acha hai?")

        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(listOf(kankeDam), result.places)
        assertTrue(result.intro.contains("suitability", ignoreCase = true))
    }

    @Test
    fun hinglishKaiseJaunParsesAsRoute() {
        val station = place("ranchi-station", "Ranchi Railway Station", "STATION")
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val route = RanchiRouteResult.Success(
            points = listOf(station.coordinate, tagoreHill.coordinate),
            distanceMeters = 4_500.0,
            durationSeconds = 720.0,
            snappedOrigin = station.coordinate,
            snappedDestination = tagoreHill.coordinate,
        )
        val router = JoharQueryRouter(
            resolvePlace = { query ->
                when (query.lowercase()) {
                    "ranchi railway station", "ranchi station" -> listOf(station)
                    "tagore hill" -> listOf(tagoreHill)
                    else -> emptyList()
                }
            },
            nearby = { _, _ -> emptyList() },
            knowledgeAnswer = { knowledge(it) },
            routeInstalled = { true },
            route = { _, _ -> route },
        )

        val result = router.answer("Ranchi railway station se Tagore Hill kaise jaun?")

        assertEquals(JoharQueryResult.Route(station, tagoreHill, route), result)
    }

    private fun knowledge(query: String) = JoharAnswer(
        text = "knowledge:$query",
        evidence = emptyList(),
        mode = JoharAnswerMode.DETERMINISTIC,
    )

    private fun place(
        id: String,
        name: String,
        type: String,
    ) = RanchiSpatialPlace(
        id = id,
        name = name,
        type = type,
        coordinate = RanchiCoordinate(23.3441, 85.3096),
        region = "Ranchi",
        description = null,
        distanceKm = null,
    )
}
