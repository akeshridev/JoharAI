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

The model must support exhaustive category coverage without hardcoding place-specific fields or URLs. Each knowledge domain should be able to hold all discovered source-backed facts relevant to that category while remaining extensible as new fields appear.

The model must separate:
- canonical entities
- source documents/snapshots
- source facts/claims
- provenance/evidence
- relationships between entities
- media assets/references
- category/field definitions used to organize knowledge
- future canonical/resolved facts
- future derived recommendations

Do not collapse these into one giant Dassam record.

## Discovery direction
Entity-specific URLs/IDs should not be hardcoded into the knowledge model. Eventual discovery starts from a canonical entity request (name, aliases, region/country) and source adapters resolve dynamic IDs/URLs from open sources. Source adapters/endpoints may be configured; place-specific data is discovered.

## Category completeness
For every canonical entity, the packed knowledge DB should be able to represent all discovered data across every applicable knowledge domain, not a fixed minimal subset.

Examples include:
- Tourism: description, highlights, best time, suggested duration, attractions, activities.
- Family & Accessibility: walking effort, stairs, wheelchair access, elderly/kid suitability, rest areas.
- Safety & Emergency: hazards, restrictions, emergency contacts, nearby medical/police entities.
- History & Culture: origin, historical claims, local names/stories, cultural significance.
- Food: dishes, vendors/restaurants, nearby food entities, availability.
- Travel & Logistics: routes, distances, transport, parking, entry fee, hours.
- Weather & Season: seasonal behavior, monsoon/heat/fog context, water-flow context.
- Facilities: toilets, water, shops, changing areas, connectivity, rest areas.
- Geography: coordinates, administrative areas, rivers, terrain, elevation/height, nearby entities.

The field model must remain extensible so newly discovered source-backed attributes can be added without redesigning the whole database.

## Media direction
Media references may be captured in the packed knowledge model whenever available, even while runtime ingestion is frozen.

Supported examples:
- direct/static image URLs
- Wikimedia Commons media URLs and thumbnails
- YouTube/video page URLs and preview/thumbnail URLs
- source-page image/video links

Store references and metadata, not binary image/video payloads, at this stage. Preserve source/provenance and licensing/attribution when available. The existence of a public URL does not imply reuse rights.

## Local data budget
Johar may use up to approximately 1 GB of local knowledge data when useful.

Treat this as a local-data budget, not an APK-size target:
- Prefer a compact prepacked/downloadable knowledge database rather than bundling the full budget inside the APK.
- Facts, entities, relationships, provenance, source snapshots, indexes, and metadata may be stored locally.
- Media should default to URL/reference metadata; binary image/video caching is a separate future concern and must be bounded explicitly.
- Preserve enough raw/source data to allow reprocessing and model iteration, but avoid blindly mirroring entire upstream datasets when only a useful subset is needed.
- Storage size should not drive the domain model; the model remains simple: Entity, Fact, Relationship, Source, Media.

## Module router

### `johar-domain`
Primary active module. Pure Kotlin/JVM knowledge model and domain rules. Owns entity/fact/provenance/relationship/media/category concepts and storage-independent contracts. No Android, UI, network, parser, Room, WorkManager, or LLM implementation. Read `johar-domain/AGENTS.md` before changing it.

### `johar-data`
Currently frozen except when needed to validate the model against Room constraints later. Existing crawler/Room code is a harness, not the design authority. The domain model drives persistence shape, not the reverse.

### `app`
Frozen developer harness only. No product UI work.

## Architecture rules
Follow the principles demonstrated in `akeshridev/AIFriendlyAppArchitecture`:
- Single responsibility.
- Strict one-way dependencies.
- Domain model must remain framework-free.
- Persistence shapes must map to/from the domain model; Room annotations never enter domain types.
- Prefer small explicit concepts over a giant catch-all model.
- Never bypass provenance or entity boundaries for convenience.

## Model correctness
- Every sourced claim must retain provenance.
- Unknown is not false, zero, or empty text.
- Conflicting claims from different sources must coexist.
- Source facts are not canonical truth.
- Canonical/resolved knowledge is a later layer derived from source facts.
- Nearby places, hospitals, foods, villages, rivers, etc. should be modeled as entities/relationships when appropriate, not flattened into arbitrary strings.
- Source-specific wording/evidence must remain available even if a normalized value is also stored.
- Media references should remain independent from binary storage/download concerns.
- The model should support a prepacked local DB later, but must not be coupled to Room.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open sources for eventual core dataset.
- No vector search, canonical merge algorithm, or LLM work while MODEL ONLY is active.

## Token discipline
- Use tokens economically.
- Read only files needed for the current model decision.
- Do not scan the whole repo unless necessary.
- Keep explanations short unless explicitly asked.
- Make only changes needed for the current request.
- Avoid speculative implementation work.

## Collaboration style
Work in small increments. Lock one model concept at a time, update the relevant `AGENTS.md`, then implement only that concept.
