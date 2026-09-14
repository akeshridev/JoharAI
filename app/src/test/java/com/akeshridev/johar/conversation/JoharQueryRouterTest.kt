package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.routing.RanchiRouteResult
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoharQueryRouterTest {

    @Test
    fun placeLookupReturnsStrongSpatialMatch() {
        val tagoreHill = place(
            id = "tagore-hill",
            name = "Tagore Hill",
            type = "TOURIST_ATTRACTION",
        )
        val router = router(
            resolve = { query -> if (query == "tagore hill") listOf(tagoreHill) else emptyList() },
        )

        val result = router.answer("Tagore Hill kahan hai?")

        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(listOf(tagoreHill), result.places)
    }

    @Test
    fun knowledgeQuestionStaysTextFirst() {
        val router = router()

        val result = router.answer("Rugra kya hai?")

        assertEquals(
            JoharQueryResult.Text("knowledge:Rugra kya hai?"),
            result,
        )
    }

    @Test
    fun nearbyWithoutOriginAsksThenUsesExplicitLocality() {
        val lalpur = place(
            id = "lalpur",
            name = "Lalpur",
            type = "PLACE",
        )
        val temple = place(
            id = "temple-1",
            name = "Hanuman Mandir",
            type = "TEMPLE",
            distanceKm = 1.2,
        )
        val router = router(
            resolve = { query -> if (query.equals("Lalpur", ignoreCase = true)) listOf(lalpur) else emptyList() },
            nearby = { _, _ -> listOf(temple) },
        )

        assertEquals(
            JoharQueryResult.Text("Kis locality ya landmark ke paas? Jaise Lalpur ya Ranchi railway station."),
            router.answer("mere aas paas mandir?"),
        )

        val result = router.answer("Lalpur")
        assertTrue(result is JoharQueryResult.Places)
        result as JoharQueryResult.Places
        assertEquals(listOf(temple), result.places)
        assertTrue(result.intro.contains("Lalpur"))
        assertTrue(result.intro.contains("seedhi rekha"))
    }

    @Test
    fun weakSpatialCandidateFallsBackToKnowledge() {
        val tagoreHill = place(
            id = "tagore-hill",
            name = "Tagore Hill",
            type = "TOURIST_ATTRACTION",
        )
        val router = router(resolve = { listOf(tagoreHill) })

        val result = router.answer("Rock Garden")

        assertEquals(JoharQueryResult.Text("knowledge:Rock Garden"), result)
    }

    @Test
    fun liveStatusQuestionDoesNotPretendOfflineDataIsCurrent() {
        val router = router()

        val result = router.answer("Pahari Mandir open now?")

        assertTrue(result is JoharQueryResult.Text)
        result as JoharQueryResult.Text
        assertTrue(result.text.startsWith("Abhi ki timing, status ya availability confirm nahi hai."))
        assertTrue(result.text.contains("knowledge:Pahari Mandir open now?"))
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

        assertTrue(result is JoharQueryResult.Text)
        result as JoharQueryResult.Text
        assertTrue(result.text.contains("routing pack"))
        assertTrue(result.text.contains("road route"))
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
        knowledgeAnswer = { "knowledge:$it" },
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
