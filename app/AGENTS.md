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
- `MainViewModel` consumes `johar-domain` use cases/contracts only except for the temporary local-AI/retrieval developer harness described below.
- `johar-data` imports are allowed under `com.akeshridev.johar.di`; the current model/retrieval harness is a temporary explicit exception while local AI is being validated.
- Do not put crawling, HTTP, Room, WorkManager, source URLs, or source IDs in Activity/Compose/ViewModel code.
- No navigation architecture is needed.

## Current flow
`MainActivity` -> `MainViewModel` -> `ScheduleSourceCrawlUseCase` -> `SourceCrawlScheduler` -> data WorkManager implementation.

## Existing LLM developer harness
The harness runs retrieval, deterministic answers, frozen retrieval evaluation, and grounded LiteRT-LM inference. `MainActivity` exposes one editable query field so arbitrary Jharkhand questions can be exercised without changing code. `MainViewModel.testOnDeviceLlm(query)` builds RAG context, logs retrieved hits/facts under `JoharLLM`, invokes the retained synthesizer, and logs the generated answer and latency.

`MainViewModel` retains one synthesizer across query runs and closes it in `onCleared`; the harness does not download models. This remains a developer-validation surface, not the final product UI.
