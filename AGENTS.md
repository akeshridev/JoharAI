# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering Jharkhand through trusted local knowledge. V0 is intentionally limited to Dassam Falls.

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
URL -> fetch HTML -> Jsoup -> clean text -> source-level fact extraction -> validation -> canonical knowledge -> Room later.

Important: source facts are not the final canonical record. Keep provenance for extracted facts.

## Engineering constraints
- Package: `com.akeshridev.johar`
- Kotlin first.
- Android app only for V1; do not introduce a backend unless explicitly requested.
- Prefer simple, testable components.
- Do not introduce Room, WorkManager, vector search, or on-device LLM code until the current task needs them.
- Do not silently infer facts from source text. Missing information stays unknown/null.

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
