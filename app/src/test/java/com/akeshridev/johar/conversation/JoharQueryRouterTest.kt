package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.retrieval.JoharAnswer
import com.akeshridev.johar.data.retrieval.JoharAnswerMode
import com.akeshridev.johar.data.routing.RanchiRouteResult
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoharQueryRouterTest {

    @Test
    fun placeLookupReturnsStrongSpatialMatch() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val router = router(resolve = { query -> if (query == "tagore hill") listOf(tagoreHill) else emptyList() })

        val result = router.answer("Tagore Hill kahan hai?")

        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(listOf(tagoreHill), result.places)
    }

    @Test
    fun knowledgeQuestionPreservesGroundedAnswer() {
        val router = router()

        val result = router.answer("Rugra kya hai?")

        assertTrue(result is JoharQueryResult.Grounded)
        result as JoharQueryResult.Grounded
        assertEquals("knowledge:Rugra kya hai?", result.answer.text)
        assertEquals(GroundedTone.NORMAL, result.tone)
    }

    @Test
    fun nearbyWithoutOriginAsksThenUsesExplicitLocality() {
        val lalpur = place("lalpur", "Lalpur", "PLACE")
        val temple = place("temple-1", "Hanuman Mandir", "TEMPLE", distanceKm = 1.2)
        val router = router(
            resolve = { query -> if (query.equals("Lalpur", ignoreCase = true)) listOf(lalpur) else emptyList() },
            nearby = { _, _ -> listOf(temple) },
        )

        val clarification = router.answer("mere aas paas mandir?")
        assertTrue(clarification is JoharQueryResult.Clarification)

        val result = router.answer("Lalpur")
        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(listOf(temple), result.places)
        assertTrue(result.intro.contains("Lalpur"))
        assertTrue(result.intro.contains("seedhi rekha"))
    }

    @Test
    fun utilityNearbyResultsAreMarkedUtility() {
        val lalpur = place("lalpur", "Lalpur", "PLACE")
        val hospital = place("hospital-1", "Sadar Hospital", "HOSPITAL", distanceKm = 2.0)
        val router = router(
            resolve = { query -> if (query.equals("Lalpur", true)) listOf(lalpur) else emptyList() },
            nearby = { _, _ -> listOf(hospital) },
        )

        val result = router.answer("Lalpur ke paas hospital")

        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(PlaceResultKind.UTILITY, result.kind)
        assertEquals(hospital, result.places.single())
    }

    @Test
    fun weakSpatialCandidateFallsBackToKnowledge() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val router = router(resolve = { listOf(tagoreHill) })

        val result = router.answer("Rock Garden")

        assertTrue(result is JoharQueryResult.Grounded)
        result as JoharQueryResult.Grounded
        assertEquals("knowledge:Rock Garden", result.answer.text)
    }

    @Test
    fun liveStatusQuestionIsExplicitlyNotConfirmed() {
        val router = router()

        val result = router.answer("Pahari Mandir open now?")

        assertTrue(result is JoharQueryResult.Grounded)
        result as JoharQueryResult.Grounded
        assertEquals(GroundedTone.NOT_CONFIRMED, result.tone)
        assertTrue(result.prefix.orEmpty().contains("confirm nahi"))
    }

    @Test
    fun installedRoutingPackProducesRealRouteResult() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val station = place("ranchi-station", "Ranchi Railway Station", "STATION")
        val computed = RanchiRouteResult.Success(
            points = listOf(tagoreHill.coordinate, station.coordinate),
            distanceMeters = 6_400.0,
            durationSeconds = 960.0,
            snappedOrigin = tagoreHill.coordinate,
            snappedDestination = station.coordinate,
        )
        val router = router(
            resolve = { query ->
                when (query.lowercase()) {
                    "tagore hill" -> listOf(tagoreHill)
                    "ranchi station" -> listOf(station)
                    else -> emptyList()
                }
            },
            routeInstalled = { true },
            route = { _, _ -> computed },
        )

        val result = router.answer("route from Tagore Hill to Ranchi station")

        assertEquals(JoharQueryResult.Route(tagoreHill, station, computed), result)
    }

    @Test
    fun missingRoutingPackNeverInventsRoadRoute() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val station = place("ranchi-station", "Ranchi Railway Station", "STATION")
        val router = router(
            resolve = { query ->
                when (query.lowercase()) {
                    "tagore hill" -> listOf(tagoreHill)
                    "ranchi station" -> listOf(station)
                    else -> emptyList()
                }
            },
            routeInstalled = { false },
        )

        val result = router.answer("Tagore Hill se Ranchi station kaise jaye?")

        assertTrue(result is JoharQueryResult.Grounded)
        result as JoharQueryResult.Grounded
        assertEquals(GroundedTone.OFFLINE, result.tone)
        assertTrue(result.prefix.orEmpty().contains("routing pack"))
    }

    @Test
    fun comparisonRequiresTwoRealResolvedPlaces() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val rockGarden = place("rock-garden", "Rock Garden", "PARK")
        val router = router(resolve = { query ->
            when (query.lowercase()) {
                "tagore hill" -> listOf(tagoreHill)
                "rock garden" -> listOf(rockGarden)
                else -> emptyList()
            }
        })

        val result = router.answer("Tagore Hill vs Rock Garden")

        assertEquals(JoharQueryResult.Comparison(tagoreHill, rockGarden), result)
    }

    @Test
    fun explicitItineraryUsesOnlyResolvedStopsInRequestedOrder() {
        val tagoreHill = place("tagore-hill", "Tagore Hill", "TOURIST_ATTRACTION")
        val rockGarden = place("rock-garden", "Rock Garden", "PARK")
        val router = router(resolve = { query ->
            when (query.lowercase()) {
                "tagore hill" -> listOf(tagoreHill)
                "rock garden" -> listOf(rockGarden)
                else -> emptyList()
            }
        })

        val result = router.answer("plan: Tagore Hill, Rock Garden")

        assertTrue(result is JoharQueryResult.Itinerary)
        result as JoharQueryResult.Itinerary
        assertEquals(listOf(tagoreHill, rockGarden), result.places)
    }

    private fun router(
        resolve: (String) -> List<RanchiSpatialPlace> = { emptyList() },
        nearby: (RanchiCoordinate, Set<String>) -> List<RanchiSpatialPlace> = { _, _ -> emptyList() },
        routeInstalled: () -> Boolean = { false },
        route: (RanchiCoordinate, RanchiCoordinate) -> RanchiRouteResult = { _, _ ->
            RanchiRouteResult.Unavailable("not installed")
        },
    ) = JoharQueryRouter(
        resolvePlace = resolve,
        nearby = nearby,
        knowledgeAnswer = {
            JoharAnswer(
                text = "knowledge:$it",
                evidence = emptyList(),
                mode = JoharAnswerMode.DETERMINISTIC,
            )
        },
        routeInstalled = routeInstalled,
        route = route,
    )

    private fun place(
        id: String,
        name: String,
        type: String,
        distanceKm: Double? = null,
    ) = RanchiSpatialPlace(
        id = id,
        name = name,
        type = type,
        coordinate = RanchiCoordinate(23.3441, 85.3096),
        region = "Ranchi",
        description = null,
        distanceKm = distanceKm,
    )
}
