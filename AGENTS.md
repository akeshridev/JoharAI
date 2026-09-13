# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering Jharkhand through trusted local knowledge. V0 is intentionally limited to Dassam Falls.

## Current focus — MODEL ONLY
Development is now 100% focused on the knowledge model and the shape of the packed local database.

Freeze runtime implementation work unless explicitly requested:
- No new UI/product flows.
- No crawler expansion.
- No WorkManager changes.
- No network/parser work.
- No LLM integration.
- No answer-generation work.

The existing crawl/Room thin slice may remain in the repo as a test harness, but do not extend it while MODEL ONLY is active.

## Current V0 boundary
Johar should model only knowledge that helps a person decide whether to visit Dassam Falls, reach it, experience it, understand it, stay safe, and return.

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

## Current model goal
Design a generic local knowledge database that can later be packed with source-backed data for any canonical entity, starting with Dassam Falls.

Keep the core model basic and scalable:
- Entity
- Fact
- Relationship
- Source
- Media

Do not collapse these into one giant Dassam record.

## Discovery direction
Entity-specific URLs/IDs should not be hardcoded into the knowledge model. Eventual discovery starts from an entity plus category/search context, and source adapters resolve dynamic IDs/URLs from open sources.

Search terms should be data-driven rather than hardcoded in crawler code.

Maintain a crawl keyword/category table containing terms the crawler can pick from. A crawl task uses entity + category + keyword context, for example:
- entity: Dassam Falls
- category: FOOD
- keyword: local food

Do not crawl a bare keyword globally without entity/location context.

## Self-expanding discovery
The keyword/category table is not static.

During periodic refresh/discovery:
- existing keywords drive source discovery;
- newly discovered useful entities, aliases, topics, and category terms may produce new keyword rows;
- new keywords are normalized/deduplicated before being added;
- each keyword should retain why/how it was discovered and its category/entity context;
- low-value or repeatedly unproductive keywords may later be deprioritized or disabled;
- the crawler periodically revisits both existing knowledge and newly discovered keywords.

This creates a controlled discovery loop:

Entity/category keywords -> crawl -> facts/relationships/media -> discover new terms/entities -> keyword table -> future crawl.

## Category completeness
For every canonical entity, the packed knowledge DB should be able to represent all discovered data across every applicable knowledge domain, not a fixed minimal subset.

The field model must remain extensible so newly discovered source-backed attributes can be added without redesigning the whole database.

## Media direction
Media references may be captured whenever available:
- direct/static image URLs
- Wikimedia Commons media URLs and thumbnails
- YouTube/video page URLs and preview/thumbnail URLs
- source-page image/video links

Store references and metadata by default, not binary image/video payloads. Preserve source/provenance and licensing/attribution when available.

## Local storage budget
Target up to approximately 1.5 GB total local storage when useful. This is a total storage budget, not a Room database target or APK-size target.

Room should primarily contain structured knowledge, source metadata, freshness state, keyword/category discovery state, and indexes. Large raw snapshots or future media caches should be bounded separately.

Periodic refresh should update/replace stale knowledge rather than grow append-only forever.

## Module router

### `johar-domain`
Primary active module. Pure Kotlin/JVM knowledge model and domain rules. Owns entity/fact/provenance/relationship/media/category/discovery concepts and storage-independent contracts. No Android, UI, network, parser, Room, WorkManager, or LLM implementation.

### `johar-data`
Currently frozen except when needed to validate the model against persistence constraints later. Existing crawler/Room code is a harness, not the design authority.

### `app`
Frozen developer harness only.

## Architecture rules
- Single responsibility.
- Strict one-way dependencies.
- Domain model remains framework-free.
- Persistence maps to/from domain concepts.
- Prefer simple explicit concepts over giant catch-all objects.
- Never bypass provenance or entity boundaries for convenience.

## Model correctness
- Every sourced claim retains provenance.
- Unknown is not false, zero, or empty text.
- Conflicting claims from different sources coexist.
- Source facts are not canonical truth.
- Relationships should represent real-world linked entities when appropriate.
- Media references stay separate from binary storage concerns.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open sources for eventual core dataset.
- No vector search, canonical merge algorithm, or LLM work while MODEL ONLY is active.

## Token discipline
- Use tokens economically.
- Read only files needed for the current model decision.
- Keep explanations short unless explicitly asked.
- Avoid speculative implementation work.

## Collaboration style
Work in small increments. Lock one model concept at a time, update the relevant `AGENTS.md`, then implement only that concept.
