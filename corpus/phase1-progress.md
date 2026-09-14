# Johar AI Phase 1 — 200 MB Corpus Progress

Target processed corpus: 200 MiB (209,715,200 bytes)

## Current checkpoint

- Phase 1 now has its first fully accepted source: **Santal Folk Tales** — A. Campbell / Santal Mission Press (1891).
- The public-domain machine-readable TEI source is pinned to upstream Git blob `36b276b4f100395b949751939b929d92fccc80a9`; the ingestion workflow verifies that blob before processing.
- Full page-aware ingestion, structural QC and manual sampling are complete.
- The reusable ingestion path now includes hierarchical TEI section preservation, inline editorial-note preservation, hard 700-word chunk bounds, duplicate detection, page-provenance checks and short-boundary-chunk reporting.
- Ranchi Census Part A and Part B remain the district ingestion templates. Direct Census PDF retrieval was unavailable from the earlier research environment, so no Census bytes are counted yet.

## Measured processed-corpus progress

- Sources acquired and fully processed: **1**
- Fully ingested sources: **1**
- Pages processed: **127**
- RAG chunks created: **84**
- Retained processed bytes: **282,910 bytes**
- Retained processed size: **0.2698 MiB / 200 MiB**
- Phase 1 byte progress: **~0.1349%**
- Districts covered by processed district-specific material: **0 / 24**
- Domains represented in retained chunks: **folklore, oral traditions, culture, Santali literature**
- Rejected/low-quality retained material: **0 bytes**

`retained processed bytes` is the accepted retrieval payload in `rag_chunks.jsonl`. Per-chunk provenance is embedded in that payload. Raw source mirrors, ingestion code, workflow files and QC reports are not counted toward the 200 MiB target.

## First accepted source — Santal Folk Tales

- Source ID: `book_santal_folk_tales_1891`
- Public-domain source: Project Gutenberg / GutenbergSource machine-readable TEI.
- Page range represented: **1–127**
- Paragraphs retained: **320**
- Chunks: **84**
- Chunk size: **53–695 words**, average **512.8 words**
- Retained retrieval payload: **282,910 bytes**
- Sections represented: **32**
- Duplicate chunk IDs: **0**
- Duplicate chunk text: **0**
- Chunks missing page provenance: **0**
- QC failures after final processing: **0**
- Six chunks are below the normal 250-word target because they terminate semantic/story sections; all six were exposed by the QC report and manually reviewed before acceptance.

## Ingestion quality findings

The first full ingestion run was deliberately rejected by QC because one chunk reached 737 words. That exposed a chunker bug where a short partial chunk could absorb another paragraph and exceed the hard limit. The chunker was fixed rather than weakening the acceptance rule.

Manual sampling then found two additional quality problems before source acceptance:

1. Nested story headings such as `I.` / `II.` lacked their parent story title. The parser now preserves hierarchy, for example `The Story of Lelha. — I.`.
2. TEI editorial notes were flattened into adjacent prose, producing strings such as `Lelha.Lelha in Santali means foolish`. Notes are now retained explicitly, for example `Lelha. [Note: Lelha in Santali means foolish.]`.

The corpus was regenerated and revalidated after each fix. These intermediate outputs were not accepted as Phase 1 corpus progress.

## Remaining blockers / cautions

- **Santal Folk Tales is a historical folklore source.** Narrative events and the translator's period commentary must not be promoted into unqualified present-day cultural facts.
- Census of India catalog metadata is known, but Ranchi Part A/B file acquisition remains a blocker until a reliable retrievable copy is available. This is an acquisition problem, not a reason to reject the source family.
- The current corpus is still domain-narrow. Folklore should not dominate the next several ingestion batches; district, geography, anthropology, infrastructure and official material need to grow in parallel.

## Verified / ready source examples

1. Santal Folk Tales — A. Campbell (1891) — **INGESTED**, 127 pages / 84 chunks.
2. The Mundas and Their Country — Sarat Chandra Roy (1912) — public domain.
3. The Oraons of Chota Nagpur — Sarat Chandra Roy (1915) — public domain; 553-page scan.
4. A Santali-English Dictionary — A. Campbell (1899) — public domain; 726 pages; ~34 MB raw scan.
5. A Grammar of the Santhal Language (1873) — public domain; ~390 pages.
6. Hazaribag District Gazetteer — official Jharkhand government chapter PDFs.
7. Census of India 2011 Jharkhand District Census Handbooks — official government publications; district-by-district Part A + Part B ingestion planned.
8. Ranchi DCHB Part A — Village and Town Directory — ready to ingest when file acquisition succeeds.
9. Ranchi DCHB Part B — Primary Census Abstract — ready to ingest when file acquisition succeeds.
10. Jharkhand Tourism publications — official government tourism material.
11. Jharkhand Biodiversity Board publications — medicinal-plant / ethnobotanical material with safety labeling.
12. Jharkhand Mines & Geology publications — minerals, rocks, ores and district geology discovery bucket.

## Repo artifacts supporting ingestion

- `corpus/tools/ingest_tei.py` — generic page-aware TEI/SGML ingestion tool.
- `corpus/tools/validate_chunks.py` — reusable structural/provenance/chunk QC validator.
- `.github/workflows/phase1-ingest-santal.yml` — reproducible pinned-source ingestion workflow for the first book.
- `corpus/sources/book_santal_folk_tales_1891.json` — pinned source/provenance descriptor.
- `corpus/processed/book_santal_folk_tales_1891/rag_chunks.jsonl` — accepted retrieval payload.
- `corpus/processed/book_santal_folk_tales_1891/stats.json` — generated source statistics.
- `corpus/processed/book_santal_folk_tales_1891/quality-report.json` — generated QC report and review samples.
- `corpus/phase1-source-manifest.json` — primary source manifest.
- `corpus/phase1-source-discovery-02.json` — verified Census/biodiversity source batch.
- `corpus/phase1-ingestion-contract.md` — canonical ingestion and acceptance rules.
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

1. Acquire and ingest **The Mundas and Their Country** using the same page-aware acceptance path, preferring a public-domain machine-readable edition when available.
2. Ingest **Hazaribag District Gazetteer** chapter-by-chapter to add district/history/geography/administration coverage and avoid over-concentrating Phase 1 in folklore.
3. Resume **Ranchi DCHB Part A + Part B** as soon as a reliable source file is retrievable, then use Ranchi as the template for all 24 districts.
4. Add **The Oraons of Chota Nagpur** once page-aware extraction is established for its large public-domain scan.

## Milestones

- 25 MiB processed: first diverse corpus usable end to end.
- 50 MiB: begin broad cross-domain query evaluation.
- 100 MiB: district + history + culture coverage should be substantial.
- 150 MiB: expand weaker domains and aliases/local vocabulary.
- 200 MiB: freeze Phase 1 and run a large evaluation/tuning cycle.

## Progress accounting rule

Do not count duplicated scans, images or irrelevant OCR toward the target. Progress is based on retained processed knowledge only. Raw source PDFs are acquisition artifacts, not corpus progress.
