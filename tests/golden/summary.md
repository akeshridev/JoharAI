# Golden 100 common-user set — diagnostic summary

Curated Ranchi V1 product set, not statistically the 100 most searched questions.
Target: 95+/100 usefully handled. PASS below is automated contract triage, pending human review.

Evaluated: 100/100
PASS: 28/100
Substantive answer passes: 20; safe guardrail/clarification passes: 8.

| Verdict | Count |
|---|---:|
| PASS | 28 |
| DATA_GAP | 53 |
| PARSER/INTENT_GAP | 17 |
| SAFETY_FAIL | 0 |
| ROUTING_FAIL | 1 |
| UX_FALLBACK | 1 |

## Coverage by category

| Category | PASS | Total |
|---|---:|---:|
| food | 6 | 12 |
| places | 1 | 10 |
| temples | 5 | 7 |
| nature | 1 | 8 |
| transport | 0 | 7 |
| education | 0 | 8 |
| health | 0 | 9 |
| daily_life | 0 | 12 |
| nearby | 3 | 6 |
| routes | 4 | 5 |
| culture | 4 | 7 |
| visitor | 0 | 4 |
| live | 2 | 3 |
| unsupported | 2 | 2 |

## Cases needing review

| ID | Query | Verdict | Reason |
|---|---|---|---|
| G001 | pani puri | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G002 | puchka | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G003 | golgappa | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G010 | pittha kya hota hai? | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G011 | Jharkhand ka local khana | PARSER/INTENT_GAP | Relevant candidate not selected |
| G012 | Ranchi mein famous mithai | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G014 | Rock Garden | DATA_GAP | Selected spatial records have no source facts in this snapshot |
| G015 | Kanke Dam | UX_FALLBACK | Relevant candidates existed but no useful answer was selected |
| G016 | Ranchi Lake | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G017 | Bada Talab | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G018 | Birsa Zoological Park | PARSER/INTENT_GAP | Relevant candidate not selected |
| G019 | Nakshatra Van | DATA_GAP | Grounded answer has no returned fact/source evidence |
| G020 | Patratu Valley | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G021 | tagor hill | PARSER/INTENT_GAP | Relevant candidate not selected |
| G022 | Ranchi Science Centre | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G026 | Rajrappa mandir | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G027 | Surya Mandir Ranchi | PARSER/INTENT_GAP | Relevant candidate not selected |
| G030 | Hundru Falls | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G031 | Dassam Falls | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G032 | Jonha Falls | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G034 | Sita Falls | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G035 | Hirni Falls | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G036 | hundru waterfall | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G037 | Ranchi waterfalls | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G038 | Ranchi station | DATA_GAP | Selected spatial records have no source facts in this snapshot |
| G039 | Ranchi railway junction | DATA_GAP | Selected spatial records have no source facts in this snapshot |
| G040 | Hatia station | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G041 | Birsa Munda Airport | DATA_GAP | Selected spatial records have no source facts in this snapshot |
| G042 | Ranchi airport kahan hai? | DATA_GAP | Grounded answer has no returned fact/source evidence |
| G043 | Khadgarha bus stand | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G044 | Kantatoli bus stand | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G045 | school | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G046 | schools | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G047 | list schools in Ranchi | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G048 | college | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G049 | library | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G050 | Ranchi University | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G051 | BIT Mesra | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G052 | IIM Ranchi | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G053 | hospital | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G054 | hospitals in Ranchi | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G055 | pharmacy | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G056 | medical store | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G057 | police station | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G058 | fire station | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G059 | RIMS Ranchi | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G060 | Sadar Hospital | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G061 | ambulance number Ranchi | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G062 | market | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G063 | sabzi mandi | DATA_GAP | Grounded answer has no returned fact/source evidence |
| G064 | restaurant | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G065 | resturant | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G066 | cafe | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G067 | ATM | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G068 | petrol pump | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G069 | public toilet | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G070 | parking | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G071 | Upper Bazar | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G072 | Daily Market Ranchi | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G073 | Nucleus Mall | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G074 | Lalpur ke paas hospital | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G075 | Kanke ke paas mandir | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G076 | Ranchi station ke paas hotel | DATA_GAP | No supported answer; inspect corpus before changing parser |
| G083 | Hatia station to RIMS Ranchi | ROUTING_FAIL | Requested route not produced; inspect endpoint resolution and route pack |
| G086 | Karma festival | PARSER/INTENT_GAP | Relevant candidate not selected |
| G090 | Nagpuri language | PARSER/INTENT_GAP | Relevant candidate not selected |
| G091 | Ranchi naam ka history | DATA_GAP | No matching expected entity/category observed; may need alias/discovery investigation |
| G092 | Ranchi ghumne ki jagah | PARSER/INTENT_GAP | Relevant candidate not selected |
| G093 | bachchon ke liye park Ranchi | DATA_GAP | Grounded answer has no returned fact/source evidence |
| G094 | Pahari Mandir mein stairs hain? | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G095 | Hundru Falls wheelchair accessible hai? | PARSER/INTENT_GAP | Answer mixes requested entities/category with unrelated results |
| G098 | Ranchi station se airport ka current taxi fare? | PARSER/INTENT_GAP | Live/current question routed to clarification/fallback without current-status context |

## Run metadata

```json
{
  "evaluatedAtUtc": "2026-09-15T02:14:06.817629+00:00",
  "assetSha256": "7337394dfd093399c1dcfa4dfb43174962742e9fea6437021ac7381098c73674",
  "start": 1,
  "end": 100,
  "mode": "actual_android_pipeline",
  "database": "existing working Room DB; not cleared or replaced",
  "gitHead": "4e7de5197c258264c7c246f49db43cbfeadb6547",
  "workingTreeDirty": true,
  "device": "sdk_gphone16k_arm64",
  "packagePresentBeforeRun": true,
  "runtimeSnapshot": {
    "startedAtEpochMillis": 1789438434360,
    "entityCount": 395,
    "factCount": 209,
    "database": "working johar.db; normal seed/booster initialization"
  }
}
```

Pipeline evaluation uses mapped presentation text, not a screenshot or rendered Compose assertion.
DATA_GAP means insufficient observed evidence, not proof the information cannot exist in the corpus.
Practical/contact questions require manual fact-level review; no automatic useful pass from identity alone.
