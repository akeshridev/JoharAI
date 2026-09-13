# AGENTS.md — johar-data

Read root `AGENTS.md` first.

## Scope
Android data/runtime module. Depends on `johar-domain` and owns all source crawling and persistence implementation.

## Owns
- `source/` — source registry/config for crawl targets.
- `remote/` — HTML fetching.
- `parser/` — HTML-to-clean-text transformation.
- `local/` — Room database, DAO, entities.
- `crawl/` — crawl orchestration and Logcat dump.
- `work/` — WorkManager worker and scheduler implementation.

## Rules
- Never put Compose/ViewModel/UI code here.
- Never move Android/network/database types into `johar-domain`.
- Keep fetching, cleaning, persistence, scheduling, and logging as separate responsibilities.
- Room rows represent fetched source snapshots, not canonical/source facts.
- The worker may orchestrate implementations in this module; presentation never imports them directly.
- Preserve coroutine/WorkManager cancellation semantics if async code is introduced later.

## Current thin slice
`CrawlTarget.DASSAM_FALLS` -> Ranchi District source -> Jsoup -> clean text -> Room -> Logcat.

Do not add LLM extraction, vector search, canonical merging, or additional sources until explicitly requested.
