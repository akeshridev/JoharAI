# Johar temporary agent-test workspace

This directory is intentionally temporary. It exists only to let a UI/coding agent exercise the installed Johar debug app, record each validation result, and hand the report back for analysis. Delete `tests/agent/` after the 200-command validation cycle is complete.

## Agent job

1. Treat `docs/johar-200-command-validation.md` as the canonical list of test IDs and queries.
2. Drive the real installed app. Do not call `JoharQueryRouter` directly for the end-to-end run.
3. Prefer stable Compose semantics/test tags over screen coordinates.
4. For every case, record the command ID, query, observed result type, visible response text, elapsed time, and classification.
5. Allowed classifications are: `PASS`, `DATA_GAP`, `PARSER_GAP`, `UI_GAP`, `SAFETY_GROUNDING_FAIL`, `CRASH_ANR`, `HARNESS_ERROR`.
6. Never mark a missing offline entity/fact as a parser failure when the intent was routed correctly.
7. Never mark fabricated live data, route, price, rating, safety, availability, or place as PASS.
8. Stateful cases must note any prior command/follow-up needed to reproduce them.
9. Append machine-readable records to `tests/agent/results.jsonl`. Do not rewrite the canonical 200-command document during execution.
10. Keep screenshots/log references only when they help diagnose a failure.

## Stable UI contract

The automation harness should target stable IDs such as:

- `johar_chat_input`
- `johar_send_button`
- `johar_thinking`
- `johar_latest_answer`
- `johar_place_card`
- `johar_route_card`
- `johar_map_card`
- `johar_info_card`
- `johar_itinerary_card`
- `johar_comparison_card`
- `johar_clarification`

Do not depend on pixel coordinates unless a semantic ID is genuinely unavailable; report that as a harness gap so the app can expose a stable ID instead.

## Result format

Write one JSON object per line to `results.jsonl`:

```json
{"id":81,"query":"Tagore Hill se Ranchi railway station kaise jaye?","resultType":"ROUTE","response":"Tagore Hill → Ranchi Junction railway station","status":"PASS","elapsedMs":1200,"notes":"route card + offline map rendered"}
```

The results file is a temporary QA artifact and may be deleted after the validation report is reviewed.
