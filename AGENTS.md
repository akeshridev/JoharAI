# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering Jharkhand through trusted local knowledge. V0 is intentionally limited to Dassam Falls.

## Current focus — MODEL ONLY
Development is now 100% focused on the knowledge model and the shape of the packed local database.

Freeze runtime implementation work unless explicitly requested:
- No new UI/product flows.
- No crawler expansion.
- No WorkManager changes.
- No network/parser work.
- No LLM integration.
- No answer-generation work.

The existing crawl/Room thin slice may remain in the repo as a test harness, but do not extend it while MODEL ONLY is active.

## Current V0 boundary
Johar should model only knowledge that helps a person decide whether to visit Dassam Falls, reach it, experience it, understand it, stay safe, and return.

Knowledge domains:
1. Tourism
2. Family & Accessibility
3. Safety & Emergency
4. History & Culture
5. Food
6. Travel & Logistics
7. Weather & Season Context
8. Facilities
9. Geography

## Current model goal
Design a local knowledge database that can later be packed with source-backed Dassam Falls data and iterated without changing the core model.

The model must separate:
- canonical entities
- source documents/snapshots
- source facts/claims
- provenance/evidence
- relationships between entities
- future canonical/resolved facts
- future derived recommendations

Do not collapse these into one giant Dassam record.

## Module router

### `johar-domain`
Primary active module. Pure Kotlin/JVM knowledge model and domain rules. Owns entity/fact/provenance/relationship concepts and storage-independent contracts. No Android, UI, network, parser, Room, WorkManager, or LLM implementation. Read `johar-domain/AGENTS.md` before changing it.

### `johar-data`
Currently frozen except when needed to validate the model against Room constraints later. Existing crawler/Room code is a harness, not the design authority. The domain model drives persistence shape, not the reverse.

### `app`
Frozen developer harness only. No product UI work.

## Architecture rules
Follow the principles demonstrated in `akeshridev/AIFriendlyAppArchitecture`:
- Single responsibility.
- Strict one-way dependencies.
- Domain model must remain framework-free.
- Persistence shapes must map to/from the domain model; Room annotations never enter domain types.
- Prefer small explicit concepts over a giant catch-all model.
- Never bypass provenance or entity boundaries for convenience.

## Model correctness
- Every sourced claim must retain provenance.
- Unknown is not false, zero, or empty text.
- Conflicting claims from different sources must coexist.
- Source facts are not canonical truth.
- Canonical/resolved knowledge is a later layer derived from source facts.
- Nearby places, hospitals, foods, villages, rivers, etc. should be modeled as entities/relationships when appropriate, not flattened into arbitrary strings.
- Source-specific wording/evidence must remain available even if a normalized value is also stored.
- The model should support a prepacked local DB later, but must not be coupled to Room.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open sources for eventual core dataset.
- No vector search, canonical merge algorithm, or LLM work while MODEL ONLY is active.

## Token discipline
- Use tokens economically.
- Read only files needed for the current model decision.
- Do not scan the whole repo unless necessary.
- Keep explanations short unless explicitly asked.
- Make only changes needed for the current request.
- Avoid speculative implementation work.

## Collaboration style
Work in small increments. Lock one model concept at a time, update the relevant `AGENTS.md`, then implement only that concept.
