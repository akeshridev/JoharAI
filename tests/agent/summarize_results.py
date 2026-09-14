#!/usr/bin/env python3
import json
import sys
from collections import Counter
from pathlib import Path

path = Path(sys.argv[1] if len(sys.argv) > 1 else "tests/agent/results.jsonl")
rows = []
if path.exists():
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(line))

counts = Counter(row.get("status", "UNKNOWN") for row in rows)
print(f"Executed: {len(rows)} / 200")
for key in [
    "PASS",
    "DATA_GAP",
    "PARSER_GAP",
    "UI_GAP",
    "SAFETY_GROUNDING_FAIL",
    "CRASH_ANR",
    "HARNESS_ERROR",
]:
    print(f"{key}: {counts.get(key, 0)}")

failures = [row for row in rows if row.get("status") != "PASS"]
if failures:
    print("\nNon-pass cases:")
    for row in failures:
        print(
            f"{int(row.get('id', 0)):03d} | {row.get('status')} | "
            f"{row.get('resultType')} | {row.get('query')}"
        )
