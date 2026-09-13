# AGENTS.md — johar-domain

Read the root `AGENTS.md` first.

## Scope
Pure Kotlin/JVM model module for Johar's knowledge system. This is the primary active module while MODEL ONLY is in effect.

## Hard boundaries
- No Android imports.
- No Compose/UI.
- No Jsoup, HTTP, Room, WorkManager, model runtime, or serialization implementation.
- No concrete data-source implementations.
- Keep models independent from how data is fetched, extracted, stored, or displayed.

## Current goal
Keep the knowledge model basic, clear, and scalable. Dassam Falls is the first dataset, not a special-case schema.

Use five core concepts:
1. Entity — a real thing such as Dassam Falls, a river, village, hospital, restaurant, or station.
2. Fact — one source-backed attribute about an entity, grouped into one of the existing knowledge domains.
3. Relationship — a typed connection between two entities.
4. Source — where a fact/media item came from, including provenance/evidence.
5. Media — image/video references related to an entity.

Do not introduce additional abstraction layers until real data requires them.

## Model rules
- Every fact keeps provenance/evidence.
- Conflicting facts from different sources may coexist.
- Unknown is not false, zero, or empty text.
- Facts use an extensible field/key plus typed value; do not create a giant place-specific data class.
- If a value is another real-world thing, prefer an entity relationship when useful.
- Media stores references and metadata, not binary payloads.
- Preserve media source, attribution/license metadata when available.
- Do not hardcode place-specific source URLs or external IDs in the domain model.
- Persistence must map to the model later; Room does not drive the model.
- Add fields/types only when actual data requires them.

## Frozen capabilities
Existing crawl scheduling types may remain for the current harness, but do not expand runtime ingestion contracts while MODEL ONLY is active unless explicitly requested.

## Working rule
Prefer the simplest model that preserves data correctly and can grow without migration-heavy redesign.
