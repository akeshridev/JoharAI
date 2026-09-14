#!/usr/bin/env python3
"""Convert TEI/SGML-like book sources into Johar AI RAG chunks.

The ingester intentionally uses only the Python standard library so it can run
in a clean checkout. It is designed for Project Gutenberg / Distributed
Proofreaders TEI where page breaks are represented with <pb n=...> tags and
paragraphs may use SGML-style unclosed <p> tags.

Example:
  python corpus/tools/ingest_tei.py \
    --input /tmp/SantalFolkTales-1.0.tei \
    --source corpus/sources/book_santal_folk_tales_1891.json \
    --out-dir corpus/processed/book_santal_folk_tales_1891
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
from dataclasses import dataclass, field
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable


SPACE_RE = re.compile(r"[ \t\r\f\v]+")
SENTENCE_RE = re.compile(r"(?<=[.!?])\s+")


@dataclass
class Paragraph:
    section: str
    page_start: int | None
    page_end: int | None
    text: str


@dataclass
class ParseState:
    in_text: bool = False
    current_page: int | None = None
    current_section: str = "Untitled section"
    heading_parts: list[str] = field(default_factory=list)
    paragraph_parts: list[str] = field(default_factory=list)
    paragraph_page_start: int | None = None
    paragraph_page_end: int | None = None
    collecting_heading: bool = False
    collecting_paragraph: bool = False
    paragraphs: list[Paragraph] = field(default_factory=list)


def clean_text(value: str) -> str:
    value = html.unescape(value)
    value = value.replace("\u00a0", " ")
    value = SPACE_RE.sub(" ", value)
    value = re.sub(r" *\n *", "\n", value)
    return value.strip()


def parse_page(attrs: list[tuple[str, str | None]]) -> int | None:
    raw = dict(attrs).get("n")
    if raw and raw.isdigit():
        return int(raw)
    return None


class TeiParser(HTMLParser):
    """Small tolerant parser for TEI-Lite / SGML-ish Gutenberg sources."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=False)
        self.state = ParseState()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        if tag == "text":
            self.state.in_text = True
            return
        if not self.state.in_text:
            return
        if tag == "pb":
            page = parse_page(attrs)
            if page is not None:
                self.state.current_page = page
                if self.state.collecting_paragraph:
                    self.state.paragraph_page_end = page
            return
        if tag == "head":
            self._flush_heading()
            self.state.collecting_heading = True
            self.state.heading_parts = []
            return
        if tag == "p":
            self._flush_paragraph()
            self.state.collecting_paragraph = True
            self.state.paragraph_parts = []
            self.state.paragraph_page_start = self.state.current_page
            self.state.paragraph_page_end = self.state.current_page
            return
        if tag == "lb":
            self._append_text("\n")

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag == "text":
            self._flush_heading()
            self._flush_paragraph()
            self.state.in_text = False
        elif tag == "head":
            self._flush_heading()
        elif tag == "p":
            self._flush_paragraph()
        elif tag.startswith("div"):
            self._flush_paragraph()

    def handle_data(self, data: str) -> None:
        if self.state.in_text:
            self._append_text(data)

    def handle_entityref(self, name: str) -> None:
        if self.state.in_text:
            self._append_text(html.unescape(f"&{name};"))

    def handle_charref(self, name: str) -> None:
        if self.state.in_text:
            self._append_text(html.unescape(f"&#{name};"))

    def _append_text(self, data: str) -> None:
        if self.state.collecting_heading:
            self.state.heading_parts.append(data)
        if self.state.collecting_paragraph:
            self.state.paragraph_parts.append(data)

    def _flush_heading(self) -> None:
        if not self.state.collecting_heading:
            return
        text = clean_text("".join(self.state.heading_parts))
        if text:
            self.state.current_section = text
        self.state.collecting_heading = False
        self.state.heading_parts = []

    def _flush_paragraph(self) -> None:
        if not self.state.collecting_paragraph:
            return
        text = clean_text("".join(self.state.paragraph_parts))
        if text:
            self.state.paragraphs.append(
                Paragraph(
                    section=self.state.current_section,
                    page_start=self.state.paragraph_page_start,
                    page_end=self.state.paragraph_page_end,
                    text=text,
                )
            )
        self.state.collecting_paragraph = False
        self.state.paragraph_parts = []
        self.state.paragraph_page_start = None
        self.state.paragraph_page_end = None


