# Johar AI Agent Context

## Product
Johar AI is an offline-first Android local-knowledge assistant.

### V1 prototype focus
V1 is deliberately Ranchi-first:
- Ranchi city
- Ranchi district
- Ranchi neighborhoods/localities
- nearby day-trip places that a Ranchi user would realistically ask about, roughly within an 80-100 km travel radius when useful

The architecture must remain scalable district-by-district across Jharkhand, but V1 quality is measured on Ranchi depth rather than statewide breadth.

See `docs/ranchi-v1-scope.md` for the full persona/domain matrix.

## Core product principle
Johar should feel like a small local ChatGPT whose strongest world knowledge is Ranchi.

It must be useful not only to tourists, but also to:
- local residents
- children, parents and elderly users
- women travelling alone
- students and job seekers
- commuters, drivers and bikers
- businessmen, business visitors and shopkeepers
- patients/caregivers
- devotees
- foodies
- people with accessibility needs
- users with limited connectivity or low digital literacy

Design data and presentation around these needs:
- natural spoken questions should work as well as typed questions;
- terse, fragmented, misspelled, colloquial and one-word queries should still work when intent can be inferred;
- support English, Hindi, Hinglish and local vocabulary/aliases as the knowledge base grows;
- preserve conversational context, e.g. `mere aas paas mandir?` -> `Lalpur` must continue with Lalpur as the location constraint;
- prefer short actionable answers before detail;
- trust must be visible through source/evidence cues;
- never invent a top list, shop, timing, address, availability or safety claim.

## V1 knowledge universe
Do not optimize only for tourism. Ranchi V1 should cover as much useful local life as reliable data allows:
1. Places, neighborhoods and navigation
2. Food, restaurants, cafes and street food
3. Haat, bazar, mandi, meat/fish/vegetable markets and shopping
4. Religion/spiritual places
5. Family, children, elderly and accessibility
6. Hospitals, pharmacies where supported, police/fire/emergency
7. Transport, airport, railway, bus, parking and mobility
8. Education, colleges, universities, libraries and student life
9. Work, business areas, industrial/business institutions and useful business services
10. Civic/government/public facilities
11. Culture, tribes, history, language, art, craft, music and dance
12. Festivals, fairs and events
13. Nature, biodiversity, weather and seasonal knowledge
14. Safety and practical needs
15. Recreation, sports, photography and lifestyle
16. Everyday local utilities such as banks/ATMs, fuel, toilets and repair/service categories when reliable open data exists

## Freshness model
Treat facts differently by freshness:
- `STATIC_OR_SLOW`: identity, geography, history, culture, long-lived relationships.
- `PRACTICAL_CHANGEABLE`: addresses, facilities, accessibility, restaurant/market metadata, typical market day. Refresh periodically.
- `LIVE`: weather, open-now, current inventory, traffic, fares, event route/status, current officeholder. Never imply freshness without current evidence.

Historical books/gazetteers are historical evidence only. They must not be used to prove current roads, hours, facilities, officials, business status or political status.

## Answer behavior
Johar should not reflexively say `online search karun?`.

- Strong offline evidence -> answer directly.
- Partial evidence -> answer the known part and clearly state the missing part.
- Near-me request without location -> ask only for location/locality.
- Live question without fresh evidence -> explain that the live portion cannot be verified while still giving useful static context.
- No evidence -> say the local corpus does not support the claim; do not fabricate.

## Evaluation strategy
Keep two separate benchmarks:

### Frozen retrieval evaluation
Existing retrieval benchmark remains frozen. Do not tune retrieval merely to game this benchmark.

### Ranchi Coverage Eval
A product-coverage benchmark built from realistic Ranchi queries across personas and domains. Track:
- answerable / partial / no-evidence
- missing fact groups
- wrong result type
- locality coverage
- freshness-required cases
- persona/domain coverage
- provenance completeness

The primary V1 success criterion is practical Ranchi answerability, not corpus megabytes.

## V1 scope — DATA + PRESENTATION
Focus on:
1. Data — discover, crawl, ingest, refresh, model, store, retrieve and source Ranchi knowledge.
2. Presentation — turn retrieved knowledge into simple conversational answers on one chat screen with cards/media/source cues when useful.

Out of scope unless explicitly changed:
- multiple product screens/navigation
- accounts/social/community
- unrelated Android infrastructure
- speculative features that do not improve Ranchi knowledge quality

## Product interaction invariant
Johar has one primary product surface: a single chat screen. Rich cards, facts, images/video, source cues, warnings and follow-up prompts appear inside chat.

## Core knowledge model
Keep five concepts basic and extensible:
- Entity
- Fact
- Relationship
- Source
- Media

`EntityType` answers what a thing is. `KnowledgeDomain` answers what kind of fact is stored about it. Facts remain source-backed key/value records. Relationships connect entities. Media remains separate from binary storage.

## Data priorities
For the Ranchi prototype, high-density practical local data outranks additional historical-book volume.

Prioritize:
- locality/address/coordinate coverage
- temples/religious sites
- parks/kids/family places
- restaurants/food/seasonal food
- haat/bazar/markets/meat/fish/vegetable shopping
- hospitals/police/emergency
- airport/rail/bus/transport landmarks
- accessibility, walking, stairs, parking, toilets when sourceable
- colleges/universities/student landmarks
- business/industrial/public institutions
- culture/history/festivals
- nearby day trips

Historical/cultural corpora remain valuable supporting evidence, not the main volume target.

## Current open-source adapters
Current adapters include:
- Wikidata — identity, aliases, coordinates, claims, graph links, image references
- OpenStreetMap / Overpass — places, local services, hospitals/police, markets/shops, travel infrastructure
- Wikipedia — background/history/culture/food/festival/place text and discovery
- Wikivoyage — travel/practical text and discovery
- Wikimedia Commons — media references and license/attribution metadata
- Open-Meteo — current and short-forecast weather for coordinate-bearing entities

Do not use the public Nominatim service as a periodic/bulk crawler.

## Persistent discovery
The discovery loop remains:

`Ranchi root + domain keywords -> bounded discovery -> entities/facts/relationships/media -> aliases/entities -> later crawl`

Room tables currently include:
- `knowledge_entities`
- `source_facts`
- `entity_relationships`
- `media_assets`
- `crawl_keywords`
- `crawled_sources`

Refresh/replace stale source-scoped knowledge instead of appending forever. Preserve source URL, publisher, retrieval time, evidence, freshness and media attribution/license metadata.

Unknown is not false/zero/empty. Conflicting sourced claims may coexist. Static shop/market metadata never proves current stock.

## Module router
### `johar-domain`
Pure Kotlin/JVM model and storage-independent contracts. No Android, HTTP, Room, WorkManager, UI or concrete source logic.

### `johar-data`
Source adapters, discovery/crawl orchestration, refresh, persistence and WorkManager scheduling.

### `app`
Thin composition/developer harness today; V1 product remains one chat screen.

## Architecture rules
- Single responsibility.
- Strict one-way dependencies.
- Domain model remains framework-free.
- Persistence maps to/from domain concepts.
- Prefer simple explicit concepts over giant catch-all objects.
- Never bypass provenance or entity boundaries for convenience.
- Keep fetching, parsing, storage, discovery and scheduling independently replaceable.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open/government/public-domain/user-provided/legal sources for the core dataset.

## Collaboration style
Work in small increments. Keep the Ranchi Coverage Eval visible while adding data. Every meaningful corpus addition should be justified by a real coverage gap rather than raw size.
