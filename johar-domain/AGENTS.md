# AGENTS.md — johar-domain

Read the root `AGENTS.md` first.

## Scope
Pure Kotlin/JVM model module for Johar's knowledge system. It owns domain types, contracts, and use cases only.

## Hard boundaries
- No Android imports.
- No Compose/UI.
- No Jsoup, HTTP, Room, WorkManager, model runtime, or serialization implementation.
- No concrete data-source implementations.
- Keep models independent from how data is fetched, extracted, stored, or displayed.

## Current capabilities
1. Source-level fact model for the Dassam Falls v0 ingestion pipeline.
2. Crawl scheduling contract used by presentation without exposing WorkManager/data implementation.

`SourceFact` represents one fact extracted from one source URL and retains provenance/evidence. It is not the canonical merged Dassam record.

`CrawlTarget` identifies a canonical crawl target. `SourceCrawlScheduler` is the domain-facing scheduling contract. `ScheduleSourceCrawlUseCase` is the presentation entry point.

## Model rules
- A source fact must preserve source URL, publisher, retrieval time, domain, field, value, and evidence.
- Do not invent facts for missing source content.
- Unknown must remain explicit; never silently convert unknown to false/zero/empty text.
- Keep source facts separate from future derived recommendations/conclusions.
- Domain crawl targets must not contain source URLs or WorkManager details.
- Extend the model only when a real source/use case requires it; avoid speculative fields.

## Working rule
When this module's responsibility or model contract changes, update this `AGENTS.md` in the same step.