def word_count(text: str) -> int:
    return len(text.split())


def split_long_paragraph(paragraph: Paragraph, max_words: int) -> list[Paragraph]:
    if word_count(paragraph.text) <= max_words:
        return [paragraph]

    sentences = SENTENCE_RE.split(paragraph.text)
    pieces: list[Paragraph] = []
    current: list[str] = []
    current_words = 0
    for sentence in sentences:
        count = word_count(sentence)
        if current and current_words + count > max_words:
            pieces.append(
                Paragraph(
                    section=paragraph.section,
                    page_start=paragraph.page_start,
                    page_end=paragraph.page_end,
                    text=" ".join(current).strip(),
                )
            )
            current = []
            current_words = 0
        current.append(sentence)
        current_words += count
    if current:
        pieces.append(
            Paragraph(
                section=paragraph.section,
                page_start=paragraph.page_start,
                page_end=paragraph.page_end,
                text=" ".join(current).strip(),
            )
        )
    return pieces


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
        p_words = word_count(paragraph.text)
        if current and paragraph.section != current_section:
            flush()
        if current and current_words >= min_words and current_words + p_words > max_words:
            flush()
        current.append(paragraph)
        current_words += p_words
        current_section = paragraph.section
        if current_words >= max_words:
            flush()
    flush()
    return chunks


def build_chunk(
    source: dict,
    paragraphs: list[Paragraph],
    ordinal: int,
) -> dict:
    text = "\n\n".join(p.text for p in paragraphs).strip()
    pages = [p.page_start for p in paragraphs if p.page_start is not None]
    pages += [p.page_end for p in paragraphs if p.page_end is not None]
    page_start = min(pages) if pages else None
    page_end = max(pages) if pages else None
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
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--out-dir", required=True, type=Path)
    parser.add_argument("--min-words", type=int, default=250)
    parser.add_argument("--max-words", type=int, default=700)
    args = parser.parse_args()

    if args.min_words <= 0 or args.max_words < args.min_words:
        raise SystemExit("Invalid chunk word limits")

    source = json.loads(args.source.read_text(encoding="utf-8"))
    raw = args.input.read_text(encoding="utf-8", errors="replace")

    tei = TeiParser()
    tei.feed(raw)
    tei.close()
    tei._flush_heading()
    tei._flush_paragraph()

    paragraphs = [p for p in tei.state.paragraphs if p.page_start is not None]
    chunks = chunk_paragraphs(paragraphs, args.min_words, args.max_words)
    records = [build_chunk(source, chunk, i + 1) for i, chunk in enumerate(chunks)]

    args.out_dir.mkdir(parents=True, exist_ok=True)
    chunk_path = args.out_dir / "rag_chunks.jsonl"
    with chunk_path.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")

    page_values = [
        page
        for p in paragraphs
        for page in (p.page_start, p.page_end)
        if page is not None
    ]
    stats = {
        "sourceId": source["id"],
        "pagesSeen": len(set(page_values)),
        "pageMin": min(page_values) if page_values else None,
        "pageMax": max(page_values) if page_values else None,
        "paragraphsRetained": len(paragraphs),
        "chunksCreated": len(records),
        "chunkWordMin": min((word_count(r["text"]) for r in records), default=0),
        "chunkWordMax": max((word_count(r["text"]) for r in records), default=0),
        "chunkWordAverage": round(
            sum(word_count(r["text"]) for r in records) / len(records), 1
        ) if records else 0,
        "ragChunkBytes": chunk_path.stat().st_size,
        "completeSource": True,
    }
    write_json(args.out_dir / "stats.json", stats)
    write_json(args.out_dir / "source.json", source)

    print(json.dumps(stats, indent=2))


if __name__ == "__main__":
    main()
