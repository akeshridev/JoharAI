# Tourist simulation gap ledger

Purpose: capture product/data gaps discovered by asking Johar realistic tourist questions. This is separate from the frozen retrieval benchmark. The goal is usefulness, not benchmark tuning.

## Evaluation rule

For each scenario classify the problem before changing code or data:

- `DATA_GAP` — the offline corpus does not contain enough verified evidence.
- `RETRIEVAL_GAP` — evidence exists but the retriever does not surface it.
- `RANKING_GAP` — relevant results exist but poor candidates rank above them.
- `STRUCTURE_GAP` — useful prose exists but practical fields are not modeled/searchable.
- `FRESHNESS_GAP` — the question needs current/live evidence that static data cannot prove.
- `CONTEXT_GAP` — location or prior-turn constraints are lost.
- `UX_POLICY_GAP` — Johar refuses too early, overuses online fallback, or asks unnecessary questions.
- `ANSWER_GAP` — retrieval is good but the deterministic/LLM answer is weak, unsupported, or unclear.

Do not change the frozen retrieval eval merely to make these scenarios pass.

## Gaps observed from manual tourist dry-run

| ID | Tourist query / flow | Gap | What Johar needs | Priority |
|---|---|---|---|---|
| G001 | `Ranchi ke bazar` | DATA_GAP + STRUCTURE_GAP | Market/haat entities by locality; market type; day; timings; what is sold; coordinates; provenance | P0 |
| G002 | `Pork kahan miltha hai` | DATA_GAP + FRESHNESS_GAP | Pork-oriented butcher/market discovery with locality/address; distinguish static seller metadata from live stock | P0 |
| G003 | `muje parents ke sath mandir jana hai` | STRUCTURE_GAP + RANKING_GAP | Temple accessibility, stairs/walking, parking, elderly suitability, approach road; constraint-aware ranking | P0 |
| G004 | `Mutton kahan miltha hai` | DATA_GAP + FRESHNESS_GAP | Meat market/butcher entities, locality/address, offering type; never claim current inventory from static metadata | P0 |
| G005 | `mere ass pass koi mandir hai` | CONTEXT_GAP + STRUCTURE_GAP | Location-aware retrieval using coordinates/locality and POI coordinates; distance ranking | P0 |
| G006 | follow-up `Lalpur` after nearby-temple query | CONTEXT_GAP | Carry previous intent and apply Lalpur as the missing location instead of restarting | P0 |
| G007 | `child park address bato` | DATA_GAP + STRUCTURE_GAP | Parks by locality, exact address/coordinates, play area/kids facilities, practical access metadata | P0 |
| G008 | `ranchi ke 5 top restaurnt` | DATA_GAP + RANKING_GAP | Restaurant entities, cuisine, locality, family friendliness, price, coordinates plus a dated ranking/review signal | P0 |
| G009 | `famous food` | DATA_GAP | Broader verified Jharkhand dish corpus with aliases, ingredients, region, season, cultural context and sources | P1 |
| G010 | `deri mandir kahan hai` | STRUCTURE_GAP | Exact address/coordinates, distance references, parking/accessibility and source-backed static facts for Deori Mandir | P0 |
| G011 | `rugra kahan milega` | DATA_GAP + FRESHNESS_GAP | Seasonal food-to-market relationships, locality, typical months, source date; live availability only with fresh evidence | P0 |
| G012 | repeated `Online search karun?` on weak queries | UX_POLICY_GAP | Attempt offline partial answer first; online fallback only when the unresolved part truly requires fresh/current data | P0 |
| G013 | broad/underspecified queries such as `Ranchi ke bazar` | UX_POLICY_GAP | Return useful offline candidates first, then offer compact refinement chips/choices instead of refusing | P1 |
| G014 | practical tourist questions generally | STRUCTURE_GAP | Address, coordinates, distance, access, parking, walking/stairs, family/kids suitability, timing and season should be first-class searchable facts | P0 |

## Immediate data priority implied by the dry-run

Before adding much more historical-book volume, prioritize practical local coverage for Ranchi and then expand district by district:

1. POIs with coordinates/address: temples, parks, museums, waterfalls, viewpoints, hospitals, police.
2. Local markets/haats with day/time/type/offerings and locality.
3. Food availability relationships: dish/produce -> market/locality -> season.
4. Restaurants and food outlets with cuisine/locality/price/family metadata, with dated source signals for rankings.
5. Accessibility/elderly/kids/parking/approach attributes for tourist POIs.
6. Neighborhood/locality entities such as Lalpur, Harmu, Doranda, Kanke and HEC for location-aware retrieval.
7. A clear static-vs-live policy for opening status, stock, weather, event timing, fares and other rapidly changing facts.

## Automated suite

`app/src/main/assets/tourist-eval.jsonl` contains realistic tourist scenarios. `TouristSimulationEvaluator` runs them against the current offline retriever and records signals such as:

- no offline evidence,
- thin candidate coverage,
- wrong result type,
- missing practical fact groups,
- location-context requirement,
- live-freshness requirement.

This suite is a coverage diagnostic, not a ground-truth benchmark. A result marked `PARTIAL` means retrieval found something but the evidence is not rich enough for the tourist need.
