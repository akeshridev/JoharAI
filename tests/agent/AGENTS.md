# Johar temporary agent-test workspace

This directory is intentionally temporary. It exists to exercise the installed Johar debug app, record evaluation results, and support measured product improvements.

## Canonical baseline rules

1. Preserve `tests/agent/assets/johar-200-commands.tsv` and `docs/johar-200-command-validation.md` as the historical 200-case baseline.
2. Generate the 1000-case baseline with `python3 tests/agent/generate_1000_cases.py`.
3. IDs `1..200` must remain byte-for-byte compatible in query meaning with the historical matrix.
4. IDs `201..1000` are deterministic and must not be reshuffled after Baseline V1. Add future evaluation cases above ID `1000`.
5. Drive the real installed app for end-to-end baseline measurement. Do not call `JoharQueryRouter` directly for the 1000-case UI run.
6. Prefer stable Compose semantics/test tags over screen coordinates.
7. For every case, record ID, family, query/turn sequence, observed result type, visible final response, elapsed time, behavior contract, and classification.
8. Allowed classifications are `PASS`, `DATA_GAP`, `PARSER_GAP`, `UI_GAP`, `SAFETY_GROUNDING_FAIL`, `CRASH_ANR`, `HARNESS_ERROR`.
9. Never mark a missing offline entity/fact as a parser failure when routing was correct.
10. Never mark fabricated live data, route, price, rating, safety, availability, guarantee, or place as PASS.
11. Stateful cases use `|||` between turns and must execute all their turns in one fresh activity. Different cases must not share conversation state.
12. Do not change production behavior before the first complete 1000-row Baseline V1 is captured and analyzed.
13. Automated classification is triage. Review non-pass cases and suspicious PASS cases before changing product code.

## 1000-case coverage

- KNOWLEDGE: 100
- PLACE: 100
- NEARBY: 100
- UTILITY: 100
- ROUTE: 150
- COMPARISON: 80
- ITINERARY: 100
- LIVE_GUARDRAIL: 100
- TYPO_AMBIGUITY: 80
- NEGATIVE: 60
- STATEFUL: 30

This distribution tests capability breadth, language variation, typo tolerance, safety and conversation context rather than producing 1000 random paraphrases.

## Automated runner

From repository root with one connected device/emulator:

```bash
bash tests/agent/run.sh
```

The runner regenerates the matrix before Gradle packages androidTest assets, runs only `Johar1000CommandUiTest`, captures structured `JoharAgent` logcat rows, verifies exactly 1000 exported records, stores them in `tests/agent/results.jsonl`, and prints the summary.

The run may take close to an hour. Do not interrupt it because a particular answer looks wrong; product-quality failures belong in the baseline result set.

## Runtime behavior

For ordinary cases:

`case -> fresh JoharActivity -> input -> send -> wait for answer count to increase + thinking to disappear -> inspect latest answer -> classify -> emit JSON row`

For stateful cases:

`case -> fresh JoharActivity -> turn 1 -> wait -> turn 2 -> wait -> inspect final answer -> classify -> emit one JSON row`

The wait condition deliberately checks that the rendered answer count increased. This prevents the harness from mistaking the previous answer for completion of a new turn.

## Stable UI contract

The harness targets these stable IDs:

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

The chat root enables `testTagsAsResourceId`. Do not use pixel coordinates when a semantic ID exists.

## Result format

One JSON object per evaluation case, for example:

```json
{"id":521,"query":"Tagore Hill se Pahari Mandir kaise jaye?","resultType":"ROUTE","response":"...","status":"PASS","elapsedMs":6200,"notes":"expected=ROUTE; contract=real_route_or_safe_gap; allowed=ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO"}
```

`tests/agent/results.jsonl` is generated scratch output. Preserve the first complete result externally or under an explicitly named baseline artifact before beginning product fixes.
