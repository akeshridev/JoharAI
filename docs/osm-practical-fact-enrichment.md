# OSM practical fact enrichment

Ranchi common-man acquisition already discovered many useful OSM entities, but discovery results previously persisted entity identity only. Practical tags returned in the same Overpass response were discarded, leaving domains such as daily life and food/markets with entities but almost no facts.

## Pipeline

Overpass discovery
→ existing specialized/school adapter
→ `OsmPracticalFactEnrichingDiscoveryAdapter`
→ `OsmPracticalFactExtractor`
→ `DiscoveryResult.facts`
→ canonical entity ID remap in `KnowledgeStore.persistDiscovery`
→ `source_facts`

The extractor is deliberately conservative. It keeps practical metadata such as address fields, phone/contact fields, website, operator, cuisine/diet, accessibility/facilities, amenity/shop classification, fuel metadata and published opening hours. No arbitrary OSM tags are bulk-imported.

## Safety

`opening_hours` is stored as published schedule metadata with `Freshness.SEASONAL`. It must never be used by itself to confirm that a place is open now. Current/open-now answers still require a live source or must remain unconfirmed.

## Identity

Facts are initially keyed by the source OSM entity ID. During discovery persistence, `KnowledgeStore` remaps each fact to the canonical entity selected by the identity resolver before writing Room. This prevents a second OSM-specific knowledge store from emerging.

## Expected measurement impact

After a fresh Ranchi crawl, compare the `RANCHI FOUNDATION COVERAGE` lines against the previous baseline. The main expected improvement is `withFacts` and `facts` for daily life, food/markets, health, transport and education. Entity count itself is not the objective of this change.
