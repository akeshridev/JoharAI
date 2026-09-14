# Johar AI — Phase 1 Book Corpus

## Goal
Build an approximately 200 MB high-density Jharkhand knowledge corpus from diverse books and official publications before large-scale query experimentation.

The target is useful knowledge, not raw byte count. Raw scans may be much larger than the final corpus. The 200 MB target refers to the processed knowledge payload: extracted text, cleaned chunks, structured facts/relationships, provenance metadata, and later retrieval index material.

## Source principles

1. Prefer public-domain, open-access, government, or user-provided books.
2. Preserve exact provenance: title, author/publisher, year, chapter/section, page, source URL, extraction timestamp.
3. Historical sources are historical evidence. They must not be used to assert current opening hours, road conditions, fees, current political roles, live facilities, or other volatile facts.
4. Colonial-era ethnographic works may contain outdated or biased terminology. Preserve source meaning for historical research but do not present stereotypes or historical classifications as current truth.
5. Conflicting source-backed facts may coexist. Do not silently merge disagreement into one unsupported statement.
6. Current political/civic knowledge must remain non-partisan and source-backed. The same evidence standard applies regardless of party or leader.

## Phase 1 coverage mix

The corpus should deliberately span:
- historical tourism and travel;
- district gazetteers and census handbooks;
- tribal communities and anthropology;
- history and regional movements;
- festivals, religion, customs and folklore;
- food and agriculture;
- art, music, dance and craft;
- geography, rivers, forests, wildlife and geology;
- mining and industrial history;
- archaeology and heritage;
- villages, towns and local infrastructure;
- languages and local vocabulary;
- neutral civic/political history.

## Initial vetted source set

### Tribal history / anthropology

**The Mundas and Their Country — Sarat Chandra Roy, 1912**
- Public-domain scan available through Wikimedia Commons / Internet Archive.
- Strong coverage for Munda history, geography, social organization, customs and Chota Nagpur context.
- Treat historical descriptions as historical evidence, not current demographic/cultural claims.

**The Oraons of Chota Nagpur — Sarat Chandra Roy, 1915**
- Public-domain scan available through Wikimedia Commons / Internet Archive.
- Wikimedia Commons metadata identifies a 553-page scan of approximately 320.81 MB and marks the scan/publication as public domain.
- Coverage: history, economic life, social organization, customs and regional geography.
- Wikisource also exposes page-level text, useful as a secondary extraction/check source.

**Oraon Religion and Customs — Sarat Chandra Roy**
- Public-domain digitized edition available through Wikisource/Wikimedia sources.
- Strong complementary coverage for religious practice, ritual, folklore and customary life.
- Keep period-specific terminology in provenance and normalize only in user-facing summaries.

**The Birhors — Sarat Chandra Roy, 1925**
- Public-domain digitized copies are available online.
- Coverage: Birhor society, customs, folklore, material culture, hunting/forest life and social organization.
- Use the original/public-domain edition rather than relying on later copyrighted reprints.

**The Tribes and Castes of Bengal: Ethnographic Glossary — H. H. Risley, 1892**
- Public-domain copies exist through major digital-book repositories.
- Use selectively for Jharkhand-region communities and historical terminology.
- High caution for period-specific colonial classification and language.

### Historical district / place knowledge

**Bengal District Gazetteers: Singhbhum, Saraikela and Kharsawan**
- Useful for historical geography, settlements, routes, rivers, administration, communities, economy and heritage.
- Prefer a public-domain/government-hosted digitized copy and preserve edition/year metadata.

**Bengal / Bihar and Orissa District Gazetteer: Hazaribagh — E. Lister, 1917**
- Historical district gazetteer; useful for geography, administration, settlements, economy, communities, routes and notable places.
- Use a public-domain digitized copy when available.

**Hazaribag District Gazetteer — Government of Jharkhand digital edition**
- The official Hazaribag district website currently provides the gazetteer as Introduction, Index, 17 chapters, Appendix, subject index and picture PDF downloads.
- This is a particularly good ingestion target because it is official, chapter-separated and already exposed as discrete PDFs.
- Ingest chapter-by-chapter so page/source provenance remains exact.

**Ranchi historical gazetteers/statistical volumes**
- Useful for historical administrative/statistical context, settlements, infrastructure and regional history.
- Only ingest copies whose rights and provenance are clear.

