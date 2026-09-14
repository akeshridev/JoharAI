# Ranchi V1 Gap Ledger

This ledger turns real Johar questions into concrete data/retrieval/product work. A gap is recorded when Johar cannot answer usefully from trusted offline evidence, returns the wrong type, lacks required practical fields, loses conversation context, or needs genuinely live information.

The goal is not to eliminate every `LIVE` gap offline. The goal is to know exactly which part can be answered offline and which part requires fresh evidence.

## Gap classes

- `DATA_STATIC` — durable identity/history/geography/entity coverage is missing.
- `DATA_PRACTICAL` — address, coordinates, parking, accessibility, cuisine, typical market day, facilities, etc. are missing or too thin.
- `RETRIEVAL` — data exists but query routing/ranking/filtering cannot find it correctly.
- `CONTEXT` — follow-up locality/constraint is not carried across turns.
- `RANKING` — multiple valid entities exist but Johar lacks reliable ranking/constraint signals.
- `LIVE` — open-now, inventory, traffic, current fare, current officeholder, event today, live weather, etc. require fresh evidence.
- `UX` — Johar should ask one small clarification instead of failing or reflexively offering online search.

## P0 — prototype blockers

| Gap | Example queries | Class | Required knowledge / behavior | Current source direction |
|---|---|---|---|---|
| Locality-level discovery | `mere aas paas mandir`, then `Lalpur` | DATA_PRACTICAL + CONTEXT + RETRIEVAL | locality entities, coordinates, entity-to-locality relation/distance, carry `Lalpur` into next turn | OSM localities + official Ranchi PIN/locality data |
| Markets/haat | `Ranchi ke bazar`, `Sunday ko kaunsa haat`, `Harmu ke paas daily market` | DATA_PRACTICAL | market name, coordinates/address, locality, market type, typical day/time, typical offerings, source date | OSM + government/local official sources where available |
| Mutton/pork/fish | `mutton kahan`, `pork kahan milega`, `Doranda fish market` | DATA_PRACTICAL | butcher/market identity, location, meat type/offerings when explicitly sourced; never infer current stock | OSM static shop tags + verified local/official sources |
| Temple practical use | `parents ke sath mandir`, `kam stairs wala mandir`, `Jagannath Mandir parking` | DATA_PRACTICAL + RANKING | stairs/walking, wheelchair/accessibility, parking, approach, toilets, coordinates | Jharkhand Tourism + OSM + official temple/public sources |
| Parks/kids | `child park address`, `baccho ke liye park` | DATA_PRACTICAL | exact park identity, address/coordinates, playground/kids facilities, toilets, parking | OSM + official tourism/park sources |
| Restaurants | `Ranchi ke top 5 restaurant`, `Lalpur family vegetarian restaurant` | DATA_PRACTICAL + RANKING | restaurant identity, coordinates/address, cuisine/diet, family/parking when sourced, price band where legally/sourceably available | OSM for static metadata; ranking must use transparent evidence rather than invented “top” claims |
| Local food availability | `Dhuska kahan milega`, `Rugra kahan milega` | DATA_PRACTICAL | dish entity + season + restaurant/market relationships + locality | trusted food/culture sources + explicit market/restaurant metadata |
| Hospitals/emergency | `Bariatu ke paas hospital`, `police station near me` | DATA_PRACTICAL + RETRIEVAL | official names/contacts + coordinates + locality/distance | District Ranchi NIC + OSM enrichment |
| Transport anchors | `airport se Lalpur`, `main bus stand`, `railway station` | DATA_PRACTICAL | airport/station/bus stand coordinates, locality, static route context | OSM + official Ranchi transport/how-to-reach sources |

## P1 — breadth needed for “Ask anything about Ranchi”

