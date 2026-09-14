#!/usr/bin/env bash
set -euo pipefail

# Builds Johar Ranchi V1 offline basemap.
# Build-time network is allowed; app runtime remains fully offline.
# Requirements: curl, python3, pmtiles CLI.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK_DIR="$ROOT_DIR/build/johar-maps"
OUT_DIR="$ROOT_DIR/app/src/main/assets/maps"
STATE_BOUNDARIES="$WORK_DIR/jharkhand-districts.geojson"
RANCHI_BOUNDARY="$WORK_DIR/ranchi-district.geojson"
OUT="$OUT_DIR/ranchi.pmtiles"

DISTRICT_SOURCE="${JOHAR_DISTRICT_SOURCE:-https://raw.githubusercontent.com/udit-001/india-maps-data/main/geojson/states/jharkhand.geojson}"
PM_SOURCE="${JOHAR_PM_SOURCE:-https://build.protomaps.com/20260914.pmtiles}"
MAX_ZOOM="${JOHAR_MAP_MAX_ZOOM:-15}"

for tool in curl python3 pmtiles; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "$tool is required" >&2
    exit 1
  }
done

mkdir -p "$WORK_DIR" "$OUT_DIR"

echo "Fetching Jharkhand district boundaries"
curl --fail --location --silent --show-error "$DISTRICT_SOURCE" -o "$STATE_BOUNDARIES"

python3 "$ROOT_DIR/tools/maps/extract-ranchi-boundary.py" \
  "$STATE_BOUNDARIES" \
  "$RANCHI_BOUNDARY"

echo "Building Ranchi-only offline map pack"
echo "pmtilesSource=$PM_SOURCE"
echo "boundary=$RANCHI_BOUNDARY"
echo "maxZoom=$MAX_ZOOM"

rm -f "$OUT"
pmtiles extract "$PM_SOURCE" "$OUT" \
  --region="$RANCHI_BOUNDARY" \
  --maxzoom="$MAX_ZOOM"

pmtiles show "$OUT"

echo
printf 'Created %s\n' "$OUT"
printf 'Scope: Ranchi district only (Census district code 364).\n'
printf 'Map UI must retain OpenStreetMap attribution.\n'
