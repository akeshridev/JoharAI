# AGENTS.md — johar-domain

Read the root `AGENTS.md` first.

## Scope
Pure Kotlin/JVM model module for Johar's knowledge system. Keep all models independent from Android, networking, parsing, persistence, WorkManager, and presentation.

## Hard boundaries
- No Android imports.
- No Compose/UI.
- No Jsoup, HTTP, Room, WorkManager, model runtime, or serialization implementation.
- No concrete data-source implementations.
- Persistence and source adapters map to these models; they do not define them.

## Core knowledge model
Keep the knowledge model basic, clear, and scalable. Use five core concepts:
1. Entity — a real thing such as Dassam Falls, Rugra, Sarhul, a river, village, hospital, market, restaurant, or station.
2. Fact — one source-backed attribute about an entity, grouped into a knowledge domain.
3. Relationship — a source-backed typed/predicate connection between two entities.
4. Source — where knowledge came from, including provenance/evidence.
5. Media — image/video references related to an entity.

Current domain models include `KnowledgeEntity`, `EntityRelationship`, `KnowledgeSource`, `SourceFact`, and `MediaAsset`.

## Entity type vs knowledge domain
Entity type answers **what the thing is**. Knowledge domain answers **what kind of fact we are storing about that thing**.

Examples:
- Dassam Falls -> `EntityType.TOURIST_ATTRACTION`
- Rugra -> `EntityType.FOOD`
- Sarhul -> `EntityType.FESTIVAL`
- a haat/bazar -> `EntityType.MARKET`

The same entity may have facts in many knowledge domains. Do not use `KnowledgeDomain` as the entity's identity/type.

## Crawl/discovery contracts
Crawling starts from `CrawlSeed`, not a hardcoded source URL or external ID. A seed may carry a name, Jharkhand/India context, optional coordinates, optional entity type, and source-specific external references discovered at runtime.

`CrawlTarget` is only a developer/bootstrap convenience. Current roots include Jharkhand and Dassam Falls; concrete source URLs/IDs remain outside the domain model.

The seven V1 discovery categories are represented by `DiscoveryCategory`:
- Places
- Food
- Festivals
- Culture
- Emergency
- Weather
- Local Bazar

`CrawlKeyword` represents data-driven discovery vocabulary/state. Keywords are operational discovery concepts; they must not replace canonical entities/facts.

## Model rules
- Every sourced claim retains provenance/evidence.
- Conflicting facts from different sources may coexist.
- Unknown is not false, zero, or empty text.
- Facts use an extensible field/key plus typed value; do not create giant entity-specific data classes.
- If a value is another real-world thing, prefer an entity relationship when useful.
- Relationship predicates are extensible strings so upstream knowledge graphs can be preserved without enum/schema churn.
- Media stores references and metadata, not binary payloads.
- Preserve media source, attribution/license metadata when available.
- Do not hardcode place-specific source URLs or external IDs in the domain model.
- Persistence maps to/from the model; Room does not drive the model.
- Add fields/types only when real data requires them.

## Working rule
Prefer the simplest model that preserves source-backed data correctly and can grow across Jharkhand without migration-heavy redesign.
