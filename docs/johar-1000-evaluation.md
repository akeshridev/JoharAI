# Johar 1000-case evaluation baseline

## Purpose

Before changing production behavior, Johar will be measured against a frozen 1000-case end-to-end evaluation matrix. The goal is to establish a reproducible Baseline V1, classify failure modes, then improve one category at a time without losing visibility into regressions.

This is an evaluation change only. It does not change routing, retrieval, spatial search, UI behavior, grounding policy, or product answers.

## Why 1000 cases

The original 200-command matrix proved the instrumentation path and exposed real gaps, especially typo/alias resolution, data coverage, live-data guardrails, and fallback quality. A 1000-case matrix gives enough variation to distinguish isolated phrase failures from category-level weaknesses.

The matrix is intentionally structured rather than being 1000 random prompts:

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

The first 200 IDs are preserved from the original matrix. IDs 201-1000 extend coverage. Once Baseline V1 is captured, these IDs are immutable; future cases should be appended above 1000.

## Behavior contracts

Each generated TSV row contains:

`id -> family -> query/turns -> allowed result types -> behavior contract -> state group`

Examples of contracts:

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

These contracts are more useful than a raw expected string because many valid responses depend on current offline data coverage while still having strict safety and routing requirements.

## Stateful cases

IDs 971-1000 contain two-turn conversations. `|||` separates turns in the generated asset. Both turns execute in the same fresh activity, while no state leaks between different test cases.

Examples include:

- place -> nearby follow-up
- place -> route follow-up
- comparison -> follow-up comparison
- itinerary -> add/reorder stop
- live question -> follow-up live question
- clarification -> supplied location
- invalid entity -> route request, which must not escalate into fabrication
- unknown query -> recovery with a valid place query

## Harness flow

`tests/agent/generate_1000_cases.py` generates `tests/agent/assets/johar-1000-cases.tsv` deterministically from the frozen 200 cases plus the 800-case expansion.

`Johar1000CommandUiTest` then:

1. launches a fresh `JoharActivity` per evaluation case;
2. enters every turn through the real Compose chat input;
3. taps the real send button;
4. waits until the rendered answer count increases and the thinking state disappears;
5. inspects the final answer semantics and visible text;
6. performs a first-pass classification;
7. emits one JSON result row through the stable `JoharAgent` log tag.

`tests/agent/run.sh` regenerates the matrix, runs only the 1000-case class, captures logcat, verifies that exactly 1000 rows were exported, then writes `tests/agent/results.jsonl`.

## Baseline workflow

1. Do not change production behavior.
2. Generate and compile the 1000-case harness.
3. Run the complete suite once without interruption.
4. Preserve the first complete result as **Baseline V1**.
5. Analyze outcomes by family and classification.
6. Manually review all safety failures and suspicious automated PASS results.
7. Prioritize category-level fixes rather than phrase-specific patches.
8. Re-run the same stable matrix after each meaningful product change.

Track movement such as:

`PASS 61% -> 74%`

`PARSER_GAP 18% -> 6%`

`SAFETY_GROUNDING_FAIL 4% -> 0.5%`

A stable `DATA_GAP` count may be acceptable until the offline corpus is deliberately expanded.

## Interpretation

A green Android instrumentation report means the harness executed; it does not mean product quality is 100%. Product quality comes from the JSONL classifications plus manual review of the responses.

The first 1000-case run is therefore a measurement artifact, not a release gate by itself.
