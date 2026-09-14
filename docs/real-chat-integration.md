# First real chat integration

## Flow

`JoharActivity → JoharChatViewModel → JoharQueryRouter → existing spatial/knowledge services → JoharContentMapper → JoharContent`

- Knowledge answers use the existing deterministic generator unchanged. No LLM is invoked.
- Named place lookup requires all meaningful name tokens to match an actual spatial result; weak results fall back to knowledge. Station/junction and temple/mandir normalization is deliberately small.
- One place renders an intro and place card. Multiple results use the existing compact carousel, capped at five.
- Place cards contain source-record name, region, description and type only. Computed nearby distance is labelled straight-line. No invented hours, ratings, prices, ETA, live status or user location.
- App validates map action IDs against prior resolved results, then appends an inline `RanchiMapCard`. Design system remains presentation-only.
- Nearby temple/hospital/park/market queries use an explicitly resolved origin and a 5 km radius. Missing/ambiguous origins prompt for a locality; one pending category supports a locality-only reply. This is bounded discovery, not exhaustive coverage.
- Room initialization and lookups run on IO. UI state changes stay on main. Existing splash/header/composer/thinking state and auto-scroll remain.

## Review examples (not runtime verified)

- `Rugra kya hai?` → knowledge text.
- `Tagore Hill kahan hai?`, `Pahari Mandir`, `Ranchi railway station` → place card if a strong spatial match exists.
- `Ranchi me mandir` → compact results when supported by spatial records.
- `mere aas paas mandir?` → origin question; `Lalpur` → nearby results or a clear data-gap answer.
- `Lalpur ke paas mandir` → same nearby path with supplied origin.
- `Pahari Mandir open now?` → unconfirmed live status plus existing offline knowledge.
- Unknown/unsupported queries → existing knowledge fallback.

## Validation

Run `./gradlew :app:assembleDebug :app:lintDebug` from the repository root. The missing standard Gradle 9.3.1 wrapper was restored so this command is reproducible. No new test classes are included, per user instruction. No app launch/runtime checks were performed for this milestone.

## Remaining gaps

- `app/src/main/assets/maps/ranchi.pmtiles` is absent. Map action state is real, but the map shows unavailable until a real pack is provided. See `app/src/main/assets/maps/README.md`.
- Navigation delegates to installed navigation/geo intent handlers and reports missing handlers; device behavior is unverified.
- Retrieval/spatial coverage and existing corpus quality limit answers. Matching is conservative, not a full multilingual intent/alias classifier.
- Chat history and pending origin survive configuration changes via ViewModel, not process death.
- No GPS, live data, itinerary generation, new route planning, or LLM integration was added.

Next milestone: supply and validate the real Ranchi map pack, then validate supported chat queries and map actions on a device.
