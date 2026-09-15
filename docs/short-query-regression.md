# Short-query retrieval and discovery regression

## Root cause and history

- `2850190` added `!hasSupportedIntent && !hasNamedEntityMatch -> emptyList()` before scoring. It checked canonical names only, although scoring already used aliases. Complete stored aliases therefore stopped working unless another intent happened to admit the query.
- `74fcf7c` explicitly added school/FACILITY/ORGANIZATION/SCHOOL hints. `4dae815` retained these and expanded other categories. Neither removed school support. `0dd6d20` only added coordinates to retrieval hits.
- The chat discovery path (expanded in `204ad57`) recognized SCHOOL but called name/alias search, then fell back to generic knowledge when no spatial match survived. Category metadata alone could not discover a school with a different name.

## Change and ownership

`johar-data` now admits complete normalized stored aliases using the same phrase boundary and minimum length checks as names, and applies the identity score to aliases too. No food phrase list was added. Unsupported queries still require a known intent or corpus identity. Facts and source URLs remain unchanged.

`RanchiSpatialEngine` supplies category discovery over enabled, Ranchi-scoped coordinate-bearing records. Spatial type uses sourced `joharPackType` when present, otherwise the broad entity type. This preserves distinctions such as SCHOOL versus FACILITY. Discovery filters before limiting and does not require the category word in a name.

`app` wires this lookup into the existing bare-category branch. Missing category results receive a contextual offline response with no invented places. Existing live, nearby-origin and route handling retain their ordering.

## Corpus limitation

Read-only inspection of the checked-in `johar-base-2026.09.db` found 370 entities, no puchka/golgappa identities or evidence, and no school entities. A Chhau description mentioning a school is not an educational facility. Thus this change does not promise positive food/school answers on a fresh seed-only installation. Positive answers require supported records in the working database. The user's earlier installed database was not available for comparison; the alias-gate defect is established from source history, not an observed device database trace.

## Focused regression tests

- `ShortQueryRegressionTest`: pani puri, puchka, phuchka, golgappa, PANI-PURI, puchka kya hai; source URL preservation; no corpus; partial aliases; write python code and quantum computing; school retrieval and Ranchi-scoped metadata discovery.
- `JoharQueryRouterTest`: school, schools, list schools in Ranchi; missing-category response; food aliases remain grounded; school open now stays NOT_CONFIRMED. Existing nearby, weak-match, route, comparison and itinerary tests run in the same class.
- Tests use explicitly labeled fixtures; fixture entities are not added to the product corpus.

Run only:

```sh
./gradlew :johar-data:testDebugUnitTest --tests '*ShortQueryRegressionTest' :app:testDebugUnitTest --tests '*JoharQueryRouterTest'
```

No frozen evaluation IDs, 1000-case suite, heap settings or corpus assets are changed by this fix.

Validation result: focused Gradle run passed (3 data tests + 13 router tests; zero failures/errors). `git diff --check` passed. No device/database replay or full 1000-case run was performed. Existing unrelated workspace changes were left intact.
