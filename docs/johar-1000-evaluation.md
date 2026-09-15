# Johar 1000-case evaluation baseline

## Purpose

Johar uses a frozen 1000-case end-to-end matrix to measure Ranchi V1 before and after product changes. The benchmark is a reproducible engineering baseline: capture failures, group them by behavior, fix category-level problems, then re-run the exact same IDs to measure movement.

## Coverage

| Family | Cases |
| --- | ---: |
| Knowledge / culture | 100 |
| Place lookup | 100 |
| Nearby discovery | 100 |
| Utilities / services | 100 |
| Routing | 150 |
| Comparison | 80 |
| Itinerary | 100 |
| Live/current guardrails | 100 |
| Typo / ambiguity | 80 |
| Negative / adversarial | 60 |
| Stateful conversation | 30 |
| **Total** | **1000** |

The first 200 IDs preserve the historical 200-command matrix. IDs 201-1000 extend coverage. Baseline V1 IDs are frozen; future cases should be appended rather than reshuffling these IDs.

## Behavior contracts

Each generated TSV row contains:

`id -> family -> query/turns -> allowed result types -> behavior contract -> state group`

Representative contracts:

- `grounded_or_safe_fallback`
- `resolve_real_place_or_data_gap`
- `explicit_origin_no_gps_inference`
- `real_service_only`
- `real_route_or_safe_gap`
- `two_real_places_no_fake_winner`
- `explicit_real_stops_preserve_order`
- `must_mark_live_unconfirmed`
- `conservative_resolution_no_unsafe_guess`
- `must_not_fabricate`

Automated classification is triage, not a substitute for reviewing visible answers.

## Runtime harness model

The host runner executes 50-case process shards.

Within each shard:

`fresh app process -> one JoharActivity -> reset conversation -> case -> capture -> reset -> next case`

Independent cases reset only chat/session state. Room, repositories, spatial lookup and the routing graph remain warm. Stateful cases reset once before the scenario and preserve all `|||` turns inside that scenario. The process is force-stopped after each shard to bound long-run heap/native accumulation.

The harness inspects only the latest Johar answer subtree so earlier cards cannot determine the final result type.

## Baseline V1

The first valid complete 1000-row run after routing-memory hardening produced:

| Classification | Count |
| --- | ---: |
| PASS | 508 |
| DATA_GAP | 399 |
| PARSER_GAP | 73 |
| SAFETY_GROUNDING_FAIL | 19 |
| HARNESS_ERROR | 1 |
| **Total** | **1000** |

Important observations:

- The full run completed without the previous routing OOM after the routing graph/A* memory redesign.
- Routing returned PASS for 100/150 route cases in this baseline.
- The 19 safety failures were concentrated in `current status` / `live status` wording that bypassed the existing live-data vocabulary and fell through to ordinary grounded knowledge.
- Typo/ambiguity remained a major parser weakness.
- Nearby and utility failures are dominated by offline data coverage and should remain distinct from parsing failures.
- Case 983 (`Tagore Hill se Ranchi Junction ka route batao|||reverse route?`) produced the single harness timeout and must be investigated separately from product-quality counts.

## First post-baseline reliability changes

The first category-level fix targets safety and typo tolerance without changing the benchmark IDs.

### Live/current safety

Live-data guarding now runs before route/comparison/itinerary parsing. The guarded vocabulary includes existing time/availability words plus `current`, `live`, `status`, `crowded`, `working`, and schedule terms. Any such query stays explicitly `NOT_CONFIRMED`; offline knowledge must never be presented as evidence of current status.

### Conservative typo tolerance

Place matching keeps exact matching first, then allows bounded edit-distance matching between meaningful query tokens and tokens in real resolved place names. Common generic place-type variants such as station/mandir/falls/dam/ground are normalized before resolution. This is deliberately conservative: typo tolerance may select only from real spatial candidates returned by the offline resolver; it does not synthesize a place name.

Unit tests cover `current status`, `live status`, typo-tolerant landmark matching, and a common station typo.

## Baseline workflow

1. Keep the 1000 IDs unchanged.
2. Make one coherent reliability change at a time.
3. Run focused unit/instrumentation checks first.
4. Re-run the same full 1000 cases only after the focused checks are green.
5. Compare classification deltas against Baseline V1.
6. Manually inspect all safety failures and suspicious automated PASS rows.
7. Keep `DATA_GAP` separate from parser/runtime regressions.

The useful portfolio story is not the raw number of tests. It is the measurable sequence:

`frozen benchmark -> failure cluster -> architecture/product fix -> same benchmark -> measured delta`

## Interpretation

A green Android instrumentation report means the harness executed successfully; it does not mean product quality is 100%. Product quality is determined from structured result classifications plus manual review of representative responses.
