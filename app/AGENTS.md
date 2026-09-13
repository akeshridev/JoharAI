# AGENTS.md — app

Read root `AGENTS.md` first.

## Scope
Thin Android developer harness and composition root.

## Rules
- Product UI is out of scope. Keep only the single crawl trigger button until explicitly changed.
- `MainViewModel` consumes `johar-domain` use cases/contracts only.
- `johar-data` imports are allowed only under `com.akeshridev.johar.di` for manual dependency wiring.
- Do not put crawling, Jsoup, Room, WorkManager, or source URLs in Activity/Compose/ViewModel code.
- No navigation or presentation architecture beyond what this developer trigger requires.

## Current flow
`MainActivity` -> `MainViewModel` -> `ScheduleSourceCrawlUseCase` -> domain scheduler contract.
