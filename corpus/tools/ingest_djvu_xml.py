#!/usr/bin/env python3
"""Convert Internet Archive DjVu OCR XML into Johar AI RAG chunks.

Designed for public-domain scanned books. When IA scandata contains reliable
numeric printed-page labels, those are used. Otherwise the ingester preserves
exact 1-based scan/PDF page indices and labels that provenance explicitly.

OCR section detection is intentionally conservative: a missed subheading is
safer for retrieval than promoting a footnote, index entry, or running prose
line into a false section label.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

SPACE_RE = re.compile(r"\s+")
PAGE_NUMBER_RE = re.compile(r"^\s*(\d+)\s*$")
STRUCTURAL_HEADING_RE = re.compile(
    r"^(?:CHAPTER|PART|BOOK|SECTION|APPENDIX|INTRODUCTION|PREFACE|ADDENDA|CORRIGENDA)\b",
    re.IGNORECASE,
)
FOOTNOTE_LIKE_RE = re.compile(r"^(?:[*†‡§]|[a-z]\s+(?:vide|see)\b)", re.IGNORECASE)
TRAILING_PAGE_RE = re.compile(r"\s+[\[(]?(?:\d{1,3}|[ivxlcdm]{1,8})[\])]?[.,;:]?\s*$", re.IGNORECASE)
MIN_PRINTED_PAGE_LABELS = 50


@dataclass
class Paragraph:
    section: str
    page_start: int
    page_end: int
    text: str


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1].upper()


def clean_text(text: str) -> str:
    return SPACE_RE.sub(" ", text.replace("\u00ad", "")).strip()


def word_count(text: str) -> int:
    return len(text.split())


def load_printed_page_map(scandata_path: Path) -> dict[int, int]:
    """Map IA leafNum -> printed numeric page number when scandata provides it."""
    root = ET.parse(scandata_path).getroot()
    mapping: dict[int, int] = {}
    for page in root.iter():
        if local_name(page.tag) != "PAGE":
            continue
        raw_leaf = page.attrib.get("leafNum") or page.attrib.get("leafnum")
        if raw_leaf is None or not raw_leaf.isdigit():
            continue
        for child in page.iter():
            if local_name(child.tag) == "PAGENUMBER" and child.text:
                match = PAGE_NUMBER_RE.match(child.text)
                if match:
                    mapping[int(raw_leaf)] = int(match.group(1))
                    break
    return mapping


def paragraph_text(node: ET.Element) -> str:
    lines: list[str] = []
    for line in node.iter():
        if local_name(line.tag) != "LINE":
            continue
        words: list[str] = []
        for word in line.iter():
            if local_name(word.tag) == "WORD" and word.text:
                value = clean_text(word.text)
                if value:
                    words.append(value)
        if words:
            lines.append(" ".join(words))
    if not lines:
        words = [
            clean_text(word.text or "")
            for word in node.iter()
            if local_name(word.tag) == "WORD"
        ]
        return clean_text(" ".join(word for word in words if word))
    return clean_text(" ".join(lines))


def normalize_heading(text: str) -> str:
    text = clean_text(text)
    # Scanned books often repeat a chapter/running heading followed by the
    # printed page number. Keep the semantic title, not the furniture.
    stripped = TRAILING_PAGE_RE.sub("", text).strip()
    return stripped or text


def looks_like_heading(text: str) -> bool:
    text = clean_text(text)
    words = text.split()
    if not (1 <= len(words) <= 14) or len(text) > 120:
        return False
    if FOOTNOTE_LIKE_RE.match(text):
        return False

    letters = [c for c in text if c.isalpha()]
    if len(letters) < 4:
        return False

    if STRUCTURAL_HEADING_RE.match(text):
        return True

    # Generic OCR headings must be overwhelmingly uppercase. Deliberately do
    # not infer headings from Title Case: footnotes, index terms and names are
    # commonly Title Case and previously caused severe over-segmentation.
    upper_ratio = sum(c.isupper() for c in letters) / len(letters)
    digit_ratio = sum(c.isdigit() for c in text) / max(len(text), 1)
    return upper_ratio >= 0.82 and digit_ratio <= 0.12


def useful_text(text: str) -> bool:
    if len(text) < 20:
        return False
    printable = sum(
        ch.isalnum() or ch.isspace() or ch in ".,;:'\"!?()-[]/"
        for ch in text
    )
    return printable / max(len(text), 1) >= 0.90


def compile_start_patterns(source: dict) -> list[re.Pattern[str]]:
    extraction = source.get("extraction") or {}
    patterns = extraction.get("contentStartPatterns") or []
    return [re.compile(pattern, re.IGNORECASE) for pattern in patterns]


def matches_any(text: str, patterns: list[re.Pattern[str]]) -> bool:
    return any(pattern.search(text) for pattern in patterns)


def read_ocr_paragraphs(
    djvu_path: Path,
    printed_page_map: dict[int, int],
    use_scan_pages: bool,
    start_patterns: list[re.Pattern[str]],
) -> tuple[list[Paragraph], dict]:
    paragraphs: list[Paragraph] = []
    current_section = "Page-aware OCR"
    object_index = -1
    objects_seen = 0
    pages_seen: set[int] = set()
    pages_without_text = 0
    rejected_paragraphs = 0
    content_started = not start_patterns
    content_start_page: int | None = None
    pages_skipped_before_start = 0

    for _, elem in ET.iterparse(djvu_path, events=("end",)):
        if local_name(elem.tag) != "OBJECT":
            continue
        object_index += 1
        objects_seen += 1

        if use_scan_pages:
            page_number = object_index + 1
        else:
            page_number = printed_page_map.get(object_index)
            if page_number is None:
                elem.clear()
                continue

        page_paragraphs: list[str] = []
        for node in elem.iter():
            if local_name(node.tag) != "PARAGRAPH":
                continue
            text = paragraph_text(node)
            if not text:
                continue
            if useful_text(text):
                page_paragraphs.append(text)
            else:
                rejected_paragraphs += 1

        if not content_started:
            start_index: int | None = None
            for index, text in enumerate(page_paragraphs):
                if matches_any(clean_text(text), start_patterns):
                    start_index = index
                    break
            if start_index is None:
                pages_skipped_before_start += 1
                elem.clear()
                continue
            content_started = True
            content_start_page = page_number
            page_paragraphs = page_paragraphs[start_index:]

        pages_seen.add(page_number)
        if not page_paragraphs:
            pages_without_text += 1
            elem.clear()
            continue

        for text in page_paragraphs:
            if looks_like_heading(text):
                current_section = normalize_heading(text)
                continue
            paragraphs.append(
                Paragraph(
                    section=current_section,
                    page_start=page_number,
                    page_end=page_number,
                    text=text,
                )
            )
        elem.clear()

    if start_patterns and not content_started:
        raise SystemExit("Configured OCR contentStartPatterns were never matched")

    diagnostics = {
        "scanObjectsSeen": objects_seen,
        "printedPageLabelsAvailable": len(printed_page_map),
        "pageReferenceType": "SCAN_PAGE_1_BASED" if use_scan_pages else "PRINTED_PAGE",
        "contentStartPage": content_start_page,
        "pagesSkippedBeforeContentStart": pages_skipped_before_start,
        "pagesMapped": len(pages_seen),
        "pagesWithoutUsableText": pages_without_text,
        "ocrParagraphsRejectedAsNoise": rejected_paragraphs,
    }
    return paragraphs, diagnostics


def split_long_paragraph(paragraph: Paragraph, max_words: int) -> list[Paragraph]:
    words = paragraph.text.split()
    if len(words) <= max_words:
        return [paragraph]
    return [
        Paragraph(
            section=paragraph.section,
            page_start=paragraph.page_start,
            page_end=paragraph.page_end,
            text=" ".join(words[start : start + max_words]),
        )
        for start in range(0, len(words), max_words)
    ]


def chunk_paragraphs(
    paragraphs: Iterable[Paragraph], min_words: int, max_words: int
) -> list[list[Paragraph]]:
    expanded: list[Paragraph] = []
    for paragraph in paragraphs:
        expanded.extend(split_long_paragraph(paragraph, max_words))

    chunks: list[list[Paragraph]] = []
    current: list[Paragraph] = []
    current_words = 0
    current_section: str | None = None

    def flush() -> None:
        nonlocal current, current_words, current_section
        if current:
            chunks.append(current)
        current = []
        current_words = 0
        current_section = None

    for paragraph in expanded:
        words = word_count(paragraph.text)
        if current and paragraph.section != current_section:
            flush()
        if current and current_words + words > max_words:
            flush()
        current.append(paragraph)
        current_words += words
        current_section = paragraph.section
        if current_words >= max_words:
            flush()
    flush()
    return chunks


def build_chunk(
    source: dict,
    paragraphs: list[Paragraph],
    ordinal: int,
    page_reference_type: str,
) -> dict:
    text = "\n\n".join(p.text for p in paragraphs).strip()
    page_start = min(p.page_start for p in paragraphs)
    page_end = max(p.page_end for p in paragraphs)
    section = paragraphs[0].section
    digest = hashlib.sha1(
        f"{source['id']}|{section}|{page_start}|{page_end}|{text}".encode("utf-8")
    ).hexdigest()[:12]
    return {
        "chunkId": f"{source['id']}:{ordinal:05d}:{digest}",
        "sourceId": source["id"],
        "title": source["title"],
        "authorOrPublisher": source["authorOrPublisher"],
        "publicationYear": source.get("year"),
        "sourceType": source["sourceType"],
        "historicalOrCurrent": source["historicalOrCurrent"],
        "rightsClass": source["rightsClass"],
        "chapterOrSection": section,
        "pageStart": page_start,
        "pageEnd": page_end,
        "pageReferenceType": page_reference_type,
        "districts": source.get("districts", []),
        "entities": source.get("entities", []),
        "topics": source.get("topics", []),
        "text": text,
        "sourceUrl": source["sourceUrl"],
    }


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--djvu", required=True, type=Path)
    parser.add_argument("--scandata", required=True, type=Path)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--out-dir", required=True, type=Path)
    parser.add_argument("--min-words", type=int, default=250)
    parser.add_argument("--max-words", type=int, default=700)
    args = parser.parse_args()

    if args.min_words <= 0 or args.max_words < args.min_words:
        raise SystemExit("Invalid chunk word limits")

    source = json.loads(args.source.read_text(encoding="utf-8"))
    printed_page_map = load_printed_page_map(args.scandata)
    use_scan_pages = len(printed_page_map) < MIN_PRINTED_PAGE_LABELS
    start_patterns = compile_start_patterns(source)

    paragraphs, diagnostics = read_ocr_paragraphs(
        args.djvu,
        printed_page_map,
        use_scan_pages,
        start_patterns,
    )
    if not paragraphs:
        raise SystemExit("No usable OCR paragraphs found")

    chunks = chunk_paragraphs(paragraphs, args.min_words, args.max_words)
    page_reference_type = diagnostics["pageReferenceType"]
    records = [
        build_chunk(source, chunk, i + 1, page_reference_type)
        for i, chunk in enumerate(chunks)
    ]

    args.out_dir.mkdir(parents=True, exist_ok=True)
    chunk_path = args.out_dir / "rag_chunks.jsonl"
    with chunk_path.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")

    pages = {p.page_start for p in paragraphs} | {p.page_end for p in paragraphs}
    stats = {
        "sourceId": source["id"],
        "pagesSeen": len(pages),
        "pageMin": min(pages) if pages else None,
        "pageMax": max(pages) if pages else None,
        "paragraphsRetained": len(paragraphs),
        "chunksCreated": len(records),
        "chunkWordMin": min((word_count(r["text"]) for r in records), default=0),
        "chunkWordMax": max((word_count(r["text"]) for r in records), default=0),
        "chunkWordAverage": round(
            sum(word_count(r["text"]) for r in records) / len(records), 1
        ) if records else 0,
        "ragChunkBytes": chunk_path.stat().st_size,
        "completeSource": True,
        **diagnostics,
    }
    write_json(args.out_dir / "stats.json", stats)
    write_json(args.out_dir / "source.json", source)
    print(json.dumps(stats, indent=2))


if __name__ == "__main__":
    main()
