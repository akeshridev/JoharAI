# AGENTS.md — app

Read root `AGENTS.md` first.

## Scope
Thin Android developer harness and composition root today; eventual V1 product remains one conversational screen.

## Product UI direction
Do not introduce tabs, drawers, browse pages, category pages, entity detail screens, or a navigation stack unless product direction explicitly changes.

The one chat surface may render rich answer components inline, such as entity cards, fact cards, media previews, source/evidence chips, safety warnings, and suggested follow-up prompts.

## Current rules
- Product presentation work is not part of the crawler implementation step. Keep the developer harness minimal.
- The current button triggers the statewide Jharkhand crawl through the domain scheduler; it does not contain crawler logic itself.
- `MainViewModel` consumes `johar-domain` use cases/contracts only.
- `johar-data` imports are allowed only under `com.akeshridev.johar.di` for manual dependency wiring.
- Do not put crawling, HTTP, Room, WorkManager, source URLs, or source IDs in Activity/Compose/ViewModel code.
- No navigation architecture is needed.

## Current flow
`MainActivity` -> `MainViewModel` -> `ScheduleSourceCrawlUseCase` -> `SourceCrawlScheduler` -> data WorkManager implementation.
