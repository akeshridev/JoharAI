# AGENTS.md — app

Read root `AGENTS.md` first.

## Scope
Production branded chat and application composition. The separate MainActivity remains a developer harness.

## Product UI direction
Do not introduce tabs, drawers, browse pages, category pages, entity detail screens, or a navigation stack unless product direction explicitly changes.

The one chat surface may render rich answer components inline, such as entity cards, fact cards, media previews, source/evidence chips, safety warnings, suggested follow-up prompts, comparisons, explicit itineraries, utility cards, maps and routes.

## Current rules
- Keep the product on JoharActivity’s single branded conversation surface.
- The current button triggers the statewide Jharkhand crawl through the domain scheduler; it does not contain crawler logic itself.
- `MainViewModel` consumes `johar-domain` use cases/contracts only except for the temporary local-AI/retrieval developer harness described below.
- Data imports are allowed in `di`, app conversation orchestration/mapping, app map presentation/models, and the existing developer harness. Design-system components never receive retrieval/Room/engine objects.
- Do not put crawling, HTTP, Room, WorkManager, source IDs or retrieval engine objects in Activity/Compose/ViewModel code. Source display names may be derived in the app mapper from evidence already returned by retrieval.
- No navigation architecture is needed.

## Current flow
`MainActivity` -> `MainViewModel` -> `ScheduleSourceCrawlUseCase` -> `SourceCrawlScheduler` -> data WorkManager implementation.

## Existing LLM developer harness
The harness runs retrieval, deterministic answers, frozen retrieval evaluation, and grounded LiteRT-LM inference. `MainActivity` exposes one editable query field so arbitrary Jharkhand questions can be exercised without changing code. `MainViewModel.testOnDeviceLlm(query)` builds RAG context, logs retrieved hits/facts under `JoharLLM`, invokes the retained synthesizer, and logs the generated answer and latency.

`MainViewModel` retains one synthesizer across query runs and closes it in `onCleared`; the harness does not download models. This remains a developer-validation surface, not the final product UI.

## Real chat integration
- `JoharGraph.conversationRouter()` wires `RanchiSpatialEngine`, the deterministic knowledge generator, and `RanchiOfflineRouter` into app-owned `JoharQueryRouter`.
- Keep `JoharAnswer` evidence intact across the app boundary. Do not collapse knowledge answers to plain strings before `JoharContentMapper`; source rows depend on the evidence list.
- `JoharContentMapper` is the only data-to-presentation boundary. It maps grounded answers, spatial results, utility results, comparisons, explicit itineraries and real routes into presentation-only models.
- Text first remains the default. Rich UI appears only when structure helps: one/many places, utilities, route, comparison, explicit itinerary, clarification choices, semantic status or source evidence.
- `JoharChatViewModel` owns messages, thinking state and validated map-action targets. `JoharCardAction` opens an inline `RanchiMapCard`; no product navigation screen is introduced.
- Nearby search requires a resolved, explicitly supplied origin. A pending category supports a query such as `mere aas paas mandir?` followed by a locality choice. Never infer GPS/current user location. Distances are straight-line within 5 km, not road distances.
- Nearby/discovery categories include temples, hospitals/clinics, pharmacies, police/fire, ATM/bank, fuel/EV charging, toilets/parking, restaurants/cafes, hotels, markets, parks, waterfalls/hills, museums/heritage, malls/cinemas, libraries and education. A returned result must still match a real spatial record; category vocabulary is not evidence by itself.
- Utility/service results use compact utility cards; discovery/place results use place cards/carousels. Both use validated map actions from actual resolved results.
- Comparison is allowed only when both named sides resolve to exactly one real place. The comparison card may show only attributes present in spatial data; do not invent a recommendation.
- Explicit itinerary syntax may arrange 2–6 user-supplied, uniquely resolved stops. Preserve requested order. Do not invent stop times, travel times or a supposedly optimal order. Automatic trip planning is a separate future capability.
- Clarification prompts may offer locality chips, but choosing a chip sends that explicit locality back through the same router. Chips do not imply current location.
- Route queries must resolve both origin and destination to exactly one real spatial entity before invoking `RanchiOfflineRouter`. Never guess either endpoint.
- A route card may show distance/duration only when they come from `RanchiRouteResult.Success`. If the routing pack is absent or routing fails, return a contextual unavailable state instead of fake ETA or geometry.
- Route geometry stays in app UI models and may be rendered by `RanchiMapCard`; it must not enter `:johar-design-system`.
- Live/changeable wording such as open now, today, availability, weather, traffic or stock must render as NOT_CONFIRMED unless a live source is actually wired. Offline evidence can provide context but is never proof of current status.
- Weak spatial matches and unsupported requests fall back to the grounded knowledge pipeline. Never fabricate ratings, hours, prices, safety, availability or live status.
- Database construction/querying and map-pack installation run off the UI thread. The map requires `maps/ranchi.pmtiles`; missing packs have a contextual unavailable state. External navigation tries installed handlers and reports when none is available.
- Design catalog is for isolated component work, never product business logic.
- Keep app-level unit tests around query routing behavior. Cover grounded knowledge, strong spatial matches, utility/category routing, nearby follow-up state, weak-match fallback, live-status guardrails, comparison, explicit itinerary, real route parsing and missing-routing-pack behavior without changing frozen retrieval evaluation fixtures.
