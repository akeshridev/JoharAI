# Golden 100 prototype hardening — pass 1

## Baseline
The first real Android pipeline run of the curated Golden 100 produced 28 PASS, 53 DATA_GAP, 17 PARSER/INTENT_GAP, 1 ROUTING_FAIL, 1 UX_FALLBACK, and 0 SAFETY_FAIL. The working database snapshot contained 395 entities and 209 facts.

This hardening pass does not rewrite the Golden 100 or the frozen 1000 benchmark. It attacks the causes visible in diagnostics.

## Problems addressed
1. Common aliases such as puchka/golgappa/pittha and typo variants needed durable entity identity, not query-specific router branches.
2. Broad category questions could have source-backed entities without coordinates, but the spatial path treated them as if no useful data existed.
3. Named-place detail questions could fall into broad category answer logic and mix unrelated entities.
4. The prototype lacked source-backed Ranchi essentials across transport, education, hospitals/emergency, visitor places and family-oriented parks.
5. Existing booster entities could not receive new aliases because booster enrichment previously left `aliasesJson` unchanged.

## Runtime changes
- `JoharBoosterLoader` now merges normalized aliases when enriching an existing entity and applies V4/V5 boosters once per working database.
- `JoharQueryRouter` falls back to source-backed knowledge when a category has useful non-spatial records but no coordinate-bearing place cards.
- `JoharQueryRouter.uniquePlace` may accept a single resolved coordinate candidate after conservative strong matching fails. This supports unique stored aliases for routing while still refusing ambiguity.
- `DeterministicJoharAnswerGenerator` separates broad-category lists from named-entity questions.
- Detail questions prefer a matching source fact. If the entity exists but that attribute is not supported, Johar returns a contextual `NO_ANSWER` rather than an unrelated fact.

## Source-backed prototype content
V4/V5 add or enrich high-value common-user knowledge including:
- pani puri / puchka / phuchka / golgappa / gup chup identity;
- Daal Pitha/Pittha and Karam/Karma aliases;
- Ranchi Junction, Hatia station, Birsa Munda Airport and Khadgarha bus stand;
- Pahari Mandir steps, Tagore Hill, Rock Garden, Kanke Dam, Birsa Biological Park, Ranchi Science Centre, Nakshatra Van, Ranchi Lake/Bada Talab, Patratu Valley, Sidhu-Kanhu Park and Biodiversity Park;
- selected Ranchi schools/colleges/universities;
- RIMS, Sadar Hospital, Central Institute of Psychiatry, emergency helplines and selected police stations;
- Nagpuri language identity.

Priority sources are official Government of Jharkhand, District Ranchi, Jharkhand Tourism/Forest Department and official institutions. Structured public sources are used selectively for identity/coordinates when an official source is not sufficient.

## Grounding rules
This is not test-fixture stuffing. Every booster fact carries publisher, source URL, freshness and evidence. Missing facts remain gaps. Static packaged knowledge never confirms current opening, live traffic, fares, stock, temporary closure or availability.

Alias metadata is an internal identity aid, not external evidence. A common spelling can resolve an entity without pretending that a cited page explicitly documents every spelling variant.

## Validation
Focused regression tests cover stored food aliases, category metadata discovery, a broad school answer, entity-specific Pahari Mandir step answering, unknown accessibility handling, unsupported-query gating and trace invariance.

Android/device validation is still required after this pass. Run:

```sh
python3 -m unittest discover -s tests/golden -p 'test_*.py'
./gradlew :johar-data:testDebugUnitTest :app:testDebugUnitTest
python3 tests/golden/run.py
```

Do not run the full frozen 1000 suite until the Golden 100 failure clusters have materially improved.

## Next decision after rerun
Compare the new Golden 100 summary to the 28/100 baseline. Prioritize remaining clusters by user value, especially daily-life spatial services, visitor discovery language, and the remaining route endpoint failure. Do not chase the score by fabricating records.
