# Johar AI Phase 1 — 200 MB Corpus Progress

Target processed corpus: 200 MiB (209,715,200 bytes)

## Current checkpoint

- Source catalog is active and machine-readable manifests exist.
- Added a second verified source batch focused on Census and biodiversity material.
- Added a formal ingestion contract covering chunk shape, provenance, historical/current separation, political neutrality, ethnobotanical safety and acceptance checks.
- Census of India Jharkhand District Census Handbooks are confirmed as a major structured source family; Part A provides Village/Town Directory data and Part B provides Primary Census Abstract data.
- Ranchi Part A and Part B are explicitly queued `READY_TO_INGEST` as templates before expanding district by district.
- Jharkhand Biodiversity Board material is queued for medicinal-plant / ethnobotanical / forest-knowledge coverage, with cultural-use claims kept separate from medical efficacy claims.
- Current stage: source acquisition + ingestion setup + district-by-district queue construction.

## Verified / ready source examples

1. The Mundas and Their Country — Sarat Chandra Roy (1912) — public domain.
2. The Oraons of Chota Nagpur — Sarat Chandra Roy (1915) — public domain; 553-page scan.
3. Santal Folk Tales — Andrew Campbell (1891) — public domain.
4. A Santali-English Dictionary — A. Campbell (1899) — public domain; 726 pages; ~34 MB raw scan.
5. A Grammar of the Santhal Language (1873) — public domain; ~390 pages.
6. Hazaribag District Gazetteer — official Jharkhand government chapter PDFs.
7. Census of India 2011 Jharkhand District Census Handbooks — official government publications; district-by-district Part A + Part B ingestion planned.
8. Ranchi DCHB Part A — Village and Town Directory — ready to ingest.
9. Ranchi DCHB Part B — Primary Census Abstract — ready to ingest.
10. Jharkhand Tourism publications — official government tourism material.
11. Jharkhand Biodiversity Board publications — medicinal plants and ethnobotanical knowledge queued with safety labeling.
12. Jharkhand Mines & Geology publications — minerals, rocks, ores and district geology discovery bucket.

## New repo artifacts

- `corpus/phase1-source-manifest.json` — primary source manifest.
- `corpus/phase1-source-discovery-02.json` — verified Census/biodiversity source batch.
- `corpus/phase1-ingestion-contract.md` — canonical rules for producing page-aware RAG chunks and structured facts.
- `docs/phase1-book-corpus.md` — overall Phase 1 strategy and source mix.

## Coverage buckets

- Tourism / historical tourism
- District gazetteers and census handbooks
- Tribes / anthropology
- Folklore / oral traditions
- Santali and other local languages / vocabulary
- Food / forest produce / agriculture
- Art / music / dance / crafts
- Rivers / forests / wildlife / medicinal plants
- Geology / mining / minerals
- Heritage / archaeology / historical routes
- Villages / towns / civic infrastructure
- Neutral civic / political history with source + date

## Milestones

- 25 MiB processed: first end-to-end book corpus usable in retrieval.
- 50 MiB: begin cross-domain query evaluation.
- 100 MiB: substantial district/history/culture coverage.
- 150 MiB: fill weak domains and local-language aliases.
- 200 MiB: freeze Phase 1 and run large evaluation/tuning cycle.

## Progress accounting rule

Do not count duplicated scans, images or irrelevant OCR toward the target. Progress is based on retained processed knowledge only. Raw source PDFs are acquisition artifacts, not corpus progress.
