# Johar AI Agent Context

## Product
Johar AI is an offline-first Android companion for discovering and understanding Jharkhand through trusted local knowledge.

Johar should feel like a micro-ChatGPT whose world is Jharkhand.

## Core product principle
Johar must be useful to people across Jharkhand, including users who prefer speaking over typing, have limited digital literacy, read slowly, use mixed/local language, or have unreliable connectivity.

Design data and presentation around these needs:
- natural spoken questions should work as well as typed questions;
- answers should use simple, direct language before detail;
- support Hindi/Hinglish and local-language vocabulary/aliases as the knowledge base grows;
- prefer short actionable answers, icons, cards, images, audio/voice, and clear choices over dense paragraphs;
- preserve local names and colloquial terms so users do not need formal spelling or terminology;
- offline/local knowledge should remain useful under poor connectivity;
- trust must be visible through source/evidence cues without forcing users to understand technical provenance;
- follow-up questions should feel conversational, not like navigating a database.

The product should reduce the amount of reading, typing, navigation, and technical knowledge required to get a useful answer.

## V1 scope — DATA + PRESENTATION ONLY
For V1, focus only on:
1. Data — discover, crawl, refresh, model, store, retrieve, and source Jharkhand knowledge.
2. Presentation — turn retrieved knowledge into clear conversational answers on the single chat screen, with rich inline answer components when useful.

Do not expand V1 into additional product/platform work unless explicitly requested.

Out of scope for V1 unless explicitly changed:
- multiple screens or navigation
- browse/category/detail pages
- accounts/social/community features
- backend/platform expansion
- unrelated Android infrastructure work
- speculative features not required to collect or present knowledge

## Product interaction invariant
Johar has one primary product surface: a single chat screen.

Users ask naturally in chat; Johar resolves entities/intents and returns conversational answers.

Rich UI is allowed only inside the conversation when useful, for example:
- entity cards
- compact fact cards
- images/video previews
- source/evidence chips
- safety warnings
- suggested follow-up prompts

These are answer components, not separate product destinations.

## Current focus — MODEL / DATA FOUNDATION
Current implementation work should prioritize the knowledge model and packed/local data foundation before presentation runtime.

The existing crawl/Room thin slice may remain as a test harness. Expand runtime pieces only when they directly serve V1 data collection, refresh, storage, retrieval, or presentation.

## Current V0 boundary
Dassam Falls is the first validation entity, not a special-case product boundary.

Knowledge domains currently include:
1. Tourism
2. Family & Accessibility
3. Safety & Emergency
4. History & Culture
5. Food
6. Travel & Logistics
7. Weather & Season Context
8. Facilities
9. Geography

The model must be able to grow to other Jharkhand entity types such as foods, festivals, places, rivers, villages, institutions, and other useful local knowledge without redesigning the core schema.

## Current model goal
Design a generic local knowledge database that can later be packed with source-backed data for any canonical entity, starting with Dassam Falls.

Keep the core model basic and scalable:
- Entity
- Fact
- Relationship
- Source
- Media

Do not collapse these into one giant Dassam record.

## Discovery direction
Entity-specific URLs/IDs should not be hardcoded into the knowledge model. Discovery starts from an entity plus category/search context, and source adapters resolve dynamic IDs/URLs from open sources.

Search terms should be data-driven rather than hardcoded in crawler code.

Maintain a crawl keyword/category table containing terms the crawler can pick from. A crawl task uses entity + category + keyword context, for example:
- entity: Dassam Falls
- category: FOOD
- keyword: local food

Do not crawl a bare keyword globally without entity/location context.

## Self-expanding discovery
The keyword/category table is not static.

During periodic refresh/discovery:
- existing keywords drive source discovery;
- newly discovered useful entities, aliases, topics, and category terms may produce new keyword rows;
- new keywords are normalized/deduplicated before being added;
- each keyword should retain why/how it was discovered and its category/entity context;
- low-value or repeatedly unproductive keywords may later be deprioritized or disabled;
- the crawler periodically revisits both existing knowledge and newly discovered keywords.

This creates a controlled discovery loop:

Entity/category keywords -> crawl -> facts/relationships/media -> discover new terms/entities -> keyword table -> future crawl.

## Category completeness
For every canonical entity, the packed knowledge DB should be able to represent all discovered data across every applicable knowledge domain, not a fixed minimal subset.

The field model must remain extensible so newly discovered source-backed attributes can be added without redesigning the whole database.

## Media direction
Media references may be captured whenever available:
- direct/static image URLs
- Wikimedia Commons media URLs and thumbnails
- YouTube/video page URLs and preview/thumbnail URLs
- source-page image/video links

Store references and metadata by default, not binary image/video payloads. Preserve source/provenance and licensing/attribution when available.

## Local storage budget
Target up to approximately 1.5 GB total local storage when useful. This is a total storage budget, not a Room database target or APK-size target.

Room should primarily contain structured knowledge, source metadata, freshness state, keyword/category discovery state, and indexes. Large raw snapshots or future media caches should be bounded separately.

Periodic refresh should update/replace stale knowledge rather than grow append-only forever.

## Module router

### `johar-domain`
Primary active module. Pure Kotlin/JVM knowledge model and domain rules. Owns entity/fact/provenance/relationship/media/category/discovery concepts and storage-independent contracts.

### `johar-data`
Owns V1 data work: source discovery/crawling, refresh, extraction, persistence, retrieval, and mapping. Existing crawler/Room code is a harness until evolved deliberately around the domain model.

### `app`
Single-screen presentation/composition root only. No navigation architecture.

## Architecture rules
- Single responsibility.
- Strict one-way dependencies.
- Domain model remains framework-free.
- Persistence maps to/from domain concepts.
- Prefer simple explicit concepts over giant catch-all objects.
- Never bypass provenance or entity boundaries for convenience.

## Model correctness
- Every sourced claim retains provenance.
- Unknown is not false, zero, or empty text.
- Conflicting claims from different sources coexist.
- Source facts are not canonical truth.
- Relationships should represent real-world linked entities when appropriate.
- Media references stay separate from binary storage concerns.

## Engineering constraints
- Package root: `com.akeshridev.johar`
- Kotlin first.
- Android-only V1; no backend unless explicitly requested.
- Use free/open sources for core dataset.

## Token discipline
- Use tokens economically.
- Read only files needed for the current decision.
- Keep explanations short unless explicitly asked.
- Avoid speculative implementation work.

## Collaboration style
Work in small increments. Lock one concept at a time, update the relevant `AGENTS.md`, then implement only that concept.
