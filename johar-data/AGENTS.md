# AGENTS.md — johar-data

Read root `AGENTS.md` first.

## Scope
Android data/runtime module. Depends on `johar-domain` and owns the prebuilt knowledge database, source discovery, crawling, parsing/extraction, refresh, Room persistence, and WorkManager scheduling for Johar's V1 knowledge database.

## Knowledge bootstrap architecture
Johar now starts from a prebuilt Room-compatible SQLite seed database rather than importing a JSON pack row-by-row at runtime.

- Seed asset path: `database/johar-base-2026.09.db`.
- Current seed knowledge release: `2026.09-mega-v1.2`.
- The seed matches the current Room schema and is copied by Room only when `johar.db` does not yet exist.
- After first creation, the copied database is the user's working Room database. Live crawler refresh/enrichment writes into the same tables.
- Future bundled seed releases must not overwrite an existing working database; upgrades should use schema migration plus an explicit pack-merge strategy when needed.
- Stable/slow-changing knowledge belongs in the seed. Weather, opening/status information, transport schedules, temporary closures, changing contacts, and other volatile facts belong to live refresh.
- The seed is an offline baseline, not evidence of current availability. Market/shop metadata never proves live stock.

## Owns
- `source/` — source adapters that resolve/fetch open data dynamically.
- `remote/` — generic identified HTTP fetching.
- `parser/` — source-specific parsing/cleaning helpers where needed.
- `local/` — Room database, DAOs, source snapshots, entities, facts, relationships, media, crawl keywords, and mappers.
- `crawl/` — bootstrap vocabulary, stable IDs, persistent crawl store, queue/orchestration, and crawl budgets.
- `work/` — immediate and periodic WorkManager execution.
- `src/main/assets/database/` — prebuilt Room seed database metadata/asset.

## Crawler invariant
Do not hardcode Jharkhand entity-specific URLs, Wikidata Q IDs, OSM node/way/relation IDs, or Commons category IDs into production crawl logic.

Crawling starts from `CrawlSeed` context such as a name + Jharkhand + India. Adapters resolve source-specific identifiers dynamically and persist them in entity `externalRefs` for later refresh.

`CrawlTarget` is only a developer/bootstrap entry point. `JHARKHAND` is the statewide root; `DASSAM_FALLS` remains a focused validation target.

## Current source adapters
- Wikidata — identity, aliases, coordinates, generic claims, graph relationships, image references.
- OpenStreetMap / Overpass — dynamic named-entity resolution, nearby facilities/services, places, markets, shops, hospitals, police, travel infrastructure, and bounded statewide category discovery.
- Specialized Overpass discovery — precise bootstrap queries for dams, hills/lakes, temples, protected areas, heritage, fire/ambulance services, mandi/vegetable/fish markets, and explicitly tagged/named pork sellers.
- Wikipedia — background/history/culture/food/festival/place text and discovery.
- Wikivoyage — travel/practical text and travel-oriented place/food discovery.
- Wikimedia Commons — media references plus creator/attribution/license metadata.
- Open-Meteo — coordinate-based current conditions and short forecast facts for place-like entities.

Public Overpass is a shared best-effort source, not Johar's primary statewide backbone. Do not issue one OSM enrichment request per discovered entity during a `JHARKHAND` run. Statewide runs use OSM only for bounded bootstrap discovery; full OSM entity/nearby enrichment is reserved for focused entity crawls or a future dedicated throttled enrichment queue.

Do not use public Overpass instances for very broad statewide scans such as all villages, all rivers, generic places, or similarly expensive queries. Broad statewide entity discovery should come from Wikidata/MediaWiki or other suitable sources; OSM should handle narrower, selective statewide categories.

Keep statewide discovery batches deliberately small. The persistent queue is expected to grow coverage over many periodic runs instead of maximizing requests in one execution. Treat 429, 504, and network timeout responses as source-pressure signals, not reasons to increase retries or concurrency.

Do not use the public Nominatim service as a periodic/bulk statewide crawler. If Nominatim is introduced later, its public-use policy, rate limit, caching, identification, and bulk restrictions must be respected or a suitable/self-hosted service must be used.

## Discovery quality invariants
- V1 optimizes for useful breadth, not perfect classification. Roughly 70–80% useful/accurate discovery is acceptable while the knowledge base grows; reject obvious junk without over-filtering plausible Jharkhand knowledge.
- A bootstrap/discovery category is a search intent, not an entity type. Never label every result of a FOOD search as `FOOD`, every CULTURE result as `CULTURAL_PRACTICE`, or every LOCAL_BAZAR result as `MARKET` without source evidence.
- Broad MediaWiki/Wikidata search results must pass semantic category validation before persistence.
- For MediaWiki discovery, page title is the strongest identity/type signal. Search snippets may support geography/context but must not turn a district into a waterfall, a temple into a district, or a national park into a town.
- Broad text-search candidates must contain candidate-side evidence tying them to Jharkhand or a Jharkhand locality; the query phrase itself is not relevance evidence.
- Reject high-confidence noise such as unrelated regions, list/index pages, election/assembly pages, and unresolved source IDs. Do not chase perfect filtering of every ambiguous candidate in V1.
- Do not persist unresolved source identifiers such as bare Wikidata `Q12345` values as user-facing `KnowledgeEntity` rows.
- Structured/high-precision source results such as explicit OSM hospital/market tags may use their source semantics directly, but must still avoid unsupported inventory/availability claims.
- Keep regression tests for real crawl failures.

## Persistent discovery loop
The crawler is resumable and self-expanding:

`seed database + root/category refresh -> bounded discovery -> entities/facts/relationships/media -> later refresh`

Room persists:
- `knowledge_entities`
- `source_facts`
- `entity_relationships`
- `media_assets`
- `crawl_keywords`
- `crawled_sources`

Keyword/entity queues are selected by freshness and stored between WorkManager runs. Do not attempt to crawl all of Jharkhand in one execution.

## V1 discovery categories
Crawler vocabulary/discovery supports Places, Food, Festivals, Culture, Emergency, Weather context, and Local bazar / haat / market.

Local bazar discovery may include marketplaces, butcher/meat shops, nearby services, and OSM-tagged shops. A discovered shop/market is not evidence of live inventory.

## Refresh/storage rules
- Source failures are isolated.
- Source-backed facts from different sources may conflict and coexist.
- Refresh replaces source-scoped data instead of growing append-only forever.
- Raw source snapshots remain separate and bounded.
- Store media references/metadata by default, not binary image/video payloads.
- Preserve source URL, publisher, retrieval time, evidence, and media attribution/license metadata.
- The overall local target remains approximately 1.5 GB, not 1.5 GB of Room rows and not APK size.

## WorkManager
Manual enqueue performs an immediate crawl and ensures a unique 24-hour statewide periodic refresh exists. Network-connected + battery-not-low constraints apply. Manual unique work uses KEEP semantics.

## Architecture rules
- No Compose/ViewModel/UI code here.
- No Android/network/database types in `johar-domain`.
- Presentation never imports source adapters or DAOs directly.
- Keep seed creation, source fetching, extraction, storage, discovery, and scheduling independently replaceable.
