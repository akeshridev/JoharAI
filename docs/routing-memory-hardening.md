# Ranchi offline routing memory hardening

## Why this change exists

The 1000-query Android evaluation exposed a real runtime `OutOfMemoryError` while repeatedly exercising offline routes. Android reported a roughly 192 MB heap growth limit and the failing allocation happened while `RanchiOfflineRouter.route()` was growing a `HashMap` during A* search.

The failure was not treated as a reason to increase the Android heap. It exposed that the original in-memory representation was too object-heavy for repeated routing on a constrained device process.

## Previous runtime shape

The original graph/search path used JVM collections and per-entry Kotlin/JVM objects:

- `HashMap<Int, RoutingNode>` for graph nodes;
- `HashMap<Int, MutableList<RoutingEdge>>` for adjacency;
- one `RoutingEdge` object per edge;
- `MutableMap<Int, Double>` for A* best-known cost;
- `MutableMap<Int, PreviousStep>` for predecessor state;
- `PriorityQueue<SearchState>` with one object per queued state.

This representation is simple, but every boxed `Int`/`Double`, hash entry, list, edge object, predecessor object, and queued search object adds heap overhead beyond the raw graph values.

## Current runtime shape

`RanchiOfflineRouter` now converts the TSV graph into dense node indexes once and keeps the resident graph in primitive arrays:

```text
node index: 0 .. N-1

latitudes             DoubleArray
longitudes            DoubleArray
edgeOffsets            IntArray       (CSR adjacency offsets)
edgeTargets            IntArray
edgeDistanceMeters     FloatArray
edgeDurationSeconds    FloatArray
```

Adjacency uses a compressed-sparse-row style layout. For node `i`, outgoing edges are stored from `edgeOffsets[i]` until `edgeOffsets[i + 1]`.

The temporary OSM/node IDs from the asset are converted to dense indexes only while loading. A primitive open-addressed int-to-int table is used for that conversion instead of a boxed `HashMap<Int, Int>`.

## A* search memory

Per-route A* state is also primitive:

```text
bestSeconds               DoubleArray
previousNode              IntArray
previousDistanceMeters    FloatArray
previousDurationSeconds   FloatArray
```

The open set is a custom primitive binary min-heap containing parallel arrays for node id, elapsed time, and estimated total time. It replaces `PriorityQueue<SearchState>` so normal search expansion does not allocate one JVM object per queue entry.

The routing algorithm itself is unchanged conceptually:

`nearest-road snap -> A* -> predecessor reconstruction -> route geometry + distance + duration`

The heuristic remains straight-line distance divided by a 130 km/h upper-bound speed, so it remains admissible for the existing car-routing model.

## What did not change

- Routing remains fully offline.
- The same generated Ranchi routing asset is used.
- No live traffic or turn-by-turn instructions are introduced.
- Route distance, duration, and geometry are still derived from the real bundled road graph.
- Missing/out-of-range routes still return `Unavailable`; Johar must never fabricate a route.
- The 1000-case evaluation matrix is not changed to accommodate the fix.

## Validation required

This commit changes runtime memory representation and therefore must be validated on the Android target before claiming success.

Minimum checks:

1. Build `:app:assembleDebug` and `:app:assembleDebugAndroidTest`.
2. Run several known route queries and compare that route cards still render plausible distance/duration/geometry.
3. Run the route-heavy evaluation shard(s) that previously triggered OOM.
4. Run the frozen 1000-case suite only after the route-heavy stress run survives.
5. Record any remaining OOM with the failing case ID and stack trace; do not increase heap as the first response.

## Follow-up optimization, not part of this fix

`nearestNode()` still performs an O(N) scan over graph nodes for both route endpoints. That is CPU work rather than the identified heap failure. A compact spatial grid/index can replace the scan later if route latency becomes a measured problem. Keep that separate from this memory fix so its effect is measurable.
