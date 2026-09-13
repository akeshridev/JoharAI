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
Design the knowledge model that will later back a prepacked local database for Dassam Falls.

The model must represent these concepts separately:
1. Canonical entity — e.g. Dassam Falls, Kanchi River, Taimara village, Ranchi Railway Station.
2. Source document/snapshot — one retrieved source with publisher, URL and retrieval metadata.
3. Source fact/claim — one source-backed claim about an entity.
4. Evidence/provenance — exact support for the claim.
5. Entity relationship — e.g. waterfall LOCATED_NEAR village, waterfall FED_BY river.
6. Media asset/reference — image/video associated with a canonical entity, with source, attribution, license and optional thumbnail/preview metadata.
7. Canonical/resolved fact — future selected/normalized knowledge derived from source claims.
8. Derived recommendation — future conclusions such as suitable-for-elderly; never mix these with source facts.

`SourceFact` is an initial model and may be refactored as these concepts become explicit.

## Model rules
- Every source claim must preserve provenance.
- A source fact is not canonical truth.
- Conflicting source claims must be representable simultaneously.
- Unknown must remain explicit; never silently convert unknown to false/zero/empty text.
- Prefer typed/normalized values while retaining source wording/evidence.
- If a value is itself a real-world thing (river, village, hospital, food place, attraction), prefer an entity reference/relationship over a plain string when useful.
- Media is a first-class concept, not just a raw URL string on a place.
- Media references may point to direct/static images, Wikimedia Commons assets, YouTube/video pages, source-page media, thumbnails, or previews.
- At this stage store references + metadata only, not binary image/video payloads.
- Media must retain source URL and, when available, creator/attribution, license, license URL, MIME type, dimensions/duration and preview/thumbnail URL.
- Do not assume media is reusable merely because it is publicly reachable; licensing/attribution metadata must be preserved when available.
- Avoid a giant DassamFalls data class containing every domain.
- Avoid Room-driven modeling; persistence maps to the model later.
- Add fields/types only when they serve a real knowledge requirement.

## Frozen capabilities
Existing crawl scheduling types may remain for the current harness, but do not expand runtime ingestion contracts while MODEL ONLY is active unless explicitly requested.

## Working rule
For each model decision: update this file if the boundary/rule changes, then implement that single concept.
