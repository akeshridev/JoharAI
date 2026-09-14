# Johar AI Phase 1 — 200 MB Corpus Progress

Target processed corpus: 200 MiB (209,715,200 bytes)

## Current checkpoint

- Phase 1 has moved from discovery/setup into actual page-aware ingestion.
- First retained processed data is committed from **Santal Folk Tales** (A. Campbell, 1891), using a public-domain machine-readable TEI source pinned to an upstream Git SHA.
- Added a reusable standard-library TEI ingester that preserves numeric page breaks, section headings, source provenance and 250–700 word semantic chunks.
- Ranchi Census Part A and Part B remain the district ingestion templates; direct Census PDF retrieval was unavailable during this checkpoint, so no Census bytes are counted yet.
- No source is marked `INGESTED` merely because a pilot exists. Full-source acceptance checks still apply.

## Measured processed-corpus progress

- Sources with retained processed chunks: **1 partial source**
- Pages processed: **5**
- RAG chunks created: **4**
- Retained processed bytes: **12,108 bytes**
- Retained processed size: **0.0115 MiB / 200 MiB**
- Phase 1 byte progress: **~0.0058%**
- Districts covered by processed district-specific material: **0 / 24**
- Domains represented in retained chunks: **folklore, oral traditions, culture, Santali literature**
- Rejected/low-quality retained material: **0 bytes**

The first checkpoint is intentionally small. Its purpose is to prove the real output contract and byte accounting before scaling ingestion.

## Ingestion quality / blockers

- The Santal source is historical folklore. Narrative content and period commentary must not be promoted into unqualified present-day cultural facts.
- Pages 1–5 are a pilot only; the source remains `READY_TO_INGEST` until the complete volume is processed and manually sampled.
- Census of India catalog metadata remains accessible, but direct PDF retrieval returned gateway failures in the current research environment. This is an acquisition blocker, not a reason to reject the source.
- Raw scans, source mirrors, ingestion code and discovery metadata are not counted toward the 200 MiB target.

## Verified / ready source examples

1. The Mundas and Their Country — Sarat Chandra Roy (1912) — public domain.
2. The Oraons of Chota Nagpur — Sarat Chandra Roy (1915) — public domain; 553-page scan.
3. Santal Folk Tales — A. Campbell (1891) — public domain; machine-readable TEI acquired; partial page-aware ingestion active.
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

- `corpus/tools/ingest_tei.py` — generic page-aware TEI/SGML ingestion tool.
- `corpus/sources/book_santal_folk_tales_1891.json` — pinned source/provenance descriptor.
- `corpus/processed/book_santal_folk_tales_1891/rag_chunks.partial.jsonl` — first retained page-aware chunks.
- `corpus/processed/book_santal_folk_tales_1891/stats.partial.json` — machine-readable pilot stats.
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

## Next ingestion sequence

1. Complete **Santal Folk Tales** through the new TEI ingester and run sample inspection across early/middle/late pages.
2. Ingest **The Mundas and Their Country** from a public-domain machine-readable source if available; otherwise use page-aware extraction from the verified scan.
3. Resume **Ranchi DCHB Part A + Part B** as soon as the source files are retrievable, then use Ranchi as the template for all 24 districts.
4. Add Hazaribag Gazetteer chapters to diversify the corpus beyond folklore/anthropology.

## Milestones

- 25 MiB processed: first end-to-end book corpus usable in retrieval.
- 50 MiB: begin broad cross-domain query evaluation.
- 100 MiB: district + history + culture coverage should be substantial.
- 150 MiB: expand weaker domains and aliases/local vocabulary.
- 200 MiB: freeze Phase 1 and run a large evaluation/tuning cycle.

## Progress accounting rule

Do not count duplicated scans, images or irrelevant OCR toward the target. Progress is based on retained processed knowledge only. Raw source PDFs are acquisition artifacts, not corpus progress.
