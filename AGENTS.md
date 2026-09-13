# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering Jharkhand through trusted local knowledge. V0 is intentionally limited to Dassam Falls.

## Current focus — INGESTION PIPELINE
Development is focused on the knowledge/data pipeline, not product UI.

The only UI currently allowed is a tiny developer harness: one button that enqueues the Dassam crawl. Do not add navigation, product screens, presentation state, design work, or other user flows unless explicitly requested.

## Current V0 boundary
Johar should answer only questions that help a person decide whether to visit Dassam Falls, reach it, experience it, understand it, stay safe, and return.

Knowledge domains:
1. Tourism
2. Family & Accessibility
3. Safety & Emergency
4. History & Culture
5. Food
6. Travel & Logistics
7. Weather & Season Context
8. Facilities
9. Geography

## Current ingestion thin slice
Button -> domain use case -> WorkManager -> source catalog -> Jsoup fetch -> clean text -> Room -> Logcat dump.

The current source is the Ranchi District Government Dassam Falls page. Source-fact extraction/LLM comes after this crawl/store slice is proven.

## Module router

### `johar-domain`
Pure Kotlin/JVM model and contracts. Owns `SourceFact`, crawl targets, scheduler contracts, and use cases. No Android, UI, network, parser, database, or WorkManager code. Read `johar-domain/AGENTS.md` before changing it.

### `johar-data`
Android data/runtime module. Owns source registry, Jsoup fetching/cleaning, Room storage, WorkManager worker/scheduler, and Logcat dumping. Depends on `johar-domain`. Read `johar-data/AGENTS.md` before changing it.

### `app`
Thin developer harness and composition root. UI/ViewModels consume domain use cases only; imports from `johar-data` are restricted to `app/.../di`. Read `app/AGENTS.md` before changing it.

## Architecture rules
Follow the principles demonstrated in `akeshridev/AIFriendlyAppArchitecture`:
- Single responsibility.
- Strict one-way dependencies: `app -> johar-data -> johar-domain` (app may also consume domain contracts).
- Presentation must not reach into data implementations.
- Domain must remain framework-free.
- Data owns fetching/parsing/persistence/runtime details.
- Prefer small module-local changes so an AI agent does not need the whole repo in context.
- Never bypass a domain contract because a shortcut is faster.

## Data correctness
- Source facts are not the final canonical record.
- Preserve provenance/evidence when source-fact extraction is added.
- Never silently infer missing facts.
- Current Room row stores crawled source text, not canonical knowledge.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open sources only for the core dataset.
- No LLM/vector/canonical merge work until crawl/store is proven.

## Token discipline
- Use tokens economically.
- Read only files needed for the current task.
- Do not scan the whole repo unless necessary.
- Do not repeat context already present in AGENTS.md.
- Keep explanations short unless explicitly asked.
- Make only changes needed for the current request.
- Avoid speculative refactors or unrelated improvements.

## Collaboration style
Work in small increments and prefer a working thin slice before adding architecture layers.

After every meaningful architecture/focus decision, update the relevant `AGENTS.md` before implementation.
