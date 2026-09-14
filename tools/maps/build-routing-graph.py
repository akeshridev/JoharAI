#!/usr/bin/env python3
"""Compile an OSM XML road extract into Johar's compact Ranchi routing graph.

Output format (gzip TSV):
  # JOHAR_ROUTING_V1
  N <id> <lat> <lon>
  E <from> <to> <distance_m> <duration_s>

Only car-routable highway ways are included. Oneway tags and roundabouts are respected.
Turn restrictions are intentionally not modeled in v1.
"""

from __future__ import annotations

import argparse
import gzip
import math
import re
import xml.etree.ElementTree as ET
from collections import defaultdict

ALLOWED_HIGHWAYS = {
    "motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link",
    "secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified",
    "residential", "living_street", "service", "road",
}

DEFAULT_SPEED_KPH = {
    "motorway": 90,
    "motorway_link": 50,
    "trunk": 70,
    "trunk_link": 45,
    "primary": 55,
    "primary_link": 40,
    "secondary": 45,
    "secondary_link": 35,
    "tertiary": 35,
    "tertiary_link": 30,
    "unclassified": 30,
    "residential": 25,
    "living_street": 15,
    "service": 15,
    "road": 25,
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("input_osm")
    parser.add_argument("output_graph")
    return parser.parse_args()


def haversine_m(a: tuple[float, float], b: tuple[float, float]) -> float:
    lat1, lon1 = map(math.radians, a)
    lat2, lon2 = map(math.radians, b)
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    h = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 6_371_008.8 * 2 * math.atan2(math.sqrt(h), math.sqrt(1 - h))


def speed_kph(tags: dict[str, str], highway: str) -> float:
    raw = tags.get("maxspeed", "").lower().strip()
    match = re.search(r"(\d+(?:\.\d+)?)", raw)
    if match:
        value = float(match.group(1))
        if "mph" in raw:
            value *= 1.609344
        return max(5.0, min(value, 130.0))
    return float(DEFAULT_SPEED_KPH[highway])


def allowed(tags: dict[str, str]) -> bool:
    highway = tags.get("highway")
    if highway not in ALLOWED_HIGHWAYS:
        return False
    if tags.get("access") in {"no", "private"}:
        return False
    if tags.get("motor_vehicle") in {"no", "private"}:
        return False
    if tags.get("motorcar") in {"no", "private"}:
        return False
    return True


def direction(tags: dict[str, str]) -> int:
    value = tags.get("oneway", "").lower()
    if value == "-1":
        return -1
    if value in {"yes", "true", "1"} or tags.get("junction") == "roundabout":
        return 1
    return 0


def main() -> None:
    args = parse_args()
    coordinates: dict[int, tuple[float, float]] = {}
    ways: list[tuple[list[int], dict[str, str]]] = []

    for event, elem in ET.iterparse(args.input_osm, events=("end",)):
        if elem.tag == "node":
            coordinates[int(elem.attrib["id"])] = (float(elem.attrib["lat"]), float(elem.attrib["lon"]))
            elem.clear()
        elif elem.tag == "way":
            refs = [int(nd.attrib["ref"]) for nd in elem.findall("nd")]
            tags = {tag.attrib["k"]: tag.attrib["v"] for tag in elem.findall("tag")}
            if len(refs) >= 2 and allowed(tags):
                ways.append((refs, tags))
            elem.clear()

    used_nodes: set[int] = set()
    edge_rows: list[tuple[int, int, float, float]] = []

    for refs, tags in ways:
        highway = tags["highway"]
        meters_per_second = speed_kph(tags, highway) / 3.6
        way_direction = direction(tags)
        for left, right in zip(refs, refs[1:]):
            a = coordinates.get(left)
            b = coordinates.get(right)
            if a is None or b is None:
                continue
            distance = haversine_m(a, b)
            if distance <= 0.0:
                continue
            duration = distance / meters_per_second
            used_nodes.add(left)
            used_nodes.add(right)
            if way_direction >= 0:
                edge_rows.append((left, right, distance, duration))
            if way_direction <= 0:
                edge_rows.append((right, left, distance, duration))

    node_ids = {osm_id: index for index, osm_id in enumerate(sorted(used_nodes))}
    adjacency_count: dict[int, int] = defaultdict(int)

    with gzip.open(args.output_graph, "wt", encoding="utf-8", newline="\n") as out:
        out.write("# JOHAR_ROUTING_V1\n")
        out.write("# scope=ranchi-district mode=car source=openstreetmap\n")
        for osm_id, graph_id in node_ids.items():
            lat, lon = coordinates[osm_id]
            out.write(f"N\t{graph_id}\t{lat:.7f}\t{lon:.7f}\n")
        for from_osm, to_osm, distance, duration in edge_rows:
            from_id = node_ids[from_osm]
            to_id = node_ids[to_osm]
            adjacency_count[from_id] += 1
            out.write(f"E\t{from_id}\t{to_id}\t{distance:.2f}\t{duration:.2f}\n")

    print(f"nodes={len(node_ids)} edges={len(edge_rows)}")
    print(f"output={args.output_graph}")


if __name__ == "__main__":
    main()
