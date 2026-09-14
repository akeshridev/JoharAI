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
- Wikimedia Commons scan metadata reports 662 pages and approximately 45 MB.
- Treat historical descriptions as historical evidence, not current demographic/cultural claims.

**The Oraons of Chota Nagpur — Sarat Chandra Roy, 1915**
- Public-domain scan available through Wikimedia Commons / Internet Archive.
- Coverage: history, economic life, social organization, customs and regional geography.
- A Commons scan is over 500 pages; raw scan size is much larger than processed text.

**The Birhors — Sarat Chandra Roy, 1925**
- Digitized through Internet Archive/Open Library.
- Coverage: Birhor society, customs, folklore and material culture.

**The Tribes and Castes of Bengal: Ethnographic Glossary — H. H. Risley, 1892**
- Two volumes; public-domain copies indexed by The Online Books Page with Archive.org/Google/HathiTrust editions.
- Use selectively for Jharkhand-region communities and historical terminology.
- High caution for period-specific colonial classification and language.

### Historical district / place knowledge

**Bengal District Gazetteers: Singhbhum, Saraikela and Kharsawan**
- Digitized PDF available from IGNCA.
- Useful for historical geography, settlements, routes, rivers, administration, communities, economy and heritage.

**Bengal / Bihar and Orissa District Gazetteer: Hazaribagh — E. Lister, 1917**
- British Library catalogue record confirms the historical district gazetteer.
- Use digitized/public copy when available.

**Bengal District Gazetteers: Ranchi — historical statistical volumes**
- British Library catalogue records include Ranchi statistics volumes from 1901/02 and 1900/01–1910/11.
- Useful as historical administrative/statistical context when a digitized copy is accessible.

### Modern official district knowledge

**Census of India 2011 — Jharkhand District Census Handbooks**
- Directorate of Census Operations, Jharkhand.
- Part A: Village and Town Directory.
- Part B: Primary Census Abstract.
- These volumes include demographic, village/town, infrastructure, education, medical, transport, communication, water, electricity and administrative context.
- Initial verified district landing pages include Ranchi, Deoghar, Pashchimi Singhbhum, Saraikela Kharsawan, Lohardaga, Dhanbad, Bokaro, Garhwa, Sahibganj and Simdega.
- Extend to all 24 Jharkhand districts.

### Tourism / modern context

**Jharkhand Tourism — Nature’s Hidden Jewel**
- Official Department of Tourism e-magazine / tourism publication.
- Useful for destinations, tourism circuits, official descriptions and travel framing.

**Jharkhand Tourism Policy 2021 and official tourism downloads**
- Use for tourism policy, definitions, circuits and official development context; not as a substitute for destination facts when more direct sources exist.

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