| Gap | Example queries | Class | Required knowledge |
|---|---|---|---|
| Education | `major colleges`, `Lalpur ke paas library` | DATA_STATIC + DATA_PRACTICAL | colleges/universities/schools/libraries, addresses, coordinates, official websites |
| Business/work | `IT companies kis area me`, `coworking`, `major business areas`, `convention venue` | DATA_STATIC + DATA_PRACTICAL | commercial/industrial areas, offices where sourceable, coworking, conference/event venues, airport/business connectivity |
| Hotels/lodging | `airport ke paas hotel` | DATA_PRACTICAL | hotels/guest houses/hostels, coordinates, locality, facilities; no invented availability/price |
| Civic/government | `collector office`, `municipal office`, `trade license kahan` | DATA_STATIC + DATA_PRACTICAL | office identities, addresses, authoritative process links, service responsibility |
| Shopping/utilities | `ATM`, `petrol pump`, `EV charging`, `public toilet`, `mobile repair`, `bike puncture` | DATA_PRACTICAL | service category, coordinates, address, static facilities/opening hours when sourced |
| Recreation/lifestyle | `morning walk park`, `stadium`, `gym`, `cinema`, `peaceful evening place` | DATA_PRACTICAL + RANKING | relevant places plus facilities, hours where sourced, suitability evidence |
| Rural/local culture | `Ranchi ke aas paas gaon aur local culture` | DATA_STATIC + DATA_PRACTICAL | nearby village entities, culture relationships, provenance; avoid stereotyping |
| History/culture | `Birsa Munda connection`, `Munda/Oraon culture`, `Sarhul`, `Sohrai` | DATA_STATIC | trusted historical/cultural evidence linked to Ranchi entities/places |
| Day-trip planning | `1 day plan`, `nearby day trip`, `parents + little walking` | RANKING | distance, travel time context, accessibility, season, category, family constraints |

## P2 — freshness/live layer

These are expected to remain explicitly freshness-sensitive even when a useful offline fallback exists.

| Live question | Offline fallback | Fresh evidence needed |
|---|---|---|
| `restaurant abhi open hai?` | static restaurant + normal opening-hours metadata if available | current/open-now verification |
| `fresh pork aaj milega?` | shops/markets that typically sell it | current inventory |
| `Deori Mandir aaj kitne baje band?` | static temple/timing information with source date | current-day schedule/exception |
| `aaj traffic kaisa hai?` | route/road geography | live traffic |
| `auto fare kitna?` | route endpoints/distance | current fare/rate |
| `current DC kaun hai?` | office role + official office page | current officeholder |
| `aaj mela/event?` | known festivals/event venues | current event listing |
| `aaj weather?` / `kal waterfall jana?` | place coordinates + seasonal climate | Open-Meteo/current forecast |

## Manual dry-run gaps already observed

1. `Ranchi ke bazar` — corpus could not return a practical market list.
2. `Pork kahan miltha hai` — no verified locality-level seller/market coverage.
3. `Muje parents ke sath mandir jana hai` — insufficient accessibility/parking/walking evidence.
4. `Mere ass pass koi mandir hai` -> `Lalpur` — nearby discovery and conversation carry-over required.
5. `Mutton kahan miltha hai` — same meat-market gap as pork.
6. `Child park address bato` — kids-park/address/coordinates coverage missing.
7. `Ranchi ke 5 top restaurant` — restaurant corpus/ranking evidence missing.
8. `Famous food` — food coverage exists but is too narrow for broad Ranchi/Jharkhand discovery.
9. `Deori Mandir kahan hai` — basic identity answerable; exact coordinates/accessibility/parking/timings still need structured evidence.
10. `Rugra kahan milega` — season knowledge exists, but market/locality availability relationships are missing.

## Data-source priority

1. **Official Ranchi/Jharkhand government sources** — authoritative public utilities, police/emergency, civic offices, tourism, transport, institutional lists.
2. **OpenStreetMap** — locality/coordinates and static service/place metadata, always retaining OSM provenance and never treating presence as live inventory.
3. **Jharkhand Tourism / other official specialist sources** — attraction history, access, practical tourism/accessibility facts where stated.
4. **Wikipedia/Wikivoyage/Wikidata/Commons** — background, aliases, travel context, identity, media and cross-links; not authoritative for live status.
5. **Historical books/gazetteers** — history/culture only, never current practical status.
6. **Live adapters** — Open-Meteo today; additional live sources only when terms, reliability and product need justify them.

## Acceptance rule

A query is not considered solved merely because retrieval returns an entity. The result must contain the fact groups necessary to answer the user’s intent. Examples:

- `parents ke sath mandir` requires accessibility/approach evidence, not just a temple name.
- `pork kahan milega` requires an explicitly sourced seller/market relationship, not a generic nearby shop.
- `top 5 restaurant` requires a declared ranking signal; five arbitrary restaurant names do not pass.
- `near me` requires a usable location anchor and spatial relationship.
- live claims must never be inferred from static metadata.
