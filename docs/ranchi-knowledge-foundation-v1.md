# Ranchi Knowledge Foundation V1

## Goal
Build a Ranchi-first knowledge foundation that is genuinely useful to a normal resident or visitor, not merely large or optimized for Golden-100.

Johar should answer everyday Ranchi questions from grounded local evidence, work offline for static/slow-changing knowledge, and clearly separate live/current claims from offline facts.

## Product-oriented acquisition domains
Crawler acquisition is prioritized by common-man need, while storage remains source-agnostic and canonical.

1. Health & emergency
2. Transport & mobility
3. Daily life
4. Education
5. Food & markets
6. Places & recreation
7. Culture & local knowledge

`RanchiCommonManAcquisitionPlan` owns the ordered bootstrap plan. Source adapters do not own product priority.

## Canonical pipeline

raw external source
-> source adapter result
-> candidate validation/classification
-> canonical entity resolution/deduplication
-> source-backed facts/relationships/media
-> Room
-> retrieval/spatial/routing
-> grounded answer generation

Raw crawler output must never become a second queryable knowledge store.

Booster content follows the same canonical model and is a patch/bootstrap mechanism only.

## Source priority

### Tier 1: authoritative/official
Use first where the source actually publishes the required fact:
- Ranchi District / Jharkhand government
- official hospital/institution/university/transport/public-service sites
- Jharkhand Tourism and relevant specialist government sources

Best for identity, official contact/address, civic responsibility, institutional facts and durable public information.

### Tier 2: structured open geographic data
OpenStreetMap/Overpass is preferred for coordinates, locality and static POI/service discovery.

OSM presence is never proof of current opening status, inventory, queue, availability, fare or safety.

### Tier 3: open knowledge sources
Wikidata, Wikipedia, Wikivoyage and Wikimedia Commons support aliases, background, identity, relationships, descriptions and media.

They should not override stronger official evidence for current operational facts.

### Tier 4: historical/cultural corpus
Books and gazetteers are valuable for history/culture only. They are not current operational evidence.

### Live sources
Live facts bypass static assumptions and require an explicitly fresh source. Open-Meteo currently supplies weather. Future traffic, train status, fares, opening status and inventory require dedicated live adapters.

## Freshness rules

### STATIC_OR_SLOW
Examples: identity, alias, coordinates, history, cultural meaning, institution type.

Refresh infrequently unless upstream source changes.

### PRACTICAL_CHANGEABLE
Examples: address, phone, facilities, accessibility, parking, typical opening hours, market day, cuisine metadata.

Refresh periodically and always preserve retrieval time/source.

### LIVE
Examples: open now, weather now, traffic, current fare, stock, live train status, availability, today's event status.

Never infer from static facts. Return explicit unavailable/unconfirmed behavior when no live source is present.

## Entity and fact contracts
Entity count alone is not a useful quality metric. Each major category should satisfy a minimum fact contract.

### Hospital / clinic
Core: canonical name, aliases, type, Ranchi locality/address, coordinates when available, official/source URL.
Useful: contact, emergency capability, specialties/departments when source-backed.
Live-sensitive: current doctor availability, waiting time, bed availability.

### Pharmacy
Core: name, locality/address, coordinates, source.
Live-sensitive: open-now and medicine stock.

### Transport anchor
Core: canonical name, aliases, type, coordinates, locality, source.
Useful: official station/airport code and static access information when sourced.
Live-sensitive: train/flight/bus status, fare, platform/gate changes.

### Education institution
Core: canonical name, aliases, institution type, address/locality, coordinates when available, official website/source.
Useful: affiliation/program information only from reliable evidence.
Live-sensitive: admission status, current cutoff, current seat availability.

### Restaurant / cafe / market
Core: name, category, locality/address, coordinates when available, source.
Useful: cuisine/diet/category, market type, static facilities when explicitly sourced.
Live-sensitive: open now, stock, today's menu, current price, queue.

