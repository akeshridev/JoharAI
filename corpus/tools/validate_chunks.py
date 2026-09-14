#!/usr/bin/env python3
"""Validate a Johar AI processed RAG chunk pack.

The validator is intentionally source-agnostic. It checks provenance shape,
page ranges, chunk IDs, duplicate text, source consistency and word limits,
then writes a compact quality report with representative samples for review.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


REQUIRED_FIELDS = (
    "chunkId",
    "sourceId",
    "title",
    "authorOrPublisher",
    "publicationYear",
    "sourceType",
    "historicalOrCurrent",
    "rightsClass",
    "chapterOrSection",
    "pageStart",
    "pageEnd",
    "districts",
    "entities",
    "topics",
    "text",
    "sourceUrl",
)


def word_count(text: str) -> int:
    return len(text.split())


def load_jsonl(path: Path) -> list[dict]:
    records: list[dict] = []
    with path.open("r", encoding="utf-8") as handle:
        for line_no, line in enumerate(handle, 1):
            if not line.strip():
                continue
            try:
                value = json.loads(line)
            except json.JSONDecodeError as exc:
                raise SystemExit(f"Invalid JSON on line {line_no}: {exc}") from exc
            if not isinstance(value, dict):
                raise SystemExit(f"Line {line_no} is not a JSON object")
            records.append(value)
    return records


def sample(record: dict) -> dict:
    text = record.get("text", "")
    return {
        "chunkId": record.get("chunkId"),
        "chapterOrSection": record.get("chapterOrSection"),
        "pageStart": record.get("pageStart"),
        "pageEnd": record.get("pageEnd"),
        "wordCount": word_count(text),
        "preview": text[:320].replace("\n", " "),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--chunks", required=True, type=Path)
    parser.add_argument("--stats", required=True, type=Path)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--max-words", type=int, default=700)
    parser.add_argument("--min-pages", type=int, default=1)
    parser.add_argument("--min-chunks", type=int, default=1)
    args = parser.parse_args()

    source = json.loads(args.source.read_text(encoding="utf-8"))
    stats = json.loads(args.stats.read_text(encoding="utf-8"))
    records = load_jsonl(args.chunks)

    failures: list[str] = []
    warnings: list[str] = []
    missing_fields = 0
    missing_pages = 0
    invalid_page_ranges = 0
    over_max_words = 0
    source_mismatches = 0
    duplicate_chunk_ids = 0
    duplicate_texts = 0

    chunk_ids: set[str] = set()
    text_hashes: set[str] = set()
    sections: set[str] = set()
    pages: set[int] = set()
    word_counts: list[int] = []

    for index, record in enumerate(records, 1):
        absent = [field for field in REQUIRED_FIELDS if field not in record]
        if absent:
            missing_fields += 1
            failures.append(f"chunk {index}: missing fields {absent}")

        if record.get("sourceId") != source.get("id"):
            source_mismatches += 1

        chunk_id = record.get("chunkId")
        if chunk_id in chunk_ids:
            duplicate_chunk_ids += 1
        elif chunk_id:
            chunk_ids.add(chunk_id)

        text = record.get("text") or ""
        if not text.strip():
            failures.append(f"chunk {index}: empty text")
        text_digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        if text_digest in text_hashes:
            duplicate_texts += 1
        else:
            text_hashes.add(text_digest)

        words = word_count(text)
        word_counts.append(words)
        if words > args.max_words:
            over_max_words += 1

        page_start = record.get("pageStart")
        page_end = record.get("pageEnd")
        if not isinstance(page_start, int) or not isinstance(page_end, int):
            missing_pages += 1
        else:
            if page_start > page_end:
                invalid_page_ranges += 1
            pages.update(range(page_start, page_end + 1))

        section = record.get("chapterOrSection")
        if isinstance(section, str) and section.strip():
            sections.add(section.strip())

    if len(records) < args.min_chunks:
        failures.append(f"only {len(records)} chunks; minimum is {args.min_chunks}")
    if len(pages) < args.min_pages:
        failures.append(f"only {len(pages)} pages represented; minimum is {args.min_pages}")
    if missing_fields:
        failures.append(f"{missing_fields} chunks have missing required fields")
    if missing_pages:
        failures.append(f"{missing_pages} chunks have missing/non-numeric page provenance")
    if invalid_page_ranges:
        failures.append(f"{invalid_page_ranges} chunks have invalid page ranges")
    if over_max_words:
        failures.append(f"{over_max_words} chunks exceed {args.max_words} words")
    if source_mismatches:
        failures.append(f"{source_mismatches} chunks have the wrong sourceId")
    if duplicate_chunk_ids:
        failures.append(f"{duplicate_chunk_ids} duplicate chunk IDs")
    if duplicate_texts:
        failures.append(f"{duplicate_texts} duplicate chunk texts")

    short_records = [record for record in records if word_count(record.get("text") or "") < 250]
    short_chunks = len(short_records)
    if short_chunks:
        warnings.append(
            f"{short_chunks} chunks are below 250 words; allowed at semantic/section boundaries but should be sampled."
        )

    if stats.get("chunksCreated") != len(records):
        failures.append(
            f"stats chunksCreated={stats.get('chunksCreated')} but JSONL has {len(records)} records"
        )
    if stats.get("ragChunkBytes") != args.chunks.stat().st_size:
        failures.append(
            f"stats ragChunkBytes={stats.get('ragChunkBytes')} but file is {args.chunks.stat().st_size} bytes"
        )

    sample_records: list[dict] = []
    if records:
        indexes = sorted({0, len(records) // 2, len(records) - 1})
        sample_records = [sample(records[i]) for i in indexes]

    report = {
        "sourceId": source.get("id"),
        "passed": not failures,
        "chunkCount": len(records),
        "pageCountRepresented": len(pages),
        "pageMin": min(pages) if pages else None,
        "pageMax": max(pages) if pages else None,
        "sectionCount": len(sections),
        "chunkWordMin": min(word_counts) if word_counts else 0,
        "chunkWordMax": max(word_counts) if word_counts else 0,
        "chunkWordAverage": round(sum(word_counts) / len(word_counts), 1) if word_counts else 0,
        "shortChunkCount": short_chunks,
        "shortChunkSamples": [sample(record) for record in short_records],
        "duplicateChunkIdCount": duplicate_chunk_ids,
        "duplicateTextCount": duplicate_texts,
        "missingPageCount": missing_pages,
        "ragChunkBytes": args.chunks.stat().st_size,
        "warnings": warnings,
        "failures": failures,
        "reviewSamples": sample_records,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))

    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
