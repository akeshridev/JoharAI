# Johar Design System Agent Guide

## Purpose
`johar-design-system` is the reusable visual layer for Johar. It owns brand tokens, theme, reusable Compose primitives, cards, splash visuals, and the Johar nagada mascot/loading states.

## Design principles
- Johar is conversation-first, not a dashboard.
- Prefer natural text unless structure materially improves understanding or action.
- Keep the visual language warm, local, trustworthy, and clearly rooted in Ranchi/Jharkhand without becoming touristy or decorative.
- Reuse a small number of strong component families instead of creating a unique card for every response type.
- Keep the splash, mascot, loader, cards, and typography in one coherent visual system.

## Module boundary
This module may contain:
- color, typography, spacing, shape, and elevation tokens
- `JoharTheme`
- buttons, chips, badges, source rows, and status components
- place, route, itinerary, info, discovery, comparison, utility, and suggestion components
- `JoharSplashScreen`
- nagada drummer mascot/loading components
- UI-only presentation models used by reusable components

This module must not depend on:
- Room entities or DAOs
- retrieval/RAG code
- Gemma/LiteRT code
- routing/spatial engine classes
- ViewModels or conversation orchestration
- live/network data sources

Application/domain data must be mapped into design-system presentation models outside this module.

## Density and chat usage
- Use `COMPACT` variants inside chat feeds and carousels when repeated content would otherwise feel heavy.
- Use `STANDARD` for a primary actionable result.
- Reserve `FEATURED` for rare hero states.
- Do not wrap every Johar response in a card.

## Trust and freshness
- `Live`, `Verified`, `Offline`, `Not confirmed`, and warning states should only appear when they change user trust or action.
- Never use visual badges to imply fresh/current information unless the application layer has actually verified it.

## Brand motion
- The drummer/nagada mascot is Johar's branded thinking/searching state.
- Use it for AI thinking, local search, route planning, or itinerary generation.
- Do not use it as a generic spinner for every background task.
- Keep motion subtle and readable at small sizes.

## Testing
Use `:johar-design-catalog` as the visual playground with mock data before changing production screens.

Run locally with:

```bash
./gradlew :johar-design-catalog:installDebug
```

Then validate the production integration through `:app`.

## Change discipline
When adding a component:
1. Prefer extending an existing primitive/card family first.
2. Add or update a catalog example.
3. Check light and dark themes.
4. Check compact mobile width and touch targets.
5. Keep the public API independent of app/data implementation classes.

## Product integration
App owns conversation orchestration and mapping from `RanchiSpatialEngine` into these UI models. Card actions are opaque presentation events handled by app. The real map stays in app; catalog map examples are not a production map implementation. Keep the catalog limited to isolated visual component work.
