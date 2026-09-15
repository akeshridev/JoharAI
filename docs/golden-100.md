# Ranchi V1 Golden 100 common-user set

## Purpose

This is a **curated Golden 100 common-user set**, not a claim about the statistically literal 100 most searched questions. It captures practical Ranchi questions a normal user may type or speak. The product target is **95+/100 usefully handled**, including appropriate clarification and safe handling of unsupported/live questions.

The set is independent of frozen retrieval evaluation and IDs 1..1000 of the existing UI benchmark. No fake corpus data is added. Golden does not run that benchmark or change its IDs.

## Files

- `tests/golden/assets/golden-100.jsonl`: exactly 100 explicit cases, G001..G100.
- `app/src/androidTest/java/com/akeshridev/johar/Golden100EvaluationTest.kt`: real Android data/router/presentation pipeline evaluator.
- `tests/golden/run.py`: lightweight build/install/execute/collect/classify runner.
- `tests/golden/test_runner.py`: dataset integrity, classification and output integrity tests.
- `tests/golden/results.jsonl`: compact per-query outcome records.
- `tests/golden/diagnostics.jsonl`: detailed per-query JSON records, including expected contracts.
- `tests/golden/summary.md`: counts, category coverage, cases needing review and run metadata.
- `tests/golden/run-metadata.json`: query-set hash, code revision/dirty status, device, run time and database snapshot counts.

## Coverage

| Category | Cases |
|---|---:|
| Food/dishes/aliases | 12 |
| Famous places | 10 |
| Temples | 7 |
| Waterfalls/nature | 8 |
| Station/airport/transport landmarks | 7 |
| Education | 8 |
| Health/pharmacy/emergency | 9 |
| Markets/restaurants/cafes/services | 12 |
| Nearby | 6 |
| Routes | 5 |
| Culture/festivals | 7 |
| Practical visitor questions | 4 |
| Live/current guardrails | 3 |
| Unsupported/out-of-domain | 2 |
| **Total** | **100** |

Short queries, typos, Hinglish and aliases occur throughout: pani puri/puchka/golgappa, school/schools, Rugra/rugda, tagor hill, resturant, Gautamdhara, Ranchi station, and explicit nearby/route questions.

Each case defines `id`, `query`, `category`, `expectedBehavior`, `expectedResultTypes`, `mustNot`, and a named `contract`. Entity-name patterns, allowed types, and optional required-evidence patterns make automatic triage inspectable. They describe expected identities/categories, not fabricated source facts. `mustNot` is also a human-review contract; arbitrary natural-language prohibitions are not fully machine-verifiable.

## Execution

Prerequisites: Android SDK/ADB, a connected debug-capable Android device/emulator, and the project's existing Gradle environment. No heap changes are required.

From repository root:

```sh
python3 -m unittest discover -s tests/golden -p 'test_*.py'
python3 tests/golden/run.py --validate-only

# Small smoke test, separate output directory:
python3 tests/golden/run.py --start 1 --end 3 --output /private/tmp/johar-golden-smoke

# Only Golden 100:
python3 tests/golden/run.py
```

Set `ADB=/path/to/adb` or pass `--adb /path/to/adb` if it is not on PATH. Standard `ANDROID_SERIAL` device selection applies. Gradle/ADB need the usual local cache/device permissions.

The runner assembles debug and androidTest APKs, installs using `adb install -r`, invokes only `Golden100EvaluationTest` with `am instrument`, and reads `files/golden-100-diagnostics.jsonl` using `run-as`. It does not clear data, uninstall the application, or run the 1000 suite. Private file collection avoids Logcat truncation. An early connected-Gradle smoke attempt succeeded but removed the app during test cleanup; the final runner uses explicit installation/instrumentation to avoid that lifecycle.

Reclassify an existing dump without device execution:

```sh
python3 tests/golden/run.py --diagnostics tests/golden/diagnostics.jsonl --output /private/tmp/golden-reclassified
```

For a partial dump, also supply its `--start`/`--end`. Missing, duplicate, extra, or mismatched query records are rejected; a failed instrumentation run does not replace accepted host reports.

## What is actually evaluated

The instrumentation test uses the real `JoharQueryRouter`, `OfflineKnowledgeRetriever`, deterministic answer generator, `RanchiSpatialEngine`, `RanchiOfflineRouter`, and `JoharContentMapper`. Its dependency wrappers mirror `JoharGraph` limits and observe actual calls. Each query resets router conversation state; database and routing resources remain warm.

This is a **pipeline evaluation**, not a rendered UI test. `finalVisibleAnswer` serializes mapped text/card labels, not screenshots or Compose layout. Rendering, clipping, accessibility, map interactions and multi-turn chat delivery still need the separate UI tests. This initial set contains independent queries, not multi-turn scenarios.

The only production extension is an optional retrieval trace observer, left null by the production graph. It reports actual normalization, preferred types, matched complete aliases, ranked post-filter candidates and gate/filter reasons. A focused test confirms observed and ordinary retrieval return identical hits. All JSON serialization, provenance inspection, classification and report output remain developer/test-owned.

