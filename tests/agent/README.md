# Johar agent validation workspace

Temporary workspace for automated end-to-end validation of the installed Johar Android app.

Canonical behavior inventory: `docs/johar-200-command-validation.md`.
Executable command input: `tests/agent/assets/johar-200-commands.tsv`.

## One-command flow

Connect one Android device/emulator, then run:

```bash
bash tests/agent/run.sh
```

The script runs `:app:connectedDebugAndroidTest`, drives the real `JoharActivity` through Compose semantics, captures one structured `JoharAgent` log row per command, writes the 200 rows to `tests/agent/results.jsonl`, validates the row count, and prints a status summary.

The runner expands the device logcat buffer before the suite starts because the full 200-case run takes about 11 minutes; this prevents early result rows from being evicted before export. The instrumentation test does not clear or depend on an app-private results file.

Runtime flow:

`test id -> fresh JoharActivity -> johar_chat_input -> type query -> johar_send_button -> wait -> inspect johar_latest_answer + result tag -> classify -> emit JSON log row -> host exports results.jsonl`

Each parameterized test case launches a fresh activity so conversation state from an earlier command does not intentionally influence the next command. Stateful conversation behavior should be tested separately when required.

## Stable UI contract

Production UI exposes automation semantics without changing product behavior:

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

`testTagsAsResourceId` is enabled at the chat root so external UI agents may also use these IDs. Do not automate with pixel coordinates when a semantic ID exists.

## Result file

`results.jsonl` is scratch output. One line represents one command and contains:

- id
- query
- detected result type
- visible response text
- automated classification
- elapsed milliseconds
- expected-family note

Automated classification is triage, not the final product verdict. Review non-pass cases before changing product code, especially to distinguish a missing-data result from a parser defect.

This folder is intentionally temporary and may be deleted after the 200-command validation/fix cycle is complete.
