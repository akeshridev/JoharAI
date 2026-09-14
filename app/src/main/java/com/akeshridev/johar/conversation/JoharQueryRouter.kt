package com.akeshridev.johar.conversation

import com.akeshridev.johar.data.spatial.RanchiCoordinate
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import java.util.Locale

/** Small synchronous router. The owner runs queries serially off the UI thread. */
class JoharQueryRouter(
    private val resolvePlace: (String) -> List<RanchiSpatialPlace>,
    private val nearby: (RanchiCoordinate, Set<String>) -> List<RanchiSpatialPlace>,
    private val knowledgeAnswer: (String) -> String,
) {
    private var pendingCategory: Category? = null

    fun answer(query: String): JoharQueryResult {
        val words = words(query)
        // A location card cannot answer these questions or prove current conditions.
        if (words.any { it in KNOWLEDGE_WORDS }) {
            pendingCategory = null
            return if (words.any { it in LIVE_WORDS }) {
                JoharQueryResult.Text("Abhi ki timing, status ya availability confirm nahi hai. Offline jaankari: ${knowledgeAnswer(query)}")
            } else knowledge(query)
        }

        val category = Category.entries.firstOrNull { c -> words.any { it in c.words } }
        val isNearby = words.any { it in NEAR_WORDS }
        if (isNearby && category != null) {
            val originWords = words.filterNot { it in category.words || it in NEAR_FILLER || it in LOOKUP_FILLER }
            pendingCategory = category
            if (originWords.isEmpty()) return askOrigin()
            return nearbyFrom(originWords.joinToString(" "), category)
        }

        val pending = pendingCategory
        pendingCategory = null
        if (pending != null && words.size <= 4 && words.none { it in QUESTION_WORDS }) {
            return nearbyFrom(query, pending)
        }
        // Unsupported near/route requests stay with knowledge, never become a guessed place.
        if (isNearby || words.any { it in ROUTE_WORDS }) return knowledge(query)

        val nameWords = words.filterNot { it in LOOKUP_FILLER }
        if (nameWords.isEmpty() || words.size > 8) return knowledge(query)
        val isDiscovery = category != null && nameWords.all { it == "ranchi" || it in category.words }
        val candidates = resolvePlace(nameWords.joinToString(" ")).filter(::validCoordinate)
        val matches = if (isDiscovery) {
            candidates.filter { category.matches(it) }
        } else {
            strongMatches(nameWords, candidates)
        }
        return if (matches.isEmpty()) knowledge(query) else JoharQueryResult.Places(
            places = matches.distinctBy { it.id }.take(5),
            intro = if (isDiscovery) "Offline jaankari mein ye jagah mili hain." else "Offline jaankari mein ye jagah mili.",
        )
    }

    private fun nearbyFrom(originQuery: String, category: Category): JoharQueryResult {
        val origins = strongMatches(words(originQuery).filterNot { it in LOOKUP_FILLER }, resolvePlace(originQuery))
            .filter(::validCoordinate).distinctBy { it.id }
        if (origins.size != 1) {
            pendingCategory = category
            return JoharQueryResult.Text("Jagah clear nahi hui. Kis locality ya landmark ke paas dhoondhein?")
        }
        pendingCategory = null
        val origin = origins.single()
        val places = nearby(origin.coordinate, category.searchTypes)
            .filter { validCoordinate(it) && it.id != origin.id && category.matches(it) }
            .filter { it.distanceKm?.let { distance -> distance.isFinite() && distance in 0.0..5.0 } == true }
            .distinctBy { it.id }.sortedBy { it.distanceKm }.take(5)
        return if (places.isEmpty()) {
            JoharQueryResult.Text("${origin.name} ke 5 km ke andar offline data mein matching jagah nahi mili. Iska matlab yeh nahi ki wahan koi jagah nahi hai.")
        } else {
            JoharQueryResult.Places(places, "${origin.name} se 5 km ke andar ye jagah mili hain. Doori seedhi rekha mein hai, sadak ki doori nahi.")
        }
    }

    private fun askOrigin() = JoharQueryResult.Text("Kis locality ya landmark ke paas? Jaise Lalpur ya Ranchi railway station.")
    private fun knowledge(query: String) = JoharQueryResult.Text(knowledgeAnswer(query))

    /** Require all meaningful name tokens; the spatial engine's rank alone is not confidence. */
    private fun strongMatches(queryWords: List<String>, candidates: List<RanchiSpatialPlace>): List<RanchiSpatialPlace> {
        if (queryWords.isEmpty()) return emptyList()
        val exact = candidates.filter { words(it.name) == queryWords }
        if (exact.isNotEmpty()) return exact
        return candidates.filter { place -> queryWords.all { it in words(place.name) } }
    }

    private enum class Category(val words: Set<String>, val types: Set<String>) {
        TEMPLE(setOf("mandir", "temple", "temples"), setOf("TEMPLE", "PILGRIMAGE")),
        HOSPITAL(setOf("hospital", "hospitals"), setOf("HOSPITAL")),
        PARK(setOf("park", "parks", "garden"), setOf("PARK", "GARDEN")),
        MARKET(setOf("market", "markets", "bazar", "haat"), setOf("MARKET"));

        val searchTypes: Set<String> get() = types + setOf("TOURIST_ATTRACTION", "FACILITY", "PLACE")
        fun matches(place: RanchiSpatialPlace): Boolean = place.type.uppercase(Locale.ROOT) in types ||
            (place.type.uppercase(Locale.ROOT) in searchTypes && words(place.name).any { it in words })
    }

    private companion object {
        val LOOKUP_FILLER = setOf("kahan", "kaha", "hai", "hain", "where", "is", "the", "location", "address", "map", "on", "show", "me", "mein", "in", "batao", "dikhao", "please", "ki", "ka", "ke")
        val NEAR_WORDS = setOf("near", "nearby", "paas", "aas", "around")
        val NEAR_FILLER = NEAR_WORDS + setOf("mere", "meri", "my", "me", "to")
        val LIVE_WORDS = setOf("open", "closed", "now", "today", "aaj", "timing", "timings", "hours", "weather", "stock", "available", "availability")
        val KNOWLEDGE_WORDS = setOf("kya", "what", "why", "kyun", "history", "itihaas", "open", "closed", "now", "today", "aaj", "timing", "timings", "hours", "price", "fee", "fees", "rating", "safe", "safety", "weather", "stock", "available", "availability", "kab", "kitna", "kitne", "walking", "stairs", "accessible")
        val QUESTION_WORDS = KNOWLEDGE_WORDS + NEAR_WORDS + setOf("how", "kaise")
        val ROUTE_WORDS = setOf("route", "navigate", "navigation", "kaise", "how", "from", "se", "to")
        fun words(text: String): List<String> = text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").trim().split(Regex("\\s+"))
            .filter(String::isNotBlank).map {
                when (it) { "junction" -> "station"; "temple" -> "mandir"; else -> it }
            }.filterNot { it == "railway" }
        fun validCoordinate(place: RanchiSpatialPlace) = place.coordinate.latitude.isFinite() &&
            place.coordinate.longitude.isFinite() && place.coordinate.latitude in -90.0..90.0 &&
            place.coordinate.longitude in -180.0..180.0
    }
}

sealed interface JoharQueryResult {
    data class Text(val text: String) : JoharQueryResult
    data class Places(val places: List<RanchiSpatialPlace>, val intro: String) : JoharQueryResult
}
