# Johar AI — Phase 1 Book Ingestion Contract

## Purpose
Convert books and official publications into traceable, retrieval-ready knowledge without mixing historical claims with current facts.

## Output layers

1. `source_manifest` — one record per source/edition.
2. `page_text` — cleaned text with page identity retained.
3. `rag_chunks` — retrieval units with page ranges and topic/entity tags.
4. `structured_facts` — facts suitable for Room when evidence is explicit.
5. `relationships` — source-backed entity relationships.
6. `aliases` — historic spellings, local names, transliterations and common Hinglish variants.

## Required chunk fields

```json
{
  "chunkId": "...",
  "sourceId": "...",
  "title": "...",
  "authorOrPublisher": "...",
  "publicationYear": 1915,
  "sourceType": "BOOK",
  "historicalOrCurrent": "HISTORICAL_PRIMARY_OR_PERIOD_SOURCE",
  "rightsClass": "PUBLIC_DOMAIN",
  "chapterOrSection": "...",
  "pageStart": 120,
  "pageEnd": 122,
  "districts": ["Ranchi"],
  "entities": ["Oraon"],
  "topics": ["customs", "agriculture"],
  "text": "...",
  "sourceUrl": "..."
}
```

## Chunking rules

- Prefer semantic paragraphs/sections over fixed character slices.
- Typical target: 250–700 words per chunk.
- Do not cross chapter boundaries unless a section is extremely short.
- Keep tables as table-like records rather than flattening them into prose when possible.
- Preserve headings because they strongly improve retrieval.
- Preserve page ranges even after cleaning.
- Remove repeated headers, footers, OCR page furniture and duplicate index text.
- Do not remove historical spellings; add normalized aliases separately.

## Historical safety

Historical books may contain obsolete classifications, colonial language, stereotypes or claims that conflict with modern evidence. Therefore:

- retain them as attributed period evidence;
- tag them historical;
- do not rewrite them into unqualified current facts;
- when a modern official source conflicts, current factual answers prefer the modern source while historical questions may surface both.

## Politics / civic data

Political and civic records use the same neutral evidence rules regardless of party or leader. Store source, date, office/term and claim type. Achievements/schemes may be retrieved when relevant, but the corpus must not contain a ranking or preference rule favoring one party.

## Medical / ethnobotanical material

Traditional medicinal-plant uses may be stored as cultural/ethnobotanical knowledge only. Retrieval should phrase them as documented traditional uses, never as medical advice or established treatment efficacy unless a suitable medical source independently supports that claim.

## Size accounting

Count only processed deliverables toward the 200 MiB Phase 1 target:

- cleaned text;
- RAG chunks;
- structured facts/relationships;
- provenance metadata;
- retrieval indexes/embeddings when added.

Raw PDF/image scans are acquisition artifacts and do not count toward the 200 MiB target.

## Acceptance checks per source

A source is `INGESTED` only when:

- rights/reuse status is known enough for our use;
- source metadata is complete;
- extracted text is usable;
- page provenance survives;
- obvious OCR duplication/noise is removed;
- at least a sample of chunks has been manually inspected;
- historical/current classification is assigned;
- no unsupported fact is promoted into Room.
