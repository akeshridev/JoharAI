# Real chat integration

## Runtime flow

`JoharActivity → JoharChatViewModel → JoharQueryRouter → spatial / knowledge / offline routing → JoharContentMapper → JoharContent → branded inline component`

The app owns intent routing and presentation mapping. `:johar-design-system` remains presentation-only and never receives Room, retrieval, routing-engine or source-adapter objects.

## Integrated answer families

### 1. Grounded knowledge

Examples: `Rugra kya hai?`, `Sarhul kya hai?`, `Tagore Hill history`.

- Deterministic offline knowledge returns a full `JoharAnswer`, not only its text.
- `JoharAnswer.evidence` survives into `JoharContentMapper` so source domains can appear below the natural-language answer.
- No evidence means no fake source row.
- Normal knowledge stays text-first.

### 2. Current/live-status guardrail

Examples: `Pahari Mandir open now?`, `weather today`, `is this available now?`.

- Current/changeable wording is classified before ordinary knowledge rendering.
- Offline context may still be shown, but the answer is marked NOT_CONFIRMED.
- Offline data never proves live opening status, stock, traffic, weather, timing or availability.

### 3. Named place lookup

Examples: `Tagore Hill kahan hai?`, `Pahari Mandir`, `Ranchi railway station`.

- All meaningful place-name tokens must match a real spatial result.
- A weak spatial rank is not treated as confidence.
- One result renders a place card; several render the existing compact carousel.
- Map actions are registered only for actual resolved results.

### 4. Nearby and category discovery

Examples: `Lalpur ke paas hospital`, `Kanke ke paas cafe`, `Ranchi me waterfall`.

Supported category vocabulary now covers temples, hospitals/clinics, pharmacies, police/fire, ATM/banks, fuel/EV charging, toilets/parking, restaurants/cafes, hotels, markets, parks, waterfalls/hills, museums/heritage, malls/cinemas, libraries and schools/colleges/universities.

Rules:

- Nearby always needs an explicit resolved origin.
- `mere aas paas ...` asks for a locality/landmark and keeps one pending category for the next reply.
- Clarification chips send an explicit locality back through the normal router; they never infer GPS.
- Search radius is 5 km and displayed distance is straight-line unless a road route is actually calculated.
- Empty nearby data says coverage is missing; it does not claim that no such place exists.
- Utility/service categories render compact utility cards; discovery categories render place cards/carousels.

### 5. Offline map

`View on Map` appends an inline `RanchiMapCard` for a previously validated place action.

Required asset:

`app/src/main/assets/maps/ranchi.pmtiles`

The map pack is Ranchi-only and runtime stays offline. External Navigate delegates to installed navigation/geo handlers.

### 6. Offline road routing

Examples: `Tagore Hill se Ranchi railway station kaise jaye?`, `route from Tagore Hill to Ranchi station`.

- Both endpoints must resolve uniquely before routing.
- `RanchiOfflineRouter` loads the bundled Ranchi OSM road graph and runs A*.
- Route distance, duration and geometry are shown only from `RanchiRouteResult.Success`.
- The route geometry renders on the same offline PMTiles map.
- Android packaging may store the generated gzip graph as `assets/routing/ranchi-routing-v1.tsv`; the loader accepts the packaged `.tsv` form and the original `.tsv.gz` form and detects gzip by stream header instead of filename.

Generated routing source asset:

`app/src/main/assets/routing/ranchi-routing-v1.tsv.gz`

Observed packaged APK entry:

`assets/routing/ranchi-routing-v1.tsv`

### 7. Place comparison

Examples: `Tagore Hill vs Rock Garden`, `compare Tagore Hill versus Rock Garden`.

- Both sides must resolve to exactly one real place.
- The comparison uses only known spatial attributes such as type/area/map availability.
- Johar does not invent a winner or recommendation when evidence does not justify one.

### 8. Explicit itinerary composition

Examples: `plan: Tagore Hill, Rock Garden`, `itinerary Tagore Hill and Rock Garden and Pahari Mandir`.

- Supports 2–6 user-supplied stops.
- Every stop must resolve uniquely.
- Requested order is preserved.
- The card does not invent visit times, drive times or claim route optimization.
- Automatic recommendation-based trip planning is a separate future planner capability.

## Presentation mapping

`JoharContentMapper` is the explicit boundary between real runtime objects and reusable UI models.

Current content families:

- `Text`
- `Grounded` + optional source rows
- `Places`
- `Utilities`
- `Map`
- `Route`
- `Itinerary`
- `Comparison`
- `Clarification`
- `Info`

The UI rule remains: **Johar is a conversation, not a dashboard.** Rich components appear only when structure or action improves the answer.

## Runtime assets

Two independent offline assets are required for the complete spatial experience:

- `maps/ranchi.pmtiles` — basemap rendering.
- `routing/ranchi-routing-v1.tsv.gz` source / packaged routing graph — road calculation.

PMTiles working does not prove routing is installed; routing working does not replace the map pack.

## Validation sequence

Before the broad manual command matrix:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:installDebug
```

Then verify at least one query from each family: grounded knowledge, live guardrail, named place, discovery, utility nearby, clarification follow-up, map, route, comparison and explicit itinerary.

After that baseline is green, run the planned ~200-command matrix and record failures by class instead of patching individual phrases blindly.

## Known boundaries

- No GPS/current-location inference.
- No automatic live web fallback yet.
- No claim of current hours/availability/weather/traffic without a live verified provider.
- Retrieval/spatial coverage still bounds answer quality.
- Explicit itinerary composition is not route optimization.
- Comparison is factual structure, not an unsupported recommendation engine.
- Chat/pending context lives in ViewModel and does not survive process death.
