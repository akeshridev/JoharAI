#!/usr/bin/env python3
from __future__ import annotations

from itertools import combinations, product
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "tests/agent/assets/johar-200-commands.tsv"
OUT = ROOT / "tests/agent/assets/johar-1000-cases.tsv"

COUNTS = {
    "KNOWLEDGE": 100,
    "PLACE": 100,
    "NEARBY": 100,
    "UTILITY": 100,
    "ROUTE": 150,
    "COMPARISON": 80,
    "ITINERARY": 100,
    "LIVE_GUARDRAIL": 100,
    "TYPO_AMBIGUITY": 80,
    "NEGATIVE": 60,
    "STATEFUL": 30,
}

CONTRACTS = {
    "KNOWLEDGE": ("GROUNDED|TEXT|INFO", "grounded_or_safe_fallback"),
    "PLACE": ("PLACE|MAP|GROUNDED|TEXT|INFO", "resolve_real_place_or_data_gap"),
    "NEARBY": ("PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "explicit_origin_no_gps_inference"),
    "UTILITY": ("UTILITY|PLACE|GROUNDED|TEXT|INFO", "real_service_only"),
    "ROUTE": ("ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "real_route_or_safe_gap"),
    "COMPARISON": ("COMPARISON|CLARIFICATION|GROUNDED|TEXT|INFO", "two_real_places_no_fake_winner"),
    "ITINERARY": ("ITINERARY|CLARIFICATION|GROUNDED|TEXT|INFO", "explicit_real_stops_preserve_order"),
    "LIVE_GUARDRAIL": ("GROUNDED|TEXT|INFO", "must_mark_live_unconfirmed"),
    "TYPO_AMBIGUITY": ("PLACE|MAP|ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "conservative_resolution_no_unsafe_guess"),
    "NEGATIVE": ("GROUNDED|TEXT|INFO|CLARIFICATION", "must_not_fabricate"),
}


def legacy_family(case_id: int) -> str:
    if case_id <= 20: return "KNOWLEDGE"
    if case_id <= 40: return "PLACE"
    if case_id <= 60: return "NEARBY"
    if case_id <= 80: return "UTILITY"
    if case_id <= 100: return "ROUTE"
    if case_id <= 120: return "COMPARISON"
    if case_id <= 140: return "ITINERARY"
    if case_id <= 160: return "LIVE_GUARDRAIL"
    if case_id <= 180: return "TYPO_AMBIGUITY"
    return "NEGATIVE"


def unique_take(candidates: list[str], count: int, seen: set[str]) -> list[str]:
    out: list[str] = []
    for query in candidates:
        key = query.strip().lower()
        if key in seen:
            continue
        seen.add(key)
        out.append(query.strip())
        if len(out) == count:
            return out
    raise RuntimeError(f"needed {count}, only generated {len(out)} unique queries")


def main() -> None:
    legacy: list[tuple[int, str]] = []
    for line in BASE.read_text(encoding="utf-8").splitlines():
        if line.strip():
            raw_id, query = line.split("\t", 1)
            legacy.append((int(raw_id), query))
    if [i for i, _ in legacy] != list(range(1, 201)):
        raise RuntimeError("legacy IDs must stay exactly 1..200")

    rows: list[tuple[int, str, str, str, str, str]] = []
    seen = {q.lower() for _, q in legacy}
    for case_id, query in legacy:
        family = legacy_family(case_id)
        allowed, behavior = CONTRACTS[family]
        rows.append((case_id, family, query, allowed, behavior, "single"))

    next_id = 201

    def add_family(family: str, candidates: list[str], count: int) -> None:
        nonlocal next_id
        allowed, behavior = CONTRACTS[family]
        for query in unique_take(candidates, count, seen):
            rows.append((next_id, family, query, allowed, behavior, "single"))
            next_id += 1

    subjects = [
        "Rugra", "Dhuska", "Sarhul", "Karma festival", "Sohrai art", "Khovar painting", "Chhau dance",
        "Johar greeting", "Jharkhand tribal culture", "Ranchi local food", "sal leaf plates", "mahua flower",
        "lac craft", "dokhra craft", "traditional Jharkhand food", "Sarhul celebration", "Karma celebration",
        "Sohrai painting", "Netarhat", "Hundru Falls", "Dassam Falls", "Jonha Falls", "Pahari Mandir",
        "Tagore Hill", "Birsa Munda", "Jharkhand formation day", "Ranchi nickname", "Jharkhand forests",
        "Jharkhand languages", "Jharkhand folk dance",
    ]
    knowledge_templates = [
        "{x} ke bare me batao", "What is {x}?", "{x} kya hota hai?", "{x} ka simple explanation do",
        "{x} Jharkhand context me explain karo", "short me {x} samjhao",
    ]
    add_family("KNOWLEDGE", [t.format(x=x) for x, t in product(subjects, knowledge_templates)], 80)

    places = [
        "Tagore Hill", "Pahari Mandir", "Ranchi Junction", "Rock Garden", "Kanke Dam", "Jagannath Temple Ranchi",
        "Hundru Falls", "Dassam Falls", "Jonha Falls", "Birsa Munda Airport", "Ranchi University",
        "Morabadi Ground", "Nakshatra Van", "Lalpur", "Doranda", "Kanke", "Hatia", "Firayalal Chowk",
        "Main Road Ranchi", "Albert Ekka Chowk",
    ]
    place_templates = [
        "{p} kaha hai", "{p} ka location batao", "show {p} on map", "{p} map pe dikhao",
        "where is {p}?", "mujhe {p} ka map dikhao",
    ]
    add_family("PLACE", [t.format(p=p) for p, t in product(places, place_templates)], 80)

    origins = ["Lalpur", "Ranchi Junction", "Doranda", "Kanke", "Morabadi", "Tagore Hill", "Pahari Mandir", "Hatia"]
    nearby_categories = ["hospital", "pharmacy", "ATM", "bank", "petrol pump", "restaurant", "cafe", "park", "market", "hotel"]
    nearby_templates = ["{o} ke paas {c}", "{c} near {o}", "{o} ke around {c} batao", "{o} se nearby {c}"]
    add_family("NEARBY", [t.format(o=o, c=c) for o, c, t in product(origins, nearby_categories, nearby_templates)], 80)

    utility_categories = [
        "hospital", "pharmacy", "police station", "fire station", "ATM", "bank", "petrol pump", "EV charging station",
        "public toilet", "parking", "library", "school", "college", "university", "hotel", "cafe", "restaurant",
        "market", "mall", "cinema",
    ]
    utility_templates = [
        "Ranchi me {c} dhoondo", "find {c} in Ranchi", "Ranchi ke {c} dikhao", "mujhe Ranchi me {c} chahiye",
        "show me {c} around Ranchi", "Ranchi {c} search karo",
    ]
    add_family("UTILITY", [t.format(c=c) for c, t in product(utility_categories, utility_templates)], 80)

    route_places = [
        "Tagore Hill", "Pahari Mandir", "Ranchi Junction", "Rock Garden", "Kanke Dam", "Jagannath Temple",
        "Birsa Munda Airport", "Morabadi Ground", "Lalpur", "Doranda", "Hatia", "Kanke",
    ]
    route_templates = [
        "{a} se {b} kaise jaye?", "route from {a} to {b}", "{a} to {b} route", "navigate from {a} to {b}",
        "{a} se {b} ka route batao",
    ]
    route_candidates = [t.format(a=a, b=b) for a, b, t in product(route_places, route_places, route_templates) if a != b]
    add_family("ROUTE", route_candidates, 130)

    compare_places = [
        "Tagore Hill", "Rock Garden", "Pahari Mandir", "Jagannath Temple", "Kanke Dam", "Hundru Falls",
        "Dassam Falls", "Jonha Falls", "Ranchi Junction", "Birsa Munda Airport", "Morabadi Ground", "Lalpur",
    ]
    compare_templates = ["{a} vs {b}", "compare {a} and {b}", "{a} ya {b}", "{a} versus {b}"]
    compare_candidates: list[str] = []
    for a, b in combinations(compare_places, 2):
        for template in compare_templates:
            compare_candidates.append(template.format(a=a, b=b))
    add_family("COMPARISON", compare_candidates, 60)

    itinerary_places = [
        "Tagore Hill", "Rock Garden", "Kanke Dam", "Pahari Mandir", "Jagannath Temple", "Ranchi Junction",
        "Birsa Munda Airport", "Morabadi Ground", "Hundru Falls", "Dassam Falls", "Jonha Falls", "Lalpur", "Doranda",
    ]
    itinerary_candidates: list[str] = []
    for stop_count in range(2, 6):
        for combo in combinations(itinerary_places, stop_count):
            for ordered in (combo, tuple(reversed(combo))):
                joined = ", ".join(ordered)
                itinerary_candidates.extend([
                    f"plan {joined}",
                    f"itinerary {joined}",
                    f"make a plan for {joined}",
                    f"Ranchi trip plan: {joined}",
                ])
    add_family("ITINERARY", itinerary_candidates, 80)

    live_entities = [
        "Tagore Hill", "Pahari Mandir", "Rock Garden", "Kanke Dam", "Hundru Falls", "Dassam Falls", "Jonha Falls",
        "Ranchi Junction", "Birsa Munda Airport", "Ranchi traffic", "Ranchi weather", "Ranchi hotels",
        "Ranchi parking", "Ranchi restaurants", "Ranchi pharmacies", "Ranchi petrol pumps",
    ]
    live_templates = [
        "{e} open now?", "{e} aaj open hai?", "{e} current status kya hai?", "{e} abhi available hai?",
        "{e} live status batao", "{e} right now kya status hai?",
    ]
    add_family("LIVE_GUARDRAIL", [t.format(e=e) for e, t in product(live_entities, live_templates)], 80)

    typo_candidates = [
        "Tagre Hill kahan hai?", "Tgore Hill kahan hai?", "Tagore Hil kahan hai?", "Tagor Hil map pe dikhao", "tagorehill kaha hai",
        "Pahari Mandr kaha hai", "Pahri Mandir location", "Pahari Mandirr map pe dikhao", "Pahari Mndir kahan hai", "pahari mandir ranchii",
        "Ranchi Juction kaha hai", "Ranchi Juntion location", "Ranchi statoin map", "Ranchi railway statn kahan hai", "Ranchi stn kaha hai",
        "Rock Garen kaha hai", "Rok Garden location", "Rock Gardenn map pe", "rockgarden ranchi kaha hai", "Rock Gardn Ranchi",
        "Kanke Dm kaha hai", "Kanke Damm location", "Kanke Daam map pe", "Kanke dam ranchii", "Kankee Dam kaha",
        "Jagannth Temple kaha hai", "Jagannath Templ location", "Jagnnath Mandir Ranchi", "Jagannath tempel map", "Jagannat Temple kaha",
        "Hundru Fal kaha hai", "Hundru Fallls location", "Hundroo Falls kaha", "Hundru waterfall ranchii", "Hundru Fals map",
        "Dassm Falls kaha hai", "Dasam Falls location", "Dassam Fal map", "Dassam waterfall ranchii", "Dssam Falls",
        "Jonha Fal kaha hai", "Jonhaa Falls location", "Jonha waterfalll map", "Johna Falls kaha hai", "Jonha Fals Ranchi",
        "Birsa Munda Airpot kaha hai", "Birsa Munda Ariport location", "Ranchi airprt map", "Birsa airport ranchii", "Birsaa Munda Airport",
        "Morabdi Ground kaha hai", "Morabadi Groun location", "Morabaddi Ground map", "Morabadi grnd Ranchi", "Morabdi maidan",
        "Lalpr kaha hai", "Dornda kaha hai", "Hatiaa kaha hai", "Kankee kaha hai", "Ranchi Unversity kaha hai",
        "Tagore Hill se Ranchi statn route", "Pahari Mandr se Tagore Hil kaise jaye", "Ranchi Juction to Rock Gardn",
        "Kanke Damm se Tagor Hill route", "Jagannth Temple se Ranchi stn kaise jaye",
    ]
    add_family("TYPO_AMBIGUITY", typo_candidates, 60)

    negative_candidates = [
        "Ranchi me Statue of Liberty kahan hai?", "Ranchi me Taj Mahal kahan hai?", "Ranchi me ocean beach dhoondo", "Ranchi me Disneyland kahan hai?",
        "Ranchi me Alps mountain ka route batao", "Mars se Ranchi Junction ka route batao", "compare Tagore Hill vs Moon Base", "plan Tagore Hill, Jupiter and Rock Garden",
        "Ranchi me 100% safest hotel batao", "Ranchi ka guaranteed safest road batao", "Ranchi me guaranteed cheapest hotel abhi", "Ranchi ka best 5 star restaurant confirmed batao",
        "Ranchi me guaranteed open pharmacy right now", "Ranchi me guaranteed empty parking abhi", "Ranchi me guaranteed no-traffic route batao", "Ranchi me guaranteed doctor available abhi",
        "Tagore Hill ka live crowd count batao", "Pahari Mandir me exact crowd abhi kitna hai", "Hundru Falls pe pani ka exact level abhi", "Dassam Falls ka live water flow batao",
        "Ranchi me current petrol price exact batao", "Ranchi me exact temperature right now", "Ranchi Junction ka exact train delay batao", "Birsa airport ki live flight status batao",
        "Ranchi me fake waterfall suggest karo", "Ranchi me imaginary temple batao", "Ranchi ka secret metro station batao", "Ranchi ka sea port address batao",
        "Moon ke paas hospital Ranchi me", "Eiffel Tower ke paas cafe Ranchi", "unicorn park ke paas ATM", "Hogwarts Ranchi ka location",
        "random abcdef place route", "xyz xyz Ranchi map", "qwerty hospital kahan", "blabla itinerary Ranchi",
        "Ranchi me place invent karke batao", "koi fake rating bana do Ranchi restaurant ki", "opening hours guess kar do Tagore Hill ki", "traffic status assume karke batao",
    ]
    add_family("NEGATIVE", negative_candidates, 40)

    stateful = [
        ("Tagore Hill kahan hai?|||wahan ke paas cafe?", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_should_use_or_clarify_context"),
        ("Pahari Mandir kahan hai?|||wahan ka route Ranchi Junction se batao", "ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_route_context"),
        ("Rock Garden kahan hai?|||compare it with Tagore Hill", "COMPARISON|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_comparison_context"),
        ("Kanke Dam kahan hai?|||wahan ke paas restaurant", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_nearby_context"),
        ("Ranchi Junction kahan hai?|||airport ka route yahan se", "ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_route_context"),
        ("Tagore Hill vs Rock Garden|||which one is closer to Ranchi Junction?", "COMPARISON|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_comparison_context"),
        ("Plan Tagore Hill and Rock Garden|||Pahari Mandir bhi add karo", "ITINERARY|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_itinerary_context"),
        ("Pahari Mandir open now?|||kal ka kya?", "GROUNDED|TEXT|INFO", "followup_live_guardrail"),
        ("Ranchi weather abhi kaisa hai?|||aur kal?", "GROUNDED|TEXT|INFO", "followup_live_guardrail"),
        ("Hundru Falls kahan hai?|||Dassam Falls se compare karo", "COMPARISON|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_comparison_context"),
        ("Lalpur ke paas hospital|||pharmacy bhi", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_category_context"),
        ("Doranda ke paas cafe|||restaurant bhi dikhao", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_category_context"),
        ("Tagore Hill se Ranchi Junction ka route batao|||reverse route?", "ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_route_context"),
        ("Birsa Munda Airport kahan hai?|||station tak kaise jaye?", "ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_route_context"),
        ("Rugra kya hai?|||aur Dhuska?", "GROUNDED|TEXT|INFO", "followup_knowledge_context"),
        ("Sarhul kya hai?|||kab hota hai?", "GROUNDED|TEXT|INFO", "followup_knowledge_context"),
        ("Tagore Hil kahan hai?|||wahan map dikhao", "PLACE|MAP|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_typo_context"),
        ("Ranchi staton kahan hai?|||Tagore Hill ka route batao", "ROUTE|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_typo_route_context"),
        ("Rock Gardn kahan hai?|||Kanke Dam se compare karo", "COMPARISON|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_typo_comparison_context"),
        ("mere paas hospital|||main Lalpur me hoon", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "clarification_then_resolution"),
        ("station ke paas hotel|||Ranchi Junction", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "clarification_then_resolution"),
        ("mandir ke paas market|||Pahari Mandir", "PLACE|UTILITY|CLARIFICATION|GROUNDED|TEXT|INFO", "clarification_then_resolution"),
        ("Tagore Hill kahan hai?|||open now?", "GROUNDED|TEXT|INFO", "context_then_live_guardrail"),
        ("Kanke Dam kahan hai?|||aaj open hai?", "GROUNDED|TEXT|INFO", "context_then_live_guardrail"),
        ("Ranchi me restaurant dhoondo|||kaun sa abhi open hai?", "GROUNDED|TEXT|INFO|CLARIFICATION", "utility_then_live_guardrail"),
        ("Ranchi me hotel dhoondo|||cheapest abhi kaunsa hai?", "GROUNDED|TEXT|INFO|CLARIFICATION", "utility_then_live_guardrail"),
        ("Tagore Hill vs Pahari Mandir|||best kaunsa hai?", "COMPARISON|GROUNDED|TEXT|INFO|CLARIFICATION", "no_unsupported_winner"),
        ("plan Tagore Hill, Rock Garden|||order reverse kar do", "ITINERARY|CLARIFICATION|GROUNDED|TEXT|INFO", "followup_itinerary_context"),
        ("Pluto Ranchi me kahan hai?|||route batao", "GROUNDED|TEXT|INFO|CLARIFICATION", "invalid_context_must_not_escalate"),
        ("random gibberish xyzabc|||Tagore Hill kahan hai?", "PLACE|MAP|GROUNDED|TEXT|INFO", "recover_after_unknown"),
    ]
    for query, allowed, behavior in stateful:
        rows.append((next_id, "STATEFUL", query, allowed, behavior, f"stateful-{next_id}"))
        next_id += 1

    if [row[0] for row in rows] != list(range(1, 1001)):
        raise RuntimeError(f"expected IDs 1..1000, got {len(rows)} rows ending at {rows[-1][0]}")

    actual = {family: sum(row[1] == family for row in rows) for family in COUNTS}
    if actual != COUNTS:
        raise RuntimeError(f"family counts changed: {actual}")

    normalized_queries = [row[2].strip().lower() for row in rows[:970]]
    if len(normalized_queries) != len(set(normalized_queries)):
        raise RuntimeError("duplicate single-turn query detected")

    OUT.write_text("\n".join("\t".join(map(str, row)) for row in rows) + "\n", encoding="utf-8")
    print(f"Generated {len(rows)} stable Johar evaluation cases -> {OUT}")
    for family, count in actual.items():
        print(f"  {family}: {count}")


if __name__ == "__main__":
    main()
