# AGENTS.md — app

Read root `AGENTS.md` first.

## Scope
Production branded chat and application composition. The separate MainActivity remains a developer harness.

## Product UI direction
Do not introduce tabs, drawers, browse pages, category pages, entity detail screens, or a navigation stack unless product direction explicitly changes.

The one chat surface may render rich answer components inline, such as entity cards, fact cards, media previews, source/evidence chips, safety warnings, and suggested follow-up prompts.

## Current rules
- Keep the product on JoharActivity’s single branded conversation surface.
- The current button triggers the statewide Jharkhand crawl through the domain scheduler; it does not contain crawler logic itself.
- `MainViewModel` consumes `johar-domain` use cases/contracts only except for the temporary local-AI/retrieval developer harness described below.
- Data imports are allowed in `di`, app conversation orchestration/mapping, app map presentation/models, and the existing developer harness. Design-system components never receive retrieval/Room/engine objects.
- Do not put crawling, HTTP, Room, WorkManager, source URLs, or source IDs in Activity/Compose/ViewModel code.
- No navigation architecture is needed.

## Current flow
`MainActivity` -> `MainViewModel` -> `ScheduleSourceCrawlUseCase` -> `SourceCrawlScheduler` -> data WorkManager implementation.

## Existing LLM developer harness
The harness runs retrieval, deterministic answers, frozen retrieval evaluation, and grounded LiteRT-LM inference. `MainActivity` exposes one editable query field so arbitrary Jharkhand questions can be exercised without changing code. `MainViewModel.testOnDeviceLlm(query)` builds RAG context, logs retrieved hits/facts under `JoharLLM`, invokes the retained synthesizer, and logs the generated answer and latency.

`MainViewModel` retains one synthesizer across query runs and closes it in `onCleared`; the harness does not download models. This remains a developer-validation surface, not the final product UI.

## Real chat integration
- `JoharGraph.conversationRouter()` wires `RanchiSpatialEngine`, the deterministic knowledge generator, and `RanchiOfflineRouter` into app-owned `JoharQueryRouter`.
- `JoharContentMapper` maps real spatial and route results to presentation-only card models. Text first; one place gets a card, multiple places a compact carousel.
- `JoharChatViewModel` owns messages, thinking state and validated map-action targets. `JoharCardAction` opens an inline `RanchiMapCard`; no product navigation screen is introduced.
- Nearby search requires a resolved, explicitly supplied origin. A pending category supports `mere aas paas mandir?` followed by `Lalpur`. Never infer GPS/current user location. Distances are computed straight-line distances within 5 km, not road distances.
- Route queries must resolve both origin and destination to exactly one real spatial entity before invoking `RanchiOfflineRouter`. Never guess either endpoint.
- A route card may show distance/duration only when they come from `RanchiRouteResult.Success`. If the routing pack is absent or routing fails, return a contextual unavailable text response instead of fake ETA or geometry.
- Route geometry stays in app UI models and may be rendered by `RanchiMapCard`; it must not enter `:johar-design-system`.
- Weak spatial matches and unsupported requests fall back to the unchanged knowledge pipeline. Never fabricate ratings, hours, prices, safety, availability or live status.
- Database construction/querying and map-pack installation run off the UI thread. The map requires `maps/ranchi.pmtiles`; missing packs have a contextual unavailable state. External navigation tries installed handlers and reports when none is available.
- Design catalog is for isolated component work, never product business logic.
- Keep app-level unit tests around query routing behavior. Cover text-first knowledge queries, strong spatial matches, nearby follow-up state, weak-match fallback, live-status guardrails, real route parsing, and missing-routing-pack behavior without changing frozen retrieval evaluation fixtures.
