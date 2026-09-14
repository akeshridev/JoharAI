# Johar Design System

Reusable Compose design system for Johar — Ranchi AI.

## Modules

- `:johar-design-system` — tokens, theme, primitives and reusable Johar cards.
- `:johar-design-catalog` — standalone mock-data Android app for visual review.

The design system intentionally has no dependency on Room, retrieval, routing, MapLibre, or the LLM stack.

## Run the catalog

From Android Studio, choose the `johar-design-catalog` run configuration and launch it on an emulator/device.

Or from the command line:

```bash
./gradlew :johar-design-catalog:installDebug
```

The catalog contains mock Ranchi scenarios for place cards, nearby results, routes, itineraries, map previews, local picks, utilities, comparisons, trust states, clarification chips, constraints, sources and suggestions.

## Design rule

Johar is conversation-first. Use natural text for simple answers. Render structured cards only when they help the user compare, verify, navigate or take another action.

## Integration boundary

Application/domain data should be mapped to design-system presentation models before rendering.

Example:

```text
RanchiSpatialPlace -> app UI mapper -> JoharPlaceCardModel -> JoharPlaceCard
```

Do not make the design-system module depend on data-layer entities.
