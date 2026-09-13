# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering and understanding Jharkhand through trusted local knowledge.

Johar should feel like a micro-ChatGPT whose world is Jharkhand.

## Core product principle
Johar must be useful to people across Jharkhand, including users who prefer speaking over typing, have limited digital literacy, read slowly, use mixed/local language, or have unreliable connectivity.

Design data and presentation around these needs:
- natural spoken questions should work as well as typed questions;
- terse, fragmented, misspelled, colloquial, and one-word queries should still be useful when intent can be inferred;
- answers should use simple, direct language before detail;
- support Hindi/Hinglish and local-language vocabulary/aliases as the knowledge base grows;
- prefer short actionable answers, icons, cards, images, audio/voice, and clear choices over dense paragraphs;
- preserve local names and colloquial terms so users do not need formal spelling or terminology;
- offline/local knowledge should remain useful under poor connectivity;
- trust must be visible through source/evidence cues without forcing users to understand technical provenance;
- follow-up questions should feel conversational, not like navigating a database.

The product should reduce the amount of reading, typing, navigation, and technical knowledge required to get a useful answer.

## V1 knowledge universe
Prioritize seven everyday Jharkhand knowledge areas:
1. Places
2. Food
3. Festivals
4. Culture
5. Emergency information
6. Weather
7. Local bazar / haat / market knowledge

These organize discovery and presentation; they are not separate screens or rigid database silos.

Johar should eventually answer questions such as:
- "Dassam Falls kaise jayega?"
- "Rugra kab milta hai?"
- "Sarhul kya hai?"
- "bazar near me"
- "pork kaha milega?"
- "nearby haat kab lagta hai?"
- short local terms whose meaning can be resolved from conversation/location context

Near-me and availability answers are location-sensitive and freshness-sensitive. Static shop/market metadata can identify a likely seller; it must never be presented as live inventory unless a fresh source explicitly supports that claim.

## V1 scope — DATA + PRESENTATION ONLY
Focus only on:
1. Data — discover, crawl, refresh, model, store, retrieve, and source Jharkhand knowledge.
2. Presentation — turn retrieved knowledge into simple conversational answers on one chat screen, with inline cards/media/source cues when useful.

Out of scope unless explicitly changed:
- multiple screens/navigation
- browse/category/detail pages
- accounts/social/community
- backend/platform expansion
- unrelated Android infrastructure
- speculative features not required to collect or present knowledge

## Product interaction invariant
Johar has one primary product surface: a single chat screen. Rich components such as entity cards, facts, images/video, source chips, warnings, and follow-up prompts live inside chat; they are not destinations.

## Core knowledge model
Keep five concepts basic and extensible:
- Entity
- Fact
- Relationship
- Source
- Media

`EntityType` answers what a thing is. `KnowledgeDomain` answers what kind of fact is stored about it.

Examples:
- Dassam Falls -> `TOURIST_ATTRACTION`
- Rugra -> `FOOD`
- Sarhul -> `FESTIVAL`
- a haat/bazar -> `MARKET`

Facts remain extensible source-backed key/value records. Relationships connect real entities. Media remains separate from binary storage.

## Current crawler implementation
The V1 crawler is active in `johar-data`; it is no longer only a single Dassam HTML harness.

Bootstrap targets:
- `JHARKHAND` — statewide discovery root and default developer crawl target.
- `DASSAM_FALLS` — focused validation entity.

Crawling is dynamic and source-driven. Do not hardcode entity-specific source URLs, Wikidata Q IDs, OSM IDs, or Commons category IDs. Source identifiers are resolved at runtime and persisted in entity external references.

Current open-source adapters:
- Wikidata — identity, aliases, coordinates, generic claims, graph links, image references.
- OpenStreetMap / Overpass — places, local services, nearby entities, hospitals/police, markets/shops, travel infrastructure, bounded statewide discovery.
- Wikipedia — background/history/culture/food/festival/place text and discovery.
- Wikivoyage — travel/practical text and discovery.
- Wikimedia Commons — image/video references and attribution/license metadata.
- Open-Meteo — current and short-forecast weather facts for coordinate-bearing place entities.

Do not use the public Nominatim service as a periodic/bulk statewide crawler. If it is introduced later, obey its current usage policy or use an appropriate/self-hosted alternative.

## Persistent self-expanding discovery
The discovery loop is:

`root entity + category keywords -> bounded discovery -> entities/facts/relationships/media -> discovered entity keywords -> later crawl`

The keyword table is data-driven and self-expanding. New useful aliases/entities become later crawl work after normalization/deduplication.

Current Room tables include:
- `knowledge_entities`
- `source_facts`
- `entity_relationships`
- `media_assets`
- `crawl_keywords`
- `crawled_sources`

The crawler persists queue/freshness state, processes bounded batches, and continues in later runs instead of attempting to download all of Jharkhand at once.

Current WorkManager behavior:
- developer/manual trigger starts an immediate crawl;
- the first trigger also ensures a unique 24-hour statewide periodic refresh;
- network-connected + battery-not-low constraints apply;
- source failures are isolated so successful sources still persist;
- entity and keyword queues resume from Room on later runs.

## Crawl vocabulary
Initial statewide discovery covers useful concepts for:
- places/waterfalls/picnic spots/villages/rivers
- traditional/local/seasonal foods
- festivals/tribal festivals
- culture/tribal culture/dance/crafts
- hospitals/police
- haat/bazar/weekly markets/local markets
- butcher/meat/pork-oriented shop discovery

This seed vocabulary is not intended to enumerate all knowledge. The crawler expands it from discovered entities/aliases.

## Refresh and storage
Target up to approximately 1.5 GB total local storage when useful. This is neither a Room-only target nor APK size.

Rules:
- refresh/replace stale source-scoped knowledge rather than append forever;
- keep raw source snapshots separately and bounded;
- store media URLs/metadata by default, not binary image/video payloads;
- retain source URL, publisher, retrieval time, evidence, and media attribution/license metadata;
- conflicting source claims may coexist;
- unknown is not false/zero/empty;
- source facts are not canonical truth;
- static market/shop metadata does not prove current stock/availability.

## Module router
### `johar-domain`
Pure Kotlin/JVM model and storage-independent contracts. No Android, HTTP, Room, WorkManager, UI, or concrete source logic.

### `johar-data`
Owns source adapters, discovery/crawl orchestration, refresh, persistence, and WorkManager scheduling.

### `app`
Thin composition/developer harness today; V1 product remains one chat screen with no navigation architecture.

## Architecture rules
- Single responsibility.
- Strict one-way dependencies.
- Domain model remains framework-free.
- Persistence maps to/from domain concepts.
- Prefer simple explicit concepts over giant catch-all objects.
- Never bypass provenance or entity boundaries for convenience.
- Keep fetching, parsing, storage, discovery, and scheduling independently replaceable.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open sources for the core dataset.

## Collaboration style
Work in small increments. Update the relevant `AGENTS.md` whenever architecture/focus changes. Keep explanations compact unless the user asks for depth.
