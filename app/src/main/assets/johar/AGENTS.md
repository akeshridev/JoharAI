# AGENTS.md — app/src/main/assets/johar

Read the repository root and `app/AGENTS.md` first.

## Purpose
This directory contains versioned offline knowledge boosters applied on top of the prebuilt Room seed. Boosters are a prototype/content-release mechanism, not a place to fabricate answers or bypass retrieval.

## Current sequence
`JoharBoosterLoader` applies, once per working database, in this order:
1. `johar-booster-2026.09-v1.json`
2. `johar-booster-2026.09-v2.json`
3. `johar-booster-2026.09-v3-spatial.json`
4. `johar-booster-2026.09-v4-ranchi-essentials.json`
5. `johar-booster-2026.09-v5-visitor-essentials.json`

V4/V5 were added after the initial Golden 100 common-user run exposed large evidence gaps in food aliases, transport, education, health/emergency, visitor places and practical Ranchi queries.

## Content rules
- Every factual claim must carry `sourceUrl`, `publisher`, `freshness`, and `evidence`.
- Prefer Government of Jharkhand, District Ranchi, Jharkhand Forest/Tourism, official institutions, and structured public sources. Use weaker sources only when necessary and label them accurately.
- Static/slow facts are suitable. Never treat a packaged fact as proof of current opening status, traffic, fares, stock, temporary closure or availability.
- Aliases are identity/query metadata. They may encode common spellings/local names, but must not turn into unsupported factual claims.
- Existing entities are enriched by normalized name; aliases are merged rather than replaced. New entities use stable `booster:<key>` IDs.
- Coordinates are for spatial/routing identity and should come from an identifiable source. Do not invent precision.
- Do not add records solely to make a test green. Add them because they represent useful, defensible prototype knowledge.

## Golden 100 rule
The Golden 100 is a product-quality loop. Missing source evidence remains a DATA_GAP. The goal is useful grounded coverage, not a synthetic score. After a content batch, rerun `python3 tests/golden/run.py` and inspect both summary and diagnostics before adding the next batch.
