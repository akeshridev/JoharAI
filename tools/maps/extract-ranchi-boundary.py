#!/usr/bin/env python3
import json
import sys
from pathlib import Path

DISTRICT_CODE = "364"
DISTRICT_NAME = "ranchi"

source = Path(sys.argv[1])
output = Path(sys.argv[2])

data = json.loads(source.read_text(encoding="utf-8"))
matches = []
for feature in data.get("features", []):
    props = feature.get("properties", {})
    code = str(props.get("dt_code", ""))
    name = str(props.get("district", "")).strip().lower()
    if code == DISTRICT_CODE or name == DISTRICT_NAME:
        matches.append(feature)

if len(matches) != 1:
    raise SystemExit(f"Expected exactly one Ranchi district feature; found {len(matches)}")

feature = matches[0]
props = dict(feature.get("properties", {}))
props["joharScope"] = "RANCHI_V1"
result = {
    "type": "Feature",
    "properties": props,
    "geometry": feature["geometry"],
}

output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(result, separators=(",", ":")), encoding="utf-8")
print(f"Wrote Ranchi-only boundary: {output}")
