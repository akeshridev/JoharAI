# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering Jharkhand through trusted local knowledge. V0 is intentionally limited to Dassam Falls.

## Current focus — MODEL ONLY
Development is currently 100% focused on the knowledge/model pipeline.

- Do not build or modify UI unless explicitly requested later.
- Do not add user interaction flows, navigation, screens, buttons, or presentation state.
- The Android app module is only a runtime/bootstrap host while the model pipeline is being developed.
- Prefer unit-testable model/domain/data components over UI-driven testing.

## Current V0 boundary
Johar should answer only questions that help a person decide whether to visit Dassam Falls, reach it, experience it, understand it, stay safe, and return.

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

## Current ingestion direction
V1 runs inside the Android app.

Pipeline:
URL -> fetch/parse source -> clean text -> source-level fact extraction -> validation -> reconcile facts -> canonical knowledge -> Room later.

Important:
- Source facts are not the final canonical record.
- Preserve provenance/evidence for every extracted fact.
- Extract only facts explicitly supported by the source. Missing information stays unknown/null.

## Architecture direction
Follow the principles demonstrated in `akeshridev/AIFriendlyAppArchitecture`:

- Clean MVVM / clean architecture boundaries where relevant.
- Single responsibility per class/component.
- Strict one-way dependencies.
- Model/domain contracts must not depend on Android, network, database, or UI concerns.
- Data implementations own fetching/parsing/persistence details.
- Prefer modular boundaries that let an AI coding agent work in one area without scanning the whole repository.
- Each Gradle module should have its own `AGENTS.md` when introduced; the root `AGENTS.md` acts as the router.
- Never bypass a domain contract just because a shortcut is faster.

## Engineering constraints
- Package: `com.akeshridev.johar`
- Kotlin first.
- Android app only for V1; do not introduce a backend unless explicitly requested.
- Prefer simple, testable components.
- Do not introduce Room, WorkManager, vector search, or on-device LLM code until the current task needs them.
- Do not silently infer facts from source text.

## Token discipline
- Use tokens economically.
- Read only files needed for the current task.
- Do not scan the whole repo unless necessary.
- Do not repeat context already present in AGENTS.md.
- Keep explanations short unless explicitly asked.
- Make only the changes needed to fulfill the current request.
- Avoid speculative refactors or unrelated improvements.

## Collaboration style
Work in small increments. Explain one coding step at a time. Prefer a working thin slice before adding architecture layers.

After every meaningful architecture/focus decision, update the relevant `AGENTS.md` before proceeding with implementation.
