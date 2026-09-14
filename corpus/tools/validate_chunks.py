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
from collections import Counter
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
        "pageReferenceType": record.get("pageReferenceType"),
        "wordCount": word_count(text),
        "preview": text[:480].replace("\n", " "),
    }


def evenly_spaced_indexes(length: int) -> list[int]:
    if length <= 0:
        return []
    fractions = (0.0, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0)
    return sorted({min(length - 1, round((length - 1) * fraction)) for fraction in fractions})


def page_samples(records: list[dict], page_min: int | None, page_max: int | None) -> list[dict]:
    if not records or page_min is None or page_max is None:
        return []
    if page_max <= page_min:
        targets = [page_min]
    else:
        targets = [
            round(page_min + (page_max - page_min) * fraction)
            for fraction in (0.05, 0.15, 0.3, 0.5, 0.7, 0.85, 0.95)
        ]

    usable = [
        record
        for record in records
        if isinstance(record.get("pageStart"), int) and isinstance(record.get("pageEnd"), int)
    ]
    chosen: list[dict] = []
    used_ids: set[str] = set()
    for target in targets:
        containing = [
            record
            for record in usable
            if record["pageStart"] <= target <= record["pageEnd"]
        ]
        if containing:
            record = min(containing, key=lambda item: word_count(item.get("text") or ""))
        else:
            record = min(
                usable,
                key=lambda item: min(abs(item["pageStart"] - target), abs(item["pageEnd"] - target)),
            )
        chunk_id = record.get("chunkId")
        if chunk_id in used_ids:
            continue
        used_ids.add(chunk_id)
        value = sample(record)
        value["targetPage"] = target
        chosen.append(value)
    return chosen


def short_chunk_page_bands(short_records: list[dict], page_min: int | None, page_max: int | None) -> list[dict]:
    if page_min is None or page_max is None or not short_records:
        return []
    span = max(1, page_max - page_min + 1)
    band_count = 5
    bands: list[dict] = []
    for index in range(band_count):
        start = page_min + (span * index) // band_count
        end = page_min + (span * (index + 1)) // band_count - 1
        if index == band_count - 1:
            end = page_max
        count = sum(
            1
            for record in short_records
            if isinstance(record.get("pageStart"), int)
            and start <= record["pageStart"] <= end
        )
        bands.append({"pageStart": start, "pageEnd": end, "shortChunkCount": count})
    return bands


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
    section_counts: Counter[str] = Counter()

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
            section = section.strip()
            sections.add(section)
            section_counts[section] += 1

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

    page_min = min(pages) if pages else None
    page_max = max(pages) if pages else None
    position_records = [sample(records[i]) for i in evenly_spaced_indexes(len(records))]

    report = {
        "sourceId": source.get("id"),
        "passed": not failures,
        "chunkCount": len(records),
        "pageCountRepresented": len(pages),
        "pageMin": page_min,
        "pageMax": page_max,
        "sectionCount": len(sections),
        "mostFrequentSections": [
            {"section": section, "chunkCount": count}
            for section, count in section_counts.most_common(12)
        ],
        "chunkWordMin": min(word_counts) if word_counts else 0,
        "chunkWordMax": max(word_counts) if word_counts else 0,
        "chunkWordAverage": round(sum(word_counts) / len(word_counts), 1) if word_counts else 0,
        "shortChunkCount": short_chunks,
        "shortChunkPageBands": short_chunk_page_bands(short_records, page_min, page_max),
        "shortChunkSamples": [sample(record) for record in short_records[:30]],
        "duplicateChunkIdCount": duplicate_chunk_ids,
        "duplicateTextCount": duplicate_texts,
        "missingPageCount": missing_pages,
        "ragChunkBytes": args.chunks.stat().st_size,
        "warnings": warnings,
        "failures": failures,
        "positionSamples": position_records,
        "pageSamples": page_samples(records, page_min, page_max),
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))

    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
