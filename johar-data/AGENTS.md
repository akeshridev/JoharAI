# AGENTS.md — johar-data

Read root `AGENTS.md` first.

## Scope
Android data/runtime module. Depends on `johar-domain` and owns source discovery, crawling, parsing/extraction, refresh, Room persistence, and WorkManager scheduling for Johar's V1 knowledge database.

## Owns
- `source/` — source adapters that resolve/fetch open data dynamically.
- `remote/` — generic identified HTTP fetching.
- `parser/` — source-specific parsing/cleaning helpers where needed.
- `local/` — Room database, DAOs, source snapshots, entities, facts, relationships, media, crawl keywords, and mappers.
- `crawl/` — bootstrap vocabulary, stable IDs, persistent crawl store, queue/orchestration, and crawl budgets.
- `work/` — immediate and periodic WorkManager execution.

## Crawler invariant
Do not hardcode Jharkhand entity-specific URLs, Wikidata Q IDs, OSM node/way/relation IDs, or Commons category IDs into production crawl logic.

Crawling starts from `CrawlSeed` context such as a name + Jharkhand + India. Adapters resolve source-specific identifiers dynamically and persist them in entity `externalRefs` for later refresh.

`CrawlTarget` is only a developer/bootstrap entry point. `JHARKHAND` is the statewide root; `DASSAM_FALLS` remains a focused validation target.

## Current source adapters
- Wikidata — entity search/resolution, labels, aliases, coordinates, generic source claims, graph relationships, image references.
- OpenStreetMap / Overpass — dynamic named-entity resolution, nearby facilities/services, places, markets, shops, hospitals, police, travel infrastructure, and bounded statewide category discovery.
- Specialized Overpass discovery — precise bootstrap queries for dams, hills/lakes, temples, protected areas, heritage, fire/ambulance services, mandi/vegetable/fish markets, and explicitly tagged/named pork sellers.
- Wikipedia — source-backed background text and entity discovery for places, food, festivals, culture, and related knowledge.
- Wikivoyage — travel/practical text and travel-oriented place/food discovery.
- Wikimedia Commons — media references plus creator/attribution/license metadata.
- Open-Meteo — coordinate-based current conditions and short forecast facts for place-like entities.

Do not use the public Nominatim service as a periodic/bulk statewide crawler. If Nominatim is introduced later, its public-use policy, rate limit, caching, identification, and bulk restrictions must be respected or a suitable/self-hosted service must be used.

## Persistent discovery loop
The crawler is resumable and self-expanding:

`root entity + category keywords -> bounded discovery -> entities/facts/relationships/media -> new entity keywords -> later crawl`

Room persists:
- `knowledge_entities`
- `source_facts`
- `entity_relationships`
- `media_assets`
- `crawl_keywords`
- `crawled_sources` raw/source snapshots

Keyword/entity queues are selected by freshness and stored between WorkManager runs. Do not attempt to crawl all of Jharkhand in one execution.

Current per-run budgets are intentionally bounded. Daily periodic work continues the queue.

## V1 discovery categories
Crawler vocabulary/discovery supports:
- Places
- Food
- Festivals
- Culture
- Emergency
- Weather context
- Local bazar / haat / market

Bootstrap vocabulary should aim for useful coverage, not every wording variation. Current vocabulary includes broader place types, local/tribal/seasonal food, fairs, language/music/art, emergency services, and local market subtypes. Discovered entities and aliases expand future crawl context.

Local bazar discovery may include marketplaces, butcher/meat shops, nearby services, and OSM-tagged shops. A discovered shop/market is not evidence of live inventory. Never convert a likely seller into a claim that an item such as pork is currently in stock unless a fresh source explicitly supports that claim.

Item-specific discovery must be evidence-based. For example, a pork query may use explicit OSM `butcher=pork/pig` metadata or a pork/pig name match, but a generic butcher shop must not be labeled as a pork seller just because it is a butcher.

## Refresh/storage rules
- Source failures are isolated. One failed adapter must not discard successful data from other adapters.
- Source-backed facts from different sources may conflict and coexist.
- Refresh replaces facts/relationships/media for the same entity+source instead of growing append-only forever.
- Raw source snapshots remain separate from extracted knowledge and are bounded before persistence.
- Store media references/metadata by default, not binary image/video payloads.
- Preserve source URL, publisher, retrieval time, evidence, and media attribution/license metadata.
- The overall local target remains approximately 1.5 GB, not 1.5 GB of Room rows and not APK size.

## Source correctness
- Prefer structured APIs over scraping rendered pages when an API is available.
- Extract only source-supported information; missing data is not an invitation to guess.
- Keep endpoint/source-specific parsing inside adapters.
- Keep external IDs as source references, not canonical Johar identity.
- Do not reconcile conflicting facts in the data layer.
- Do not infer live availability from static market/shop metadata.

## WorkManager
A manual enqueue performs an immediate crawl and ensures a unique 24-hour statewide periodic refresh exists. Network connectivity and battery-not-low constraints apply. The periodic worker continues persisted stale entity/keyword queues.

## Architecture rules
- No Compose/ViewModel/UI code here.
- No Android/network/database types in `johar-domain`.
- Data adapters map to the framework-free domain model.
- Presentation never imports source adapters or DAOs directly.
- Keep source fetching, extraction, storage, discovery, and scheduling independently replaceable.
