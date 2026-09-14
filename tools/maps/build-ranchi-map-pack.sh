#!/usr/bin/env bash
set -euo pipefail

# Builds the Johar Ranchi V1 offline basemap from a Protomaps/OSM PMTiles archive.
# Requirement: `pmtiles` CLI available on PATH.
# Output is intentionally Ranchi-only and copied into app assets for packaging.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REGION="$ROOT_DIR/tools/maps/ranchi-district.geojson"
OUT_DIR="$ROOT_DIR/app/src/main/assets/maps"
OUT="$OUT_DIR/ranchi.pmtiles"

# Pin/override this URL when producing a release so map data is reproducible.
SOURCE="${JOHAR_PM_SOURCE:-https://build.protomaps.com/20260914.pmtiles}"
MAX_ZOOM="${JOHAR_MAP_MAX_ZOOM:-15}"

command -v pmtiles >/dev/null 2>&1 || {
  echo "pmtiles CLI is required: https://docs.protomaps.com/pmtiles/cli" >&2
  exit 1
}

mkdir -p "$OUT_DIR"

echo "Building Ranchi-only offline map pack"
echo "source=$SOURCE"
echo "region=$REGION"
echo "maxZoom=$MAX_ZOOM"

pmtiles extract "$SOURCE" "$OUT" \
  --region="$REGION" \
  --maxzoom="$MAX_ZOOM"

pmtiles show "$OUT"

echo
printf 'Created %s\n' "$OUT"
printf 'Remember: map UI must display OpenStreetMap attribution.\n'
