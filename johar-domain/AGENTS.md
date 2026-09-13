# AGENTS.md — johar-domain

Read the root `AGENTS.md` first.

## Scope

Pure Kotlin/JVM model module for Johar's knowledge system. It owns domain types and rules only.

## Hard boundaries

- No Android imports.
- No Compose/UI.
- No Jsoup, HTTP, Room, WorkManager, model runtime, or serialization implementation.
- No concrete data-source implementations.
- Keep models independent from how data is fetched, extracted, stored, or displayed.

## Current capability

Source-level fact model for the Dassam Falls v0 ingestion pipeline.

`SourceFact` represents one fact extracted from one source URL and retains provenance/evidence. It is not the canonical merged Dassam record.

## Model rules

- A source fact must preserve source URL, publisher, retrieval time, domain, field, value, and evidence.
- Do not invent facts for missing source content.
- Unknown must remain explicit; never silently convert unknown to false/zero/empty text.
- Keep source facts separate from future derived recommendations/conclusions.
- Extend the model only when a real source/use case requires it; avoid speculative fields.

## Working rule

When this module's responsibility or model contract changes, update this `AGENTS.md` in the same step.
