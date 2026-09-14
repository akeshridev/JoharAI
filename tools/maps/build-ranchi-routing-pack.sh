#!/usr/bin/env bash
set -euo pipefail

# Builds Johar Ranchi V1 offline car-routing graph.
# Build-time network is allowed; app runtime remains fully offline.
# Requirements: curl, python3, osmium.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK_DIR="$ROOT_DIR/build/johar-routing"
OUT_DIR="$ROOT_DIR/app/src/main/assets/routing"
STATE_BOUNDARIES="$WORK_DIR/jharkhand-districts.geojson"
RANCHI_BOUNDARY="$WORK_DIR/ranchi-district.geojson"
ZONE_PBF="$WORK_DIR/eastern-zone-latest.osm.pbf"
RANCHI_PBF="$WORK_DIR/ranchi.osm.pbf"
ROADS_PBF="$WORK_DIR/ranchi-roads.osm.pbf"
ROADS_OSM="$WORK_DIR/ranchi-roads.osm"
OUT="$OUT_DIR/ranchi-routing-v1.tsv.gz"

DISTRICT_SOURCE="${JOHAR_DISTRICT_SOURCE:-https://raw.githubusercontent.com/udit-001/india-maps-data/main/geojson/states/jharkhand.geojson}"
OSM_SOURCE="${JOHAR_OSM_SOURCE:-https://download.geofabrik.de/asia/india/eastern-zone-latest.osm.pbf}"

for tool in curl python3 osmium; do
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

if [[ ! -s "$ZONE_PBF" ]]; then
  echo "Fetching eastern India OpenStreetMap extract"
  curl --fail --location --show-error "$OSM_SOURCE" -o "$ZONE_PBF"
else
  echo "Reusing $ZONE_PBF"
fi

echo "Clipping OSM to Ranchi district"
osmium extract \
  --polygon "$RANCHI_BOUNDARY" \
  --strategy=complete_ways \
  --overwrite \
  "$ZONE_PBF" \
  -o "$RANCHI_PBF"

echo "Keeping road ways"
osmium tags-filter \
  --overwrite \
  "$RANCHI_PBF" \
  w/highway \
  -o "$ROADS_PBF"

# XML is used only as a simple, dependency-free interchange format for the Python compiler.
osmium cat --overwrite -f osm "$ROADS_PBF" -o "$ROADS_OSM"

echo "Compiling Ranchi routing graph"
rm -f "$OUT"
python3 "$ROOT_DIR/tools/maps/build-routing-graph.py" "$ROADS_OSM" "$OUT"

echo
printf 'Created %s\n' "$OUT"
printf 'Scope: Ranchi district only. Runtime routing is fully offline.\n'
printf 'Data: OpenStreetMap contributors; retain attribution in map UI.\n'
