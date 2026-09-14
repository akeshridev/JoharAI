#!/usr/bin/env python3
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

EXPECTED_TOTAL = 1000
STATUS_ORDER = [
    "PASS",
    "DATA_GAP",
    "PARSER_GAP",
    "UI_GAP",
    "SAFETY_GROUNDING_FAIL",
    "CRASH_ANR",
    "HARNESS_ERROR",
]

path = Path(sys.argv[1] if len(sys.argv) > 1 else "tests/agent/results.jsonl")
rows = []
if path.exists():
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(line))

counts = Counter(row.get("status", "UNKNOWN") for row in rows)
print(f"Executed: {len(rows)} / {EXPECTED_TOTAL}")
for key in STATUS_ORDER:
    print(f"{key}: {counts.get(key, 0)}")

family_counts: dict[str, Counter] = defaultdict(Counter)
family_totals = Counter()
for row in rows:
    notes = str(row.get("notes", ""))
    match = re.search(r"(?:^|;)\s*expected=([^;]+)", notes)
    family = match.group(1).strip() if match else "UNKNOWN"
    status = str(row.get("status", "UNKNOWN"))
    family_counts[family][status] += 1
    family_totals[family] += 1

if rows:
    print("\nBy family:")
    for family in sorted(family_totals):
        total = family_totals[family]
        passed = family_counts[family].get("PASS", 0)
        pass_rate = (passed / total * 100.0) if total else 0.0
        gaps = ", ".join(
            f"{status}={family_counts[family].get(status, 0)}"
            for status in STATUS_ORDER
            if family_counts[family].get(status, 0)
        )
        print(f"{family}: {passed}/{total} PASS ({pass_rate:.1f}%) | {gaps}")

failures = [row for row in rows if row.get("status") != "PASS"]
if failures:
    print("\nNon-pass cases:")
    for row in failures:
        print(
            f"{int(row.get('id', 0)):04d} | {row.get('status')} | "
            f"{row.get('resultType')} | {row.get('query')}"
        )
