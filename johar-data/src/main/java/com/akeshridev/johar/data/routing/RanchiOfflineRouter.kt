package com.akeshridev.johar.data.routing

import android.content.Context
import com.akeshridev.johar.data.spatial.RanchiCoordinate
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
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
 *
 * Runtime graph/search storage intentionally uses primitive arrays rather than object-heavy
 * HashMaps/lists. The 1000-query stress suite exposed Android heap exhaustion when repeated
 * A* searches had to grow boxed HashMaps while the full routing graph was resident in memory.
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
            val point = activeGraph.coordinate(start)
            return RanchiRouteResult.Success(
                points = listOf(point),
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                snappedOrigin = point,
                snappedDestination = point,
            )
        }

        val nodeCount = activeGraph.nodeCount
        val bestSeconds = DoubleArray(nodeCount) { Double.POSITIVE_INFINITY }
        val previousNode = IntArray(nodeCount) { -1 }
        val previousDistanceMeters = FloatArray(nodeCount)
        val previousDurationSeconds = FloatArray(nodeCount)
        val open = SearchMinHeap()

        bestSeconds[start] = 0.0
        open.add(
            nodeId = start,
            elapsedSeconds = 0.0,
            estimatedTotalSeconds = activeGraph.heuristicSeconds(start, end),
        )

        while (open.isNotEmpty()) {
            open.removeMin()
            val currentNode = open.removedNodeId
            val currentElapsed = open.removedElapsedSeconds

            if (currentElapsed > bestSeconds[currentNode]) continue
            if (currentNode == end) {
                return activeGraph.buildResult(
                    start = start,
                    end = end,
                    previousNode = previousNode,
                    previousDistanceMeters = previousDistanceMeters,
                    previousDurationSeconds = previousDurationSeconds,
                )
            }

            var edgeIndex = activeGraph.firstEdgeIndex(currentNode)
            val edgeEnd = activeGraph.edgeEndIndex(currentNode)
            while (edgeIndex < edgeEnd) {
                val nextNode = activeGraph.edgeTarget(edgeIndex)
                val edgeDuration = activeGraph.edgeDurationSeconds(edgeIndex)
                val candidate = currentElapsed + edgeDuration

                if (candidate < bestSeconds[nextNode]) {
                    bestSeconds[nextNode] = candidate
                    previousNode[nextNode] = currentNode
                    previousDistanceMeters[nextNode] = activeGraph.edgeDistanceMeters(edgeIndex)
                    previousDurationSeconds[nextNode] = edgeDuration
                    open.add(
                        nodeId = nextNode,
                        elapsedSeconds = candidate,
                        estimatedTotalSeconds = candidate + activeGraph.heuristicSeconds(nextNode, end),
                    )
                }
                edgeIndex++
            }
        }

        return RanchiRouteResult.Unavailable("No offline road route found inside Ranchi.")
    }

    private fun loadGraph(): RoutingGraph {
        val nodeIds = IntBuffer()
        val nodeLatitudes = DoubleBuffer()
        val nodeLongitudes = DoubleBuffer()
        val edgeFromIds = IntBuffer()
        val edgeToIds = IntBuffer()
        val edgeDistances = FloatBuffer()
        val edgeDurations = FloatBuffer()

        openRoutingAsset().use { asset ->
            openDecodedStream(asset).use { decoded ->
                BufferedReader(InputStreamReader(decoded)).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isBlank() || line.startsWith('#')) return@forEach
                        val parts = line.split('\t')
                        when (parts.firstOrNull()) {
                            "N" -> {
                                require(parts.size == 4) { "Invalid routing node row: $line" }
                                nodeIds.add(parts[1].toInt())
                                nodeLatitudes.add(parts[2].toDouble())
                                nodeLongitudes.add(parts[3].toDouble())
                            }
                            "E" -> {
                                require(parts.size == 5) { "Invalid routing edge row: $line" }
                                edgeFromIds.add(parts[1].toInt())
                                edgeToIds.add(parts[2].toInt())
                                edgeDistances.add(parts[3].toFloat())
                                edgeDurations.add(parts[4].toFloat())
                            }
                            else -> error("Unknown routing graph row: $line")
                        }
                    }
                }
            }
        }

        require(nodeIds.size > 0) { "Ranchi routing graph has no nodes" }
        require(edgeFromIds.size > 0) { "Ranchi routing graph has no edges" }
        require(nodeIds.size == nodeLatitudes.size && nodeIds.size == nodeLongitudes.size) {
            "Ranchi routing graph node columns are inconsistent"
        }
        require(
            edgeFromIds.size == edgeToIds.size &&
                edgeFromIds.size == edgeDistances.size &&
                edgeFromIds.size == edgeDurations.size,
        ) { "Ranchi routing graph edge columns are inconsistent" }

        val ids = nodeIds.toArray()
        val latitudes = nodeLatitudes.toArray()
        val longitudes = nodeLongitudes.toArray()
        val idToIndex = IntIntIndex(ids.size)
        ids.forEachIndexed { index, id ->
            require(idToIndex.putIfAbsent(id, index)) { "Duplicate routing node id: $id" }
        }

        val rawFrom = edgeFromIds.toArray()
        val rawTo = edgeToIds.toArray()
        val rawDistances = edgeDistances.toArray()
        val rawDurations = edgeDurations.toArray()
        val edgeCounts = IntArray(ids.size)

        for (i in rawFrom.indices) {
            val from = idToIndex.get(rawFrom[i])
                ?: error("Routing edge references missing from-node ${rawFrom[i]}")
            require(idToIndex.get(rawTo[i]) != null) {
                "Routing edge references missing to-node ${rawTo[i]}"
            }
            edgeCounts[from]++
        }

        val edgeOffsets = IntArray(ids.size + 1)
        for (i in edgeCounts.indices) {
            edgeOffsets[i + 1] = edgeOffsets[i] + edgeCounts[i]
        }

        val edgeTargets = IntArray(rawFrom.size)
        val edgeDistanceMeters = FloatArray(rawFrom.size)
        val edgeDurationSeconds = FloatArray(rawFrom.size)
        val writeCursor = edgeOffsets.copyOf()

        for (i in rawFrom.indices) {
            val from = idToIndex.get(rawFrom[i])!!
            val to = idToIndex.get(rawTo[i])!!
            val targetIndex = writeCursor[from]++
            edgeTargets[targetIndex] = to
            edgeDistanceMeters[targetIndex] = rawDistances[i]
            edgeDurationSeconds[targetIndex] = rawDurations[i]
        }

        return RoutingGraph(
            latitudes = latitudes,
            longitudes = longitudes,
            edgeOffsets = edgeOffsets,
            edgeTargets = edgeTargets,
            edgeDistanceMeters = edgeDistanceMeters,
            edgeDurationSeconds = edgeDurationSeconds,
        )
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