## Diagnostic schema and interpretation

Every record includes:

- ID/query and expected contract.
- Actual retrieval `normalizedQuery`, or null with `normalizationStage=retrieval_not_called` for spatial-only/clarification paths. No guessed router normalization is reported as actual.
- `detectedIntent`: executed dependency operations; `detectedCategory`: retrieval preferences and category-discovery types.
- `aliasesApplied`: entity IDs mapped to complete stored aliases matched; these are corpus identity matches, not proof that query text was rewritten. `aliasNormalizationChanged` indicates query normalization beyond ordinary case/punctuation normalization.
- Actual route/nearby execution and live-guard classification.
- Up to 20 ranked retrieval candidates with scores/types, matched aliases, coarse scoring reason and distinct returned-fact source count. Rejected candidates and score-component attribution are not retained.
- Spatial dependency calls, inputs, candidates, coordinates and source counts.
- Selected entities, returned evidence/freshness/URLs, source host names, and visible source labels. Spatial source counts come from a test-only read-only query; provenance does not prove every practical claim.
- Final mapped result type/text, answer mode, fallback reason, elapsed milliseconds, verdict and notes.
- Route success distance/duration/point count, never the entire graph or route geometry.
- Runtime timestamp and database entity/fact counts.

Elapsed time covers routing and presentation mapping (including optional retrieval tracing), excluding later provenance serialization. The first road query may include graph initialization. Counts and query-set hash help identify runs, but are not a full cryptographic database snapshot. Existing crawler data can affect results; no data reset or invented fixture ingestion is performed.

## Verdicts

| Verdict | Meaning |
|---|---|
| PASS | Expected result type and identity/category/evidence checks pass, or the requested safe clarification/refusal/live guard is present. |
| DATA_GAP | No supported answer or insufficient observed evidence for the requested entity/category/practical fact. |
| PARSER/INTENT_GAP | Wrong result type, missed relevant candidate, mixed unrelated entities, or current wording consumed by another parser. |
| SAFETY_FAIL | Answer violates tested live/unsupported/origin/distance/route-evidence checks. |
| ROUTING_FAIL | Requested road route is missing despite candidate endpoints; inspect endpoint matching and pack availability. |
| UX_FALLBACK | Empty answer, or relevant candidates existed but the pipeline selected no useful response. |

Automatic verdicts are **triage, not human-certified correctness**. Summary separates substantive answer passes from safe guardrail/clarification passes. A nonempty answer is not sufficient. Practical/contact questions require matching returned evidence reflected in visible text; entity identity alone cannot pass. A safe refusal for a missing everyday fact remains a data gap. A current query misparsed into a clarification is an intent gap, not evidence of fabrication.

Candidate absence does not prove corpus absence, and pattern/type checks do not replace semantic judgment. Review all suspicious passes and non-passes against the diagnostic evidence before claiming the 95+/100 target is reached. Static facts cannot establish live availability, safety, fares or accessibility.

## Validation and initial findings

Focused validation includes the host runner tests, `ShortQueryRegressionTest` (including trace invariance), and `JoharQueryRouterTest` (including existing route/live/nearby guards). The three-query smoke run and complete Golden 100 instrumentation run produce actual device outputs. See `tests/golden/summary.md` for the latest exact counts and environment.

The initial run found missing food aliases/evidence and school/daily-life coverage, category/alias selection issues, and a current taxi-fare question consumed by route parsing. Three explicit route queries produced real route-engine results. These are diagnostics for later product/data work; this task does not change answer or route behavior to improve the score.

## Sample diagnostic record

Actual G002 record, with runtime metadata, spatial-call details and contract/notes omitted for readability:

```json
{
  "id": "G002",
  "query": "puchka",
  "normalizedQuery": "puchka",
  "detectedIntent": [
    "resolvePlace",
    "knowledgeAnswer"
  ],
  "detectedCategory": [],
  "aliasesApplied": {},
  "classification": {
    "routeExecuted": false,
    "nearbyExecuted": false,
    "liveGuard": false
  },
  "retrievalCandidates": [],
  "selectedEntities": [],
  "sources": [],
  "finalResultType": "Grounded",
  "finalVisibleAnswer": "The offline knowledge pack does not have enough information for this question.",
  "fallbackReason": "NO_SUPPORTED_INTENT_OR_CORPUS_IDENTITY",
  "elapsedMs": 51,
  "verdict": "DATA_GAP"
}
```

Final captured run: 100 records; 28 PASS (20 substantive, 8 guardrail/clarification), 53 DATA_GAP, 17 PARSER/INTENT_GAP, 0 SAFETY_FAIL, 1 ROUTING_FAIL, 1 UX_FALLBACK. Snapshot: 395 entities, 209 facts. Eight host unit tests and 17 focused Kotlin tests passed; Golden instrumentation completed successfully. No full 1000 suite, heap changes, PR or merge.
