# Golden 100 common-user set

Read root `AGENTS.md`. This directory is separate from `tests/agent` and the frozen retrieval/UI benchmarks.

- `assets/golden-100.jsonl` is a curated Ranchi V1 common-user set, not a statistical search ranking. Preserve G001..G100 meanings; document any deliberate contract revision.
- Target 95+/100 usefully handled. Do not optimize by relaxing contracts, suppressing gaps, adding fake corpus data, or changing frozen 1000 IDs.
- Each case defines query, category, expected behavior/result types, must-not behavior and inspectable triage checks.
- Use `python3 run.py`; it runs only `Golden100EvaluationTest`. Never call the full 1000 runner as part of Golden validation.
- Prefer a small smoke range with a separate `--output` directory before the full Golden run.
- Diagnostics are developer-only. Actual pipeline calls and mapped answer text are observed in androidTest; no diagnostic fields appear in chat.
- `PASS` is automated contract triage, not human-certified correctness. Inspect substantive passes, safety, practical/contact evidence and category mismatches manually. A nonempty answer or static identity is not sufficient evidence of usefulness.
- Missing observed evidence stays DATA_GAP unless diagnostics support a parser/intent or routing cause. An unhelpful clarification is not automatically fabricated data / SAFETY_FAIL.
- Runner must reject incomplete, duplicate or mismatched records. Do not publish partial results as a 100-case baseline.
- Run uses installed working Room data and normal seed/booster initialization. Use APK replacement (`adb install -r`), never clear/uninstall application data. Record snapshot counts and environment in results.
- See `docs/golden-100.md` for contracts, limitations and execution.