private class RoutingGraph(
    private val latitudes: DoubleArray,
    private val longitudes: DoubleArray,
    private val edgeOffsets: IntArray,
    private val edgeTargets: IntArray,
    private val edgeDistanceMeters: FloatArray,
    private val edgeDurationSeconds: FloatArray,
) {
    val nodeCount: Int get() = latitudes.size

    fun coordinate(nodeIndex: Int): RanchiCoordinate = RanchiCoordinate(
        latitude = latitudes[nodeIndex],
        longitude = longitudes[nodeIndex],
    )

    fun nearestNode(point: RanchiCoordinate, maxDistanceMeters: Double): Int? {
        var bestIndex = -1
        var bestDistance = maxDistanceMeters
        for (index in latitudes.indices) {
            val distance = distanceMeters(
                point.latitude,
                point.longitude,
                latitudes[index],
                longitudes[index],
            )
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex.takeIf { it >= 0 }
    }

    fun heuristicSeconds(from: Int, to: Int): Double {
        val distance = distanceMeters(
            latitudes[from],
            longitudes[from],
            latitudes[to],
            longitudes[to],
        )
        return distance / MAX_EXPECTED_SPEED_METERS_PER_SECOND
    }

    fun firstEdgeIndex(nodeIndex: Int): Int = edgeOffsets[nodeIndex]

    fun edgeEndIndex(nodeIndex: Int): Int = edgeOffsets[nodeIndex + 1]

    fun edgeTarget(edgeIndex: Int): Int = edgeTargets[edgeIndex]

    fun edgeDistanceMeters(edgeIndex: Int): Float = edgeDistanceMeters[edgeIndex]

    fun edgeDurationSeconds(edgeIndex: Int): Float = edgeDurationSeconds[edgeIndex]

    fun buildResult(
        start: Int,
        end: Int,
        previousNode: IntArray,
        previousDistanceMeters: FloatArray,
        previousDurationSeconds: FloatArray,
    ): RanchiRouteResult.Success {
        val reversePath = IntBuffer()
        reversePath.add(end)

        var cursor = end
        var distance = 0.0
        var duration = 0.0
        while (cursor != start) {
            val previous = previousNode[cursor]
            check(previous >= 0) { "Broken route predecessor chain" }
            distance += previousDistanceMeters[cursor]
            duration += previousDurationSeconds[cursor]
            cursor = previous
            reversePath.add(cursor)
        }

        val points = ArrayList<RanchiCoordinate>(reversePath.size)
        for (i in reversePath.size - 1 downTo 0) {
            points += coordinate(reversePath[i])
        }

        return RanchiRouteResult.Success(
            points = points,
            distanceMeters = distance,
            durationSeconds = duration,
            snappedOrigin = coordinate(start),
            snappedDestination = coordinate(end),
        )
    }

    private fun distanceMeters(
        latitudeA: Double,
        longitudeA: Double,
        latitudeB: Double,
        longitudeB: Double,
    ): Double {
        val earthRadiusMeters = 6_371_008.8
        val lat1 = Math.toRadians(latitudeA)
        val lat2 = Math.toRadians(latitudeB)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(longitudeB - longitudeA)
        val haversine = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusMeters * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    companion object {
        // Admissible heuristic for car routing: 130 km/h upper bound.
        private const val MAX_EXPECTED_SPEED_METERS_PER_SECOND = 36.111111
    }
}

/** Primitive binary min-heap keyed by estimated total route time. */
private class SearchMinHeap(initialCapacity: Int = 1_024) {
    private var nodeIds = IntArray(initialCapacity)
    private var elapsedSeconds = DoubleArray(initialCapacity)
    private var estimatedTotalSeconds = DoubleArray(initialCapacity)
    private var size = 0

    var removedNodeId: Int = -1
        private set
    var removedElapsedSeconds: Double = 0.0
        private set

    fun isNotEmpty(): Boolean = size > 0

    fun add(nodeId: Int, elapsedSeconds: Double, estimatedTotalSeconds: Double) {
        ensureCapacity(size + 1)
        var index = size++
        while (index > 0) {
            val parent = (index - 1) ushr 1
            if (this.estimatedTotalSeconds[parent] <= estimatedTotalSeconds) break
            copyEntry(parent, index)
            index = parent
        }
        nodeIds[index] = nodeId
        this.elapsedSeconds[index] = elapsedSeconds
        this.estimatedTotalSeconds[index] = estimatedTotalSeconds
    }

    fun removeMin() {
        check(size > 0) { "Cannot remove from an empty routing heap" }
        removedNodeId = nodeIds[0]
        removedElapsedSeconds = elapsedSeconds[0]

        size--
        if (size == 0) return

        val lastNode = nodeIds[size]
        val lastElapsed = elapsedSeconds[size]
        val lastEstimated = estimatedTotalSeconds[size]
        var index = 0

        while (true) {
            val left = index * 2 + 1
            if (left >= size) break
            val right = left + 1
            val child = if (
                right < size && estimatedTotalSeconds[right] < estimatedTotalSeconds[left]
            ) right else left
            if (estimatedTotalSeconds[child] >= lastEstimated) break
            copyEntry(child, index)
            index = child
        }

        nodeIds[index] = lastNode
        elapsedSeconds[index] = lastElapsed
        estimatedTotalSeconds[index] = lastEstimated
    }

    private fun copyEntry(from: Int, to: Int) {
        nodeIds[to] = nodeIds[from]
        elapsedSeconds[to] = elapsedSeconds[from]
        estimatedTotalSeconds[to] = estimatedTotalSeconds[from]
    }

    private fun ensureCapacity(required: Int) {
        if (required <= nodeIds.size) return
        val newCapacity = maxOf(required, nodeIds.size * 2)
        nodeIds = nodeIds.copyOf(newCapacity)
        elapsedSeconds = elapsedSeconds.copyOf(newCapacity)
        estimatedTotalSeconds = estimatedTotalSeconds.copyOf(newCapacity)
    }
}

/** Primitive int -> int map used only while converting OSM node ids to dense runtime indexes. */
private class IntIntIndex(expectedSize: Int) {
    private val capacity = tableCapacity(expectedSize)
    private val mask = capacity - 1
    private val keys = IntArray(capacity)
    private val values = IntArray(capacity)
    private val occupied = BooleanArray(capacity)

    fun putIfAbsent(key: Int, value: Int): Boolean {
        var slot = mix(key) and mask
        while (occupied[slot]) {
            if (keys[slot] == key) return false
            slot = (slot + 1) and mask
        }
        occupied[slot] = true
        keys[slot] = key
        values[slot] = value
        return true
    }

    fun get(key: Int): Int? {
        var slot = mix(key) and mask
        while (occupied[slot]) {
            if (keys[slot] == key) return values[slot]
            slot = (slot + 1) and mask
        }
        return null
    }

    private fun mix(value: Int): Int {
        var x = value
        x = x xor (x ushr 16)
        x *= -0x7a143595
        x = x xor (x ushr 15)
        x *= -0x3d4d51cb
        return x xor (x ushr 16)
    }

    companion object {
        private fun tableCapacity(expectedSize: Int): Int {
            var capacity = 16
            val required = ((expectedSize / 0.65) + 1).toInt()
            while (capacity < required) {
                require(capacity <= (1 shl 29)) { "Routing graph is too large" }
                capacity = capacity shl 1
            }
            return capacity
        }
    }
}

private class IntBuffer(initialCapacity: Int = 1_024) {
    private var values = IntArray(initialCapacity)
    var size: Int = 0
        private set

    operator fun get(index: Int): Int = values[index]

    fun add(value: Int) {
        ensureCapacity(size + 1)
        values[size++] = value
    }

    fun toArray(): IntArray = values.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= values.size) return
        values = values.copyOf(maxOf(required, values.size * 2))
    }
}

private class FloatBuffer(initialCapacity: Int = 1_024) {
    private var values = FloatArray(initialCapacity)
    var size: Int = 0
        private set

    fun add(value: Float) {
        ensureCapacity(size + 1)
        values[size++] = value
    }

    fun toArray(): FloatArray = values.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= values.size) return
        values = values.copyOf(maxOf(required, values.size * 2))
    }
}

private class DoubleBuffer(initialCapacity: Int = 1_024) {
    private var values = DoubleArray(initialCapacity)
    var size: Int = 0
        private set

    fun add(value: Double) {
        ensureCapacity(size + 1)
        values[size++] = value
    }

    fun toArray(): DoubleArray = values.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= values.size) return
        values = values.copyOf(maxOf(required, values.size * 2))
    }
}