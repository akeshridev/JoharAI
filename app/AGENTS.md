# AGENTS.md — app

Read root `AGENTS.md` first.

## Scope
Thin Android developer harness and composition root.

## Product UI direction
When product UI work is eventually unfrozen, Johar remains a single-screen conversational app.

Do not introduce tabs, drawers, browse pages, category pages, entity detail screens, or a navigation stack unless the product direction explicitly changes.

The one chat surface may render rich answer components inline, such as entity cards, fact cards, media previews, source/evidence chips, safety warnings, and suggested follow-up prompts.

## Current rules
- Product UI implementation is still frozen. Keep only the existing developer harness until explicitly changed.
- `MainViewModel` consumes `johar-domain` use cases/contracts only.
- `johar-data` imports are allowed only under `com.akeshridev.johar.di` for manual dependency wiring.
- Do not put crawling, Jsoup, Room, WorkManager, or source URLs in Activity/Compose/ViewModel code.
- No navigation architecture is needed for the current harness or the planned single-screen product.

## Current flow
`MainActivity` -> `MainViewModel` -> `ScheduleSourceCrawlUseCase` -> domain scheduler contract.
