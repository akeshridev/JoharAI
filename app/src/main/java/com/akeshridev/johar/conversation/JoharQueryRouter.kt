package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.retrieval.JoharAnswer
import com.akeshridev.johar.data.retrieval.JoharAnswerMode
import com.akeshridev.johar.data.routing.RanchiRouteResult
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import java.util.Locale

/** Small synchronous router. The owner runs queries serially off the UI thread. */
class JoharQueryRouter(
    private val resolvePlace: (String) -> List<RanchiSpatialPlace>,
    private val nearby: (RanchiCoordinate, Set<String>) -> List<RanchiSpatialPlace>,
    private val knowledgeAnswer: (String) -> JoharAnswer,
    private val routeInstalled: () -> Boolean = { false },
    private val route: (RanchiCoordinate, RanchiCoordinate) -> RanchiRouteResult = { _, _ ->
        RanchiRouteResult.Unavailable("Offline routing pack is not installed.")
    },
) {
    private var pendingCategory: Category? = null

    /** Clears only ephemeral conversational context; repositories and routing stay warm. */
    fun resetConversationState() {
        pendingCategory = null
    }

    fun answer(query: String): JoharQueryResult {
        val words = words(query)

        // Safety first: current/live wording must never fall through to an offline route/place answer.
        if (words.any { it in LIVE_WORDS }) {
            pendingCategory = null
            return JoharQueryResult.Grounded(
                answer = knowledgeAnswer(query),
                tone = GroundedTone.NOT_CONFIRMED,
                prefix = "Abhi ki timing, status ya availability confirm nahi hai.",
            )
        }

        parseRouteRequest(query)?.let { request ->
            pendingCategory = null
            return route(request)
        }

        parseComparisonRequest(query)?.let { request ->
            pendingCategory = null
            compare(request)?.let { return it }
        }

        parseExplicitItinerary(query)?.let { requestedStops ->
            pendingCategory = null
            itinerary(requestedStops)?.let { return it }
        }

        // Explanatory questions stay text-first and retain their source evidence.
        if (words.any { it in KNOWLEDGE_WORDS }) {
            pendingCategory = null
            return knowledge(query)
        }

        val category = Category.entries.firstOrNull { c -> words.any { it in c.words } }
        val isNearby = words.any { it in NEAR_WORDS }
        if (isNearby && category != null) {
            val originWords = words.filterNot { it in category.words || it in NEAR_FILLER || it in LOOKUP_FILLER }
            pendingCategory = category
            if (originWords.isEmpty()) return askOrigin(category)
            return nearbyFrom(originWords.joinToString(" "), category)
        }

        val pending = pendingCategory
        pendingCategory = null
        if (pending != null && words.size <= 5 && words.none { it in QUESTION_WORDS }) {
            return nearbyFrom(query, pending)
        }

        // Unsupported near/route requests stay with grounded knowledge, never become a guessed place.
        if (isNearby || words.any { it in ROUTE_WORDS }) return knowledge(query)

        val nameWords = words.filterNot { it in LOOKUP_FILLER }
        if (nameWords.isEmpty() || words.size > 10) return knowledge(query)

        val isDiscovery = category != null && nameWords.all {
            it == "ranchi" || it in category.words || it in DISCOVERY_FILLER
        }
        val candidates = resolvePlace(nameWords.joinToString(" ")).filter(::validCoordinate)
        val matches = if (isDiscovery) {
            candidates.filter { category.matches(it) }
        } else {
            strongMatches(nameWords, candidates)
        }
        return if (matches.isEmpty()) {
            knowledge(query)
        } else {
            JoharQueryResult.Places(
                places = matches.distinctBy { it.id }.take(5),
                intro = if (isDiscovery) "Offline data mein ye matching jagah mili hain." else "Offline jaankari mein ye jagah mili.",
                kind = if (category?.isUtility == true) PlaceResultKind.UTILITY else PlaceResultKind.DISCOVERY,
            )
        }
    }

    private fun route(request: RouteRequest): JoharQueryResult {
        val origin = uniquePlace(request.origin)
            ?: return JoharQueryResult.Clarification(
                prompt = "Starting place clear nahi hui: ${request.origin}. Thoda specific landmark ya locality batayein.",
                options = emptyList(),
            )
        val destination = uniquePlace(request.destination)
            ?: return JoharQueryResult.Clarification(
                prompt = "Destination clear nahi hui: ${request.destination}. Thoda specific landmark ya locality batayein.",
                options = emptyList(),
            )

        if (!routeInstalled()) {
            return JoharQueryResult.Grounded(
                answer = JoharAnswer(
                    text = "Place lookup available hai, lekin road route abhi calculate nahi kar sakta.",
                    evidence = emptyList(),
                    mode = JoharAnswerMode.NO_ANSWER,
                ),
                tone = GroundedTone.OFFLINE,
                prefix = "Offline road routing pack abhi installed nahi hai.",
            )
        }

        return when (val result = route(origin.coordinate, destination.coordinate)) {
            is RanchiRouteResult.Success -> JoharQueryResult.Route(origin, destination, result)
            is RanchiRouteResult.Unavailable -> JoharQueryResult.Grounded(
                answer = JoharAnswer(result.reason, emptyList(), JoharAnswerMode.NO_ANSWER),
                tone = GroundedTone.WARNING,
                prefix = "Offline road route nahi mila.",
            )
        }
    }

    private fun compare(request: ComparisonRequest): JoharQueryResult? {
        val left = uniquePlace(request.left) ?: return null
        val right = uniquePlace(request.right) ?: return null
        if (left.id == right.id) return null
        return JoharQueryResult.Comparison(left, right)
    }

    private fun itinerary(requestedStops: List<String>): JoharQueryResult? {
        if (requestedStops.size !in 2..6) return null
        val resolved = requestedStops.map { uniquePlace(it) ?: return null }
            .distinctBy { it.id }
        if (resolved.size < 2) return null
        return JoharQueryResult.Itinerary(
            title = "Ranchi plan",
            places = resolved,
            summary = "Aapke diye hue stops ko isi order mein rakha hai. Travel time tabhi dikhayenge jab road routes calculate kiye jayen.",
        )
    }

    private fun uniquePlace(query: String): RanchiSpatialPlace? {
        val queryWords = words(query).filterNot { it in LOOKUP_FILLER || it in ROUTE_WORDS || it in COMPARISON_FILLER }
        return strongMatches(queryWords, resolvePlace(query).filter(::validCoordinate))
            .distinctBy { it.id }
            .singleOrNull()
    }

    private fun nearbyFrom(originQuery: String, category: Category): JoharQueryResult {
        val origins = strongMatches(words(originQuery).filterNot { it in LOOKUP_FILLER }, resolvePlace(originQuery))
            .filter(::validCoordinate).distinctBy { it.id }
        if (origins.size != 1) {
            pendingCategory = category
            return askOrigin(category)
        }
        pendingCategory = null
        val origin = origins.single()
        val places = nearby(origin.coordinate, category.searchTypes)
            .filter { validCoordinate(it) && it.id != origin.id && category.matches(it) }
            .filter { it.distanceKm?.let { distance -> distance.isFinite() && distance in 0.0..5.0 } == true }
            .distinctBy { it.id }.sortedBy { it.distanceKm }.take(5)
        return if (places.isEmpty()) {
            JoharQueryResult.Grounded(
                answer = JoharAnswer(
                    text = "${origin.name} ke 5 km ke andar offline data mein matching jagah nahi mili. Iska matlab yeh nahi ki wahan koi jagah nahi hai.",
                    evidence = emptyList(),
                    mode = JoharAnswerMode.NO_ANSWER,
                ),
                tone = GroundedTone.OFFLINE,
            )
        } else {
            JoharQueryResult.Places(
                places = places,
                intro = "${origin.name} se 5 km ke andar ye jagah mili hain. Doori seedhi rekha mein hai, sadak ki doori nahi.",
                kind = if (category.isUtility) PlaceResultKind.UTILITY else PlaceResultKind.DISCOVERY,
            )
        }
    }

    private fun askOrigin(category: Category) = JoharQueryResult.Clarification(
        prompt = "${category.displayName} kis locality ya landmark ke paas dhoondhein?",
        options = listOf("Lalpur", "Ranchi railway station", "Kanke", "Doranda"),
    )

    private fun knowledge(query: String) = JoharQueryResult.Grounded(knowledgeAnswer(query))

    /**
     * Prefer exact name-token matches, then accept only conservative typo matches.
     * Every meaningful query token still has to map to a token in the resolved place name.
     */
    private fun strongMatches(queryWords: List<String>, candidates: List<RanchiSpatialPlace>): List<RanchiSpatialPlace> {
        if (queryWords.isEmpty()) return emptyList()
        val exact = candidates.filter { words(it.name) == queryWords }
        if (exact.isNotEmpty()) return exact

        return candidates.mapNotNull { place ->
            val nameWords = words(place.name)
            var totalDistance = 0
            for (queryWord in queryWords) {
                val distances = nameWords.map { nameWord -> tokenDistance(queryWord, nameWord) }
                val bestDistance = distances.minOrNull() ?: return@mapNotNull null
                if (bestDistance > allowedTokenDistance(queryWord, nameWords)) return@mapNotNull null
                totalDistance += bestDistance
            }
            place to totalDistance
        }.sortedBy { (_, distance) -> distance }
            .map { (place, _) -> place }
    }

    private fun allowedTokenDistance(queryWord: String, candidateWords: List<String>): Int {
        if (queryWord.length <= 3) return 0
        val longest = maxOf(queryWord.length, candidateWords.maxOfOrNull(String::length) ?: 0)
        return if (longest >= 7) 2 else 1
    }

    private fun tokenDistance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)
        left.forEachIndexed { i, leftChar ->
            current[0] = i + 1
            right.forEachIndexed { j, rightChar ->
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (leftChar == rightChar) 0 else 1,
                )
            }
            for (index in previous.indices) previous[index] = current[index]
        }
        return previous[right.length]
    }

    private enum class Category(
        val displayName: String,
        val words: Set<String>,
        val types: Set<String>,
        val isUtility: Boolean = false,
    ) {
        TEMPLE("Mandir", setOf("mandir", "temple", "temples"), setOf("TEMPLE", "PILGRIMAGE")),
        HOSPITAL("Hospital", setOf("hospital", "hospitals", "clinic", "clinics"), setOf("HOSPITAL", "CLINIC"), true),
        PHARMACY("Pharmacy", setOf("pharmacy", "chemist", "medical"), setOf("PHARMACY", "SHOP"), true),
        POLICE("Police station", setOf("police", "thana"), setOf("POLICE_STATION"), true),
        FIRE("Fire station", setOf("fire"), setOf("FIRE_STATION", "EMERGENCY_SERVICE"), true),
        ATM("ATM", setOf("atm", "atms"), setOf("ATM"), true),
        BANK("Bank", setOf("bank", "banks"), setOf("BANK"), true),
        FUEL("Petrol pump", setOf("petrol", "fuel", "pump"), setOf("FUEL"), true),
        EV_CHARGING("EV charging", setOf("charging", "ev"), setOf("EV_CHARGING"), true),
        TOILET("Public toilet", setOf("toilet", "washroom"), setOf("TOILET"), true),
        PARKING("Parking", setOf("parking"), setOf("PARKING"), true),
        RESTAURANT("Restaurant", setOf("restaurant", "restaurants", "dhaba", "food"), setOf("RESTAURANT", "STREET_FOOD")),
        CAFE("Cafe", setOf("cafe", "cafes", "coffee"), setOf("CAFE")),
        HOTEL("Hotel", setOf("hotel", "hotels", "hostel", "guesthouse"), setOf("HOTEL")),
        MARKET("Market", setOf("market", "markets", "bazar", "bazaar", "haat", "mandi"), setOf("MARKET", "MARKET_COLLECTION", "MARKET_TYPE")),
        PARK("Park", setOf("park", "parks", "garden", "playground"), setOf("PARK", "GARDEN")),
        WATERFALL("Waterfall", setOf("waterfall", "waterfalls", "falls", "jharna"), setOf("WATERFALL", "NATURAL_FEATURE")),
        HILL("Hill", setOf("hill", "hills", "pahar", "viewpoint"), setOf("HILL", "VIEWPOINT", "TOURIST_ATTRACTION")),
        MUSEUM("Museum", setOf("museum", "museums", "heritage"), setOf("MUSEUM", "HERITAGE_SITE")),
        MALL("Mall", setOf("mall", "malls", "shopping"), setOf("MALL")),
        CINEMA("Cinema", setOf("cinema", "movie", "theatre"), setOf("CINEMA")),
        LIBRARY("Library", setOf("library", "libraries"), setOf("LIBRARY"), true),
        SCHOOL("School", setOf("school", "schools"), setOf("SCHOOL"), true),
        COLLEGE("College", setOf("college", "colleges", "university", "universities"), setOf("COLLEGE", "UNIVERSITY", "COLLEGE_UNIVERSITY"), true);

        val searchTypes: Set<String> get() = types + setOf("TOURIST_ATTRACTION", "FACILITY", "PLACE", "SHOP")
        fun matches(place: RanchiSpatialPlace): Boolean {
            val type = place.type.uppercase(Locale.ROOT)
            return type in types || (type in searchTypes && words(place.name).any { it in words })
        }
    }

    private data class RouteRequest(val origin: String, val destination: String)
    private data class ComparisonRequest(val left: String, val right: String)

    private companion object {
        val LOOKUP_FILLER = setOf(
            "kahan", "kaha", "hai", "hain", "where", "is", "the", "location", "address", "map", "on", "show", "me",
            "mein", "in", "batao", "dikhao", "please", "ki", "ka", "ke", "find", "search", "list", "some", "mujhe",
        )
        val DISCOVERY_FILLER = setOf("find", "search", "list", "show", "some", "best", "good", "top")
        val NEAR_WORDS = setOf("near", "nearby", "paas", "aas", "around")
        val NEAR_FILLER = NEAR_WORDS + setOf("mere", "meri", "my", "me", "to", "ke", "ki", "ka")
        val LIVE_WORDS = setOf(
            "open", "closed", "now", "today", "aaj", "timing", "timings", "hours", "weather", "stock",
            "available", "availability", "traffic", "current", "live", "status", "crowded", "working", "schedule", "schedules",
        )
        val KNOWLEDGE_WORDS = setOf(
            "kya", "what", "why", "kyun", "history", "itihaas", "price", "fee", "fees", "rating", "safe", "safety",
            "kab", "kitna", "kitne", "walking", "stairs", "accessible", "about", "explain", "famous", "special",
        )
        val QUESTION_WORDS = KNOWLEDGE_WORDS + LIVE_WORDS + NEAR_WORDS + setOf("how", "kaise")
        val ROUTE_WORDS = setOf("route", "navigate", "navigation", "kaise", "how", "from", "se", "to", "jaye", "jana")
        val COMPARISON_FILLER = setOf("vs", "versus", "compare", "better", "best", "or", "ya")
        val ROUTE_PATTERN = Regex(
            "^(?:route\\s+)?(?:from\\s+)?(.+?)\\s+(?:to|se)\\s+(.+?)(?:\\s+(?:kaise(?:\\s+jaye)?|route|navigate|navigation|jana))?$",
            RegexOption.IGNORE_CASE,
        )
        val COMPARISON_PATTERN = Regex(
            "^(?:compare\\s+)?(.+?)\\s+(?:vs|versus|or|ya)\\s+(.+?)(?:\\s+(?:which\\s+is\\s+better|better|kaun\\s+better))?$",
            RegexOption.IGNORE_CASE,
        )

        fun parseRouteRequest(text: String): RouteRequest? {
            val cleaned = text.trim().trimEnd('?', '!', '.')
            val match = ROUTE_PATTERN.matchEntire(cleaned) ?: return null
            val origin = match.groupValues[1].trim()
            val destination = match.groupValues[2].trim()
            return if (origin.isBlank() || destination.isBlank()) null else RouteRequest(origin, destination)
        }

        fun parseComparisonRequest(text: String): ComparisonRequest? {
            val cleaned = text.trim().trimEnd('?', '!', '.')
            val match = COMPARISON_PATTERN.matchEntire(cleaned) ?: return null
            val left = match.groupValues[1].trim()
            val right = match.groupValues[2].trim()
            return if (left.isBlank() || right.isBlank()) null else ComparisonRequest(left, right)
        }

        fun parseExplicitItinerary(text: String): List<String>? {
            val normalized = text.trim().trimEnd('?', '!', '.')
            val lower = normalized.lowercase(Locale.ROOT)
            if (listOf("itinerary", "plan", "day trip").none(lower::contains)) return null
            val payload = normalized
                .replace(Regex("(?i)^.*?(?:itinerary|plan|day trip)\\s*(?::|for)?\\s*"), "")
                .trim()
            if (payload.isBlank()) return null
            val stops = payload.split(Regex("(?i)\\s*(?:,|->| then | and | aur )\\s*"))
                .map(String::trim)
                .filter { it.length >= 3 }
            return stops.takeIf { it.size >= 2 }
        }

        fun words(text: String): List<String> = text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .map(::normalizeToken)
            .filterNot { it == "railway" }

        fun normalizeToken(token: String): String {
            val direct = when (token) {
                "junction", "stn" -> "station"
                "temple", "templ" -> "mandir"
                "guest" -> "guesthouse"
                "fall", "waterfall", "waterfalls" -> "falls"
                "damm", "daam" -> "dam"
                "maidan", "grnd" -> "ground"
                else -> token
            }
            if (direct != token) return direct

            val fuzzy = CANONICAL_QUERY_TOKENS
                .map { candidate -> candidate to tokenDistanceStatic(token, candidate) }
                .filter { (candidate, distance) ->
                    val maxLength = maxOf(token.length, candidate.length)
                    token.length >= 4 && distance <= if (maxLength >= 7) 2 else 1
                }
                .sortedBy { (_, distance) -> distance }
            return if (fuzzy.size == 1 || (fuzzy.size > 1 && fuzzy[0].second < fuzzy[1].second)) fuzzy[0].first else token
        }

        fun tokenDistanceStatic(left: String, right: String): Int {
            if (left == right) return 0
            if (left.isEmpty()) return right.length
            if (right.isEmpty()) return left.length
            val previous = IntArray(right.length + 1) { it }
            val current = IntArray(right.length + 1)
            left.forEachIndexed { i, leftChar ->
                current[0] = i + 1
                right.forEachIndexed { j, rightChar ->
                    current[j + 1] = minOf(
                        current[j] + 1,
                        previous[j + 1] + 1,
                        previous[j] + if (leftChar == rightChar) 0 else 1,
                    )
                }
                for (index in previous.indices) previous[index] = current[index]
            }
            return previous[right.length]
        }

        val CANONICAL_QUERY_TOKENS = setOf(
            "station", "railway", "mandir", "garden", "dam", "falls", "airport", "ground", "university", "ranchi",
        )

        fun validCoordinate(place: RanchiSpatialPlace) = place.coordinate.latitude.isFinite() &&
            place.coordinate.longitude.isFinite() && place.coordinate.latitude in -90.0..90.0 &&
            place.coordinate.longitude in -180.0..180.0
    }
}

enum class GroundedTone {
    NORMAL,
    OFFLINE,
    NOT_CONFIRMED,
    WARNING,
}

enum class PlaceResultKind {
    DISCOVERY,
    UTILITY,
}

sealed interface JoharQueryResult {
    data class Grounded(
        val answer: JoharAnswer,
        val tone: GroundedTone = GroundedTone.NORMAL,
        val prefix: String? = null,
    ) : JoharQueryResult

    data class Places(
        val places: List<RanchiSpatialPlace>,
        val intro: String,
        val kind: PlaceResultKind = PlaceResultKind.DISCOVERY,
    ) : JoharQueryResult

    data class Route(
        val origin: RanchiSpatialPlace,
        val destination: RanchiSpatialPlace,
        val route: RanchiRouteResult.Success,
    ) : JoharQueryResult

    data class Comparison(
        val left: RanchiSpatialPlace,
        val right: RanchiSpatialPlace,
    ) : JoharQueryResult

    data class Itinerary(
        val title: String,
        val places: List<RanchiSpatialPlace>,
        val summary: String,
    ) : JoharQueryResult

    data class Clarification(
        val prompt: String,
        val options: List<String>,
    ) : JoharQueryResult
}
