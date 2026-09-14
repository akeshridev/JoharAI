package com.akeshridev.johar.data.routing

import android.content.Context
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.PriorityQueue
import java.util.zip.GZIPInputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Self-contained offline car routing for Ranchi.
 *
 * The bundled graph is generated at build time from OSM roads clipped to Ranchi district.
 * Runtime never calls a network service and cannot route outside the bundled graph.
 */
class RanchiOfflineRouter(
    private val context: Context,
    private val assetPath: String = DEFAULT_ASSET,
) {
    @Volatile private var graph: RoutingGraph? = null

    fun isInstalled(): Boolean = runCatching {
        openRoutingAsset().close()
        true
    }.getOrDefault(false)

    fun route(
        origin: RanchiCoordinate,
        destination: RanchiCoordinate,
        maxSnapDistanceMeters: Double = 2_500.0,
    ): RanchiRouteResult {
        val activeGraph = graph ?: synchronized(this) {
            graph ?: loadGraph().also { graph = it }
        }

        val start = activeGraph.nearestNode(origin, maxSnapDistanceMeters)
            ?: return RanchiRouteResult.Unavailable("Origin is outside the Ranchi routing graph or too far from a routable road.")
        val end = activeGraph.nearestNode(destination, maxSnapDistanceMeters)
            ?: return RanchiRouteResult.Unavailable("Destination is outside the Ranchi routing graph or too far from a routable road.")

        if (start == end) {
            val point = activeGraph.nodes.getValue(start).coordinate
            return RanchiRouteResult.Success(
                points = listOf(point),
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                snappedOrigin = point,
                snappedDestination = point,
            )
        }

        val open = PriorityQueue(compareBy<SearchState> { it.estimatedTotalSeconds })
        val best = mutableMapOf<Int, Double>()
        val previous = mutableMapOf<Int, PreviousStep>()
        best[start] = 0.0
        open += SearchState(start, 0.0, activeGraph.heuristicSeconds(start, end))

        while (open.isNotEmpty()) {
            val current = open.remove()
            if (current.elapsedSeconds > (best[current.nodeId] ?: Double.POSITIVE_INFINITY)) continue
            if (current.nodeId == end) {
                return activeGraph.buildResult(start, end, previous)
            }

            activeGraph.edges[current.nodeId].orEmpty().forEach { edge ->
                val candidate = current.elapsedSeconds + edge.durationSeconds
                if (candidate < (best[edge.to] ?: Double.POSITIVE_INFINITY)) {
                    best[edge.to] = candidate
                    previous[edge.to] = PreviousStep(
                        from = current.nodeId,
                        distanceMeters = edge.distanceMeters,
                        durationSeconds = edge.durationSeconds,
                    )
                    open += SearchState(
                        nodeId = edge.to,
                        elapsedSeconds = candidate,
                        estimatedTotalSeconds = candidate + activeGraph.heuristicSeconds(edge.to, end),
                    )
                }
            }
        }

        return RanchiRouteResult.Unavailable("No offline road route found inside Ranchi.")
    }

    private fun loadGraph(): RoutingGraph {
        val nodes = HashMap<Int, RoutingNode>()
        val edges = HashMap<Int, MutableList<RoutingEdge>>()
        openRoutingAsset().use { asset ->
            openDecodedStream(asset).use { decoded ->
                BufferedReader(InputStreamReader(decoded)).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isBlank() || line.startsWith('#')) return@forEach
                        val parts = line.split('\t')
                        when (parts.firstOrNull()) {
                            "N" -> {
                                require(parts.size == 4) { "Invalid routing node row: $line" }
                                val id = parts[1].toInt()
                                nodes[id] = RoutingNode(
                                    id = id,
                                    coordinate = RanchiCoordinate(
                                        latitude = parts[2].toDouble(),
                                        longitude = parts[3].toDouble(),
                                    ),
                                )
                            }
                            "E" -> {
                                require(parts.size == 5) { "Invalid routing edge row: $line" }
                                val from = parts[1].toInt()
                                edges.getOrPut(from) { mutableListOf() } += RoutingEdge(
                                    to = parts[2].toInt(),
                                    distanceMeters = parts[3].toDouble(),
                                    durationSeconds = parts[4].toDouble(),
                                )
                            }
                            else -> error("Unknown routing graph row: $line")
                        }
                    }
                }
            }
        }
        require(nodes.isNotEmpty()) { "Ranchi routing graph has no nodes" }
        require(edges.isNotEmpty()) { "Ranchi routing graph has no edges" }
        return RoutingGraph(nodes, edges)
    }

    private fun openRoutingAsset(): InputStream {
        return runCatching { context.assets.open(assetPath) }
            .recoverCatching {
                if (assetPath == DEFAULT_ASSET) context.assets.open(LEGACY_GZIP_ASSET) else throw it
            }
            .getOrThrow()
    }

    /**
     * AAPT may expose a source `.tsv.gz` asset in the APK as `.tsv` after transparent decompression.
     * Sniff the gzip magic bytes instead of assuming compression from the packaged filename.
     */
    private fun openDecodedStream(input: InputStream): InputStream {
        val buffered = BufferedInputStream(input)
        buffered.mark(2)
        val first = buffered.read()
        val second = buffered.read()
        buffered.reset()
        return if (first == GZIP_MAGIC_1 && second == GZIP_MAGIC_2) GZIPInputStream(buffered) else buffered
    }

    companion object {
        // Android packaging exposes the generated `.tsv.gz` asset under this decompressed APK name.
        const val DEFAULT_ASSET = "routing/ranchi-routing-v1.tsv"
        private const val LEGACY_GZIP_ASSET = "routing/ranchi-routing-v1.tsv.gz"
        private const val GZIP_MAGIC_1 = 0x1f
        private const val GZIP_MAGIC_2 = 0x8b
    }
}