### Modern official district knowledge

**Census of India 2011 — Jharkhand District Census Handbooks**
- Directorate of Census Operations, Jharkhand.
- Part A: Village and Town Directory.
- Part B: Primary Census Abstract.
- These volumes include demographic, village/town, infrastructure, education, medical, transport, communication, water, electricity and administrative context.
- Extend to all 24 Jharkhand districts.

### Tourism / modern context

**Jharkhand Tourism official publications**
- Use official tourism magazines, downloadable brochures, destination publications and policy material where available.
- Useful for destinations, tourism circuits, official descriptions and travel framing.
- Current operational details such as fees/hours still require freshness handling.

**Jharkhand Tourism Policy 2021 and official tourism downloads**
- Use for tourism policy, definitions, circuits and official development context; not as a substitute for destination facts when more direct sources exist.

## Acquisition priority

### Batch A — highest value first
1. Hazaribag official District Gazetteer, all chapters.
2. The Mundas and Their Country.
3. The Oraons of Chota Nagpur.
4. Oraon Religion and Customs.
5. The Birhors.
6. Singhbhum/Saraikela/Kharsawan historical gazetteers.
7. Ranchi historical gazetteer/statistical material.
8. Census 2011 District Census Handbooks for all 24 districts.

### Batch B — breadth expansion
- Santhal/Santal history and folklore sources.
- Ho community history/language sources.
- Kharia, Asur, Paharia and other Jharkhand community references.
- Chota Nagpur regional history and freedom-movement material.
- Forest, flora, wildlife and ethnobotany books.
- Geology, minerals and mining-history publications.
- Archaeology, megalith and heritage studies.
- Folk music, dance, Sohrai/Khovar and craft references.
- Food, forest produce and agricultural traditions.
- Hindi/Nagpuri/Khortha/Kurukh/Mundari/Santali language and vocabulary material where rights permit.

### Batch C — civic/political history
- Government reports, assembly/government historical records, election-statistics publications, scheme documents and official biographies.
- Store date, office, government, source and time period explicitly.
- No party receives preferential inclusion, ranking or wording.

## Processing model

```text
Book / official publication
    ↓
metadata + rights classification
    ↓
text extraction / OCR only when necessary
    ↓
page-aware cleanup
    ↓
section/chapter segmentation
    ↓
Jharkhand relevance filtering
    ↓
RAG chunks (page provenance retained)
    ↓
entity mention / alias extraction
    ↓
source-backed facts + relationships
    ↓
quality checks
    ↓
book corpus pack
```

## Chunk record

Every retained chunk should carry at least:

```text
sourceId
title
authorOrPublisher
publicationYear
sourceType
rightsClass
historicalOrCurrent
chapterOrSection
pageStart
pageEnd
text
entities[]
topics[]
sourceUrl
```

## Historical/current classification

- `HISTORICAL_PRIMARY_OR_PERIOD_SOURCE` — old gazetteers, historical monographs, period travel accounts.
- `HISTORICAL_REFERENCE` — later works describing past events.
- `CURRENT_OFFICIAL` — current government/official publications.
- `CURRENT_REFERENCE` — current open reference work.

Historical sources can answer questions such as "What did a 1917 gazetteer say about Hazaribagh?" but cannot establish current availability or present-day status without a current source.

## Political / civic rule

Political knowledge is factual, source-backed and non-partisan. Do not train the system to favor or suppress a political party. Store achievements, schemes, offices, historical events and civic contributions with source and date, and apply the same inclusion and confidence rules regardless of party or leader.

## Phase 1 size milestones

- 25 MB: first diverse corpus usable end to end.
- 50 MB: begin broad cross-domain query evaluation.
- 100 MB: district + history + culture coverage should be substantial.
- 150 MB: expand weaker domains and aliases/local vocabulary.
- ~200 MB: freeze Phase 1, build a large evaluation set, then tune retrieval and grounded answer synthesis.

Do not pad the corpus to hit a size milestone. A smaller high-quality pack is preferable to duplicated or irrelevant text.

## Source tracking

A machine-readable acquisition manifest lives at `corpus/phase1-source-manifest.json`. Every source should move through statuses such as `DISCOVERED`, `RIGHTS_VERIFIED`, `READY_TO_INGEST`, `INGESTED`, `REJECTED`, with notes explaining any exclusion or historical-use restriction.