### Attraction / recreation place
Core: name, aliases, type, location, coordinates, source.
Useful: parking, stairs/walking, toilets, accessibility, family suitability only when evidence supports it.
Live-sensitive: current closure, crowd, today's entry availability.

## Deduplication rules
1. Prefer an existing canonical entity when normalized name + Ranchi geography agree.
2. Merge aliases rather than replacing them.
3. Preserve source-specific external IDs in `externalRefs`.
4. Do not merge only because two names are similar when geography/type disagree.
5. When authoritative IDs or coordinates strongly identify the same place, use them as additional evidence for canonical resolution.
6. Conflicting source-backed facts may coexist; do not erase disagreement by choosing an unsupported winner.

The next deduplication improvement should move beyond normalized-name-only matching toward a scored identity resolver using name/alias, type, source IDs and geographic proximity.

## Booster vs crawler vs live

### Booster
Use for:
- critical missing common-man entities
- aliases/Hinglish/local spelling patches
- verified high-value facts
- temporary acquisition gaps while source adapters are improved

Do not use booster as the long-term bulk Ranchi database.

### Crawler
Primary acquisition path for static/slow/practical knowledge. It should discover, normalize, validate, deduplicate and persist into canonical Room tables.

### Live adapter
Use only when the truth can materially change at query time and static/offline data must not be presented as current.

## Measurable milestones

### M1 — Acquisition architecture
- common-man acquisition plan is explicit and ordered
- all seven product domains have seeds
- crawler and booster continue to converge on canonical Room tables
- source/freshness metadata preserved

### M2 — Practical entity coverage
Target 500+ useful Ranchi entities with deliberate domain balance, not raw OSM volume.

Suggested minimums:
- Health/emergency: 75+
- Transport/mobility: 50+
- Daily life: 120+
- Education: 75+
- Food/markets: 100+
- Places/recreation: 100+
- Culture/local knowledge: 40+

### M3 — Fact depth
- 1,000+ source-backed useful facts
- >90% of high-value named entities have at least one useful source-backed fact beyond identity where applicable
- >90% of place-like entities used for nearby/routing have coordinates
- alias coverage measured separately from entity count

### M4 — Evidence quality
- >95% facts contain source URL/publisher/retrieval metadata
- no static fact presented as live confirmation
- conflicting claims remain attributable

### M5 — User usefulness
Evaluate by domain as well as Golden-100:
- useful-answer rate
- partial-answer rate
- data-gap rate
- parser/retrieval-gap rate
- evidence coverage
- nearby/spatial success
- live-safety correctness

Golden-100 should improve naturally, but it is not the acquisition objective.

## Implementation decisions

### 1. Product-oriented bootstrap plan
The Ranchi bootstrap seed list is separated from crawler orchestration into `RanchiCommonManAcquisitionPlan`.

Reason: product priority, source behavior and persistence are different responsibilities. A flat source-oriented keyword list made it difficult to reason about common-man coverage or change priorities safely.

This change intentionally does not introduce a second storage model or change canonical Room persistence.

### 2. Explicit school acquisition
`schools in Ranchi` is a P0 education seed. It is handled by `RanchiEducationDiscoveryAdapter`, which issues a bounded OSM query for `amenity=school` inside the Ranchi administrative area and writes normal `KnowledgeEntity` candidates with OSM provenance and `joharPackType=SCHOOL`.

Reason: before this adapter, a school seed using the generic `PLACES` category could fall through to generic tourism discovery. Schools are a common-man requirement and need precise source semantics rather than phrase-level retrieval workarounds.

The adapter intentionally handles only schools. College, university and library discovery already exists in `OverpassSpecializedDiscoveryAdapter`; keeping the school adapter narrow prevents duplicate Overpass requests and duplicate candidate streams.

School discovery remains static/practical metadata only. OSM presence does not prove current admission status, opening state, fees, board affiliation or seat availability. Those require stronger or fresher evidence.
