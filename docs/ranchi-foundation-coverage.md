# Ranchi Foundation Coverage

Johar measures Ranchi knowledge quality by common-man domain, not by raw entity count alone.

After a Ranchi crawl, `CrawlSummaryLogger` builds a `FoundationCoverageReport` from canonical Room entities and facts and logs one line per domain.

Domains:
- health and emergency
- transport and mobility
- daily life
- education
- food and markets
- places and recreation
- culture and local knowledge
- unclassified

Each domain reports:
- `entities`: enabled canonical entities assigned to the domain
- `coords`: entities with both latitude and longitude
- `aliases`: entities with at least one persisted alias
- `withFacts`: entities with at least one source-backed fact row
- `facts`: total fact rows for entities in the domain
- `evidenceCompleteFacts`: facts with non-empty source URL, publisher and evidence text plus a retrieval timestamp

The report is diagnostic. It does not claim that a category is complete merely because entity count is high. A healthy foundation should improve entity breadth, fact depth, coordinates where spatial use cases require them, alias coverage, and evidence completeness together.

`joharPackType` is used before broad Room entity type where necessary so generic `FACILITY` and `SHOP` records can still be attributed to common-man domains such as SCHOOL, ATM, PHARMACY and FUEL.

Live/current claims remain outside this static coverage metric. A high coverage score must never be interpreted as proof of open-now status, current availability, traffic, fares, stock or live timing.