sealed interface RanchiRouteResult {
    data class Success(
        val points: List<RanchiCoordinate>,
        val distanceMeters: Double,
        val durationSeconds: Double,
        val snappedOrigin: RanchiCoordinate,
        val snappedDestination: RanchiCoordinate,
    ) : RanchiRouteResult

    data class Unavailable(val reason: String) : RanchiRouteResult
}

private data class RoutingNode(val id: Int, val coordinate: RanchiCoordinate)
private data class RoutingEdge(val to: Int, val distanceMeters: Double, val durationSeconds: Double)
private data class SearchState(val nodeId: Int, val elapsedSeconds: Double, val estimatedTotalSeconds: Double)
private data class PreviousStep(val from: Int, val distanceMeters: Double, val durationSeconds: Double)

private class RoutingGraph(
    val nodes: Map<Int, RoutingNode>,
    val edges: Map<Int, List<RoutingEdge>>,
) {
    fun nearestNode(point: RanchiCoordinate, maxDistanceMeters: Double): Int? {
        var bestId: Int? = null
        var bestDistance = maxDistanceMeters
        nodes.values.forEach { node ->
            val distance = distanceMeters(point, node.coordinate)
            if (distance < bestDistance) {
                bestDistance = distance
                bestId = node.id
            }
        }
        return bestId
    }

    fun heuristicSeconds(from: Int, to: Int): Double {
        val distance = distanceMeters(nodes.getValue(from).coordinate, nodes.getValue(to).coordinate)
        return distance / MAX_EXPECTED_SPEED_METERS_PER_SECOND
    }

    fun buildResult(
        start: Int,
        end: Int,
        previous: Map<Int, PreviousStep>,
    ): RanchiRouteResult.Success {
        val nodeIds = mutableListOf(end)
        var cursor = end
        var distance = 0.0
        var duration = 0.0
        while (cursor != start) {
            val step = previous[cursor] ?: error("Broken route predecessor chain")
            distance += step.distanceMeters
            duration += step.durationSeconds
            cursor = step.from
            nodeIds += cursor
        }
        nodeIds.reverse()
        return RanchiRouteResult.Success(
            points = nodeIds.map { nodes.getValue(it).coordinate },
            distanceMeters = distance,
            durationSeconds = duration,
            snappedOrigin = nodes.getValue(start).coordinate,
            snappedDestination = nodes.getValue(end).coordinate,
        )
    }

    private fun distanceMeters(a: RanchiCoordinate, b: RanchiCoordinate): Double {
        val earthRadiusMeters = 6_371_008.8
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val haversine = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusMeters * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    companion object {
        // Admissible heuristic for car routing: 130 km/h upper bound.
        private const val MAX_EXPECTED_SPEED_METERS_PER_SECOND = 36.111111
    }
}
