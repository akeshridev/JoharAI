# AGENTS.md — retrieval

Read root `AGENTS.md` and `johar-data/AGENTS.md` first.

## Common-query prototype contract
Golden 100 hardening must improve generic retrieval/answer behavior, not add phrase-specific UI hacks.

- Complete stored aliases are valid entity identity for admission and ranking; partial aliases or arbitrary description/fact overlap are not.
- Broad category queries such as `school`, `hospital`, `Ranchi waterfalls` may return a short source-backed list.
- Named-entity questions must remain entity-specific. Do not answer `Pahari Mandir mein stairs hain?` by listing unrelated temples.
- Attribute/detail questions should select a matching returned fact. If the entity is known but the requested attribute is not sourced, return a contextual `NO_ANSWER`; never substitute another fact or attraction.
- Source-backed entities without coordinates are still useful knowledge. Spatial/card eligibility and factual-answer eligibility are separate concerns.
- Live/current wording remains guarded upstream; packaged `LIVE`/contact metadata is evidence captured at retrieval time, not proof it is current at query time.
- Preserve evidence/source URLs and freshness through `OfflineKnowledgeHit`/`OfflineKnowledgeFact`.

## Evaluation
Use focused unit tests first, then the Golden 100 pipeline. Do not modify frozen 1000 IDs to improve scores. A passing automated contract is triage, not proof of human usefulness.
