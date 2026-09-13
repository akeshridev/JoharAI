# AGENTS.md — johar-data

Read root `AGENTS.md` first.

## Scope
Android data/runtime module. Depends on `johar-domain` and owns all source crawling, source-aware extraction implementations, and persistence implementation.

## Owns
- `source/` — source registry/config for crawl targets, including canonical source URL and machine-fetch URL.
- `remote/` — fetching raw HTML/JSON payloads.
- `parser/` — HTML-to-clean-text and structured-source parsing.
- `extractor/` — concrete `SourceFactExtractor` implementations/adapters.
- `local/` — Room database, DAOs, source snapshot entities, SourceFact persistence entities/mappers.
- `crawl/` — crawl orchestration and Logcat dump.
- `work/` — WorkManager worker and scheduler implementation.

## Rules
- Never put Compose/ViewModel/UI code here.
- Never move Android/network/database types into `johar-domain`.
- Keep source registry, fetching, parsing, extraction, persistence, scheduling, and logging as separate responsibilities.
- Use structured APIs for Wikidata, OpenStreetMap, and Wikimedia Commons; do not scrape their rendered pages.
- Persist crawled source snapshots separately from extracted SourceFact rows.
- Every SourceFact must retain canonical source URL, publisher, retrieval time, and exact supporting evidence.
- Extract only explicitly supported facts; missing data produces no invented fact.
- Conflicting facts from separate sources coexist. Do not reconcile them here.
- A failure from one source must not discard successful results from other sources.
- The worker may orchestrate implementations in this module; presentation never imports them directly.
- Preserve coroutine/WorkManager cancellation semantics if async code is introduced later.

## Current source set
- Ranchi District Government Dassam Falls HTML.
- Jharkhand Tourism Dassam Falls HTML.
- Jharkhand state tourism HTML.
- Wikidata Q37918 JSON.
- OpenStreetMap node 6189619382 JSON.
- Wikimedia Commons category API JSON.

## Current thin slice
`CrawlTarget.DASSAM_FALLS` -> all registered sources -> fetch -> source-aware extraction -> Room snapshots + SourceFact[] -> Logcat.

Canonical merging, vector search, and answer generation are still out of scope.
