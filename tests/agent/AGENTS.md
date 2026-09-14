# Johar temporary agent-test workspace

This directory is intentionally temporary. It exists to exercise the installed Johar debug app, record evaluation results, and support measured product improvements.

## Canonical baseline rules

1. Preserve `tests/agent/assets/johar-200-commands.tsv` and `docs/johar-200-command-validation.md` as the historical 200-case baseline.
2. Generate the 1000-case baseline with `python3 tests/agent/generate_1000_cases.py`.
3. IDs `1..200` must preserve the historical query meaning; IDs `201..1000` must not be reshuffled after Baseline V1.
4. Drive the real installed app. Do not replace the 1000-case UI baseline with direct router calls.
5. Prefer stable Compose semantics/test tags over screen coordinates.
6. Record ID, family, query/turn sequence, final result type, visible response, elapsed time, behavior contract and classification.
7. Allowed classifications: `PASS`, `DATA_GAP`, `PARSER_GAP`, `UI_GAP`, `SAFETY_GROUNDING_FAIL`, `CRASH_ANR`, `HARNESS_ERROR`.
8. A missing offline entity/fact is not automatically a parser failure.
9. Fabricated live data, route, price, rating, safety, availability, guarantee or place must never be PASS.
10. Do not change product behavior before the first complete 1000-row Baseline V1 is captured and analyzed.
11. Automated classification is triage. Review non-pass and suspicious PASS cases manually.

## Warm-activity shard model

The host runner uses 50-case process shards.

- Start a fresh app process for the shard.
- Launch one `JoharActivity` for the shard.
- Reuse that same Activity for all 50 cases.
- Before each independent case, call the evaluation reset hook to clear only conversation/session state.
- Do not recreate Room, repositories, routing graph, offline data or the Activity between independent cases.
- For stateful cases, reset once before the scenario and keep all `|||` turns together without resetting between turns.
- Force-stop the app process only after the shard finishes, then start the next shard.

The evaluation reset clears messages, map targets and pending router clarification/category state. It is not a production user feature and must not alter normal query behavior.

## Runtime behavior

Independent cases:

`one warm JoharActivity -> reset -> query -> wait -> inspect latest answer -> classify -> emit -> reset -> next query`

Stateful cases:

`reset -> turn 1 -> wait -> turn 2 -> ... -> inspect final latest answer -> classify -> emit`

Result-type detection must be scoped to `johar_latest_answer`, not the entire chat tree, so an earlier turn cannot determine the final type of a stateful case.

## Result integrity

`tests/agent/run.sh` must:

- regenerate the deterministic matrix before packaging androidTest assets;
- run only `Johar1000CommandUiTest` for each shard;
- capture structured `JoharAgent` rows;
- require exactly 50 rows for a normal shard;
- require exactly 1000 rows overall;
- write the accepted baseline to `tests/agent/results.jsonl`.

If a shard crashes or OOMs, do not silently continue. Treat that as harness/runtime evidence and diagnose it before accepting a baseline.

## Stable UI contract

The harness may depend on:

- `johar_root`
- `johar_chat_list`
- `johar_chat_input`
- `johar_send_button`
- `johar_thinking`
- `johar_answer`
- `johar_latest_answer`
- `johar_text_answer`
- `johar_grounded_answer`
- `johar_place_card`
- `johar_utility_card`
- `johar_route_card`
- `johar_map_card`
- `johar_info_card`
- `johar_itinerary_card`
- `johar_comparison_card`
- `johar_clarification`

`testTagsAsResourceId` remains enabled. Avoid pixel-coordinate automation when a semantic contract exists.
