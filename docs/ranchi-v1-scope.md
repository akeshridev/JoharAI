# Ranchi-first V1 Scope

## Prototype goal
Johar V1 is a Ranchi-first offline/local knowledge assistant. It should be useful to a visitor and to someone who lives, studies, works, shops, travels, raises a family, runs a business, or needs help in Ranchi.

The prototype proves that the same architecture can later scale district-by-district across Jharkhand.

## Geographic scope
Primary scope:
- Ranchi city
- Ranchi district
- city neighborhoods/localities such as Lalpur, Doranda, Harmu, Kanke, HEC, Dhurwa, Morabadi, Bariatu, Kokar, Upper Bazar, Main Road and other discovered localities

Nearby scope:
- practical day-trip destinations commonly reached from Ranchi, roughly within an 80-100 km travel radius when useful
- nearby coverage is driven by real Ranchi-user intent, not administrative boundaries

Distant Jharkhand destinations do not drive V1 coverage metrics.

## Personas
Coverage tests must intentionally represent different people and needs:
- first-time tourist
- returning tourist
- local resident
- child / teenager
- parent with children
- elderly person
- caregiver
- woman travelling alone
- couple
- student
- job seeker
- office worker
- businessman / business visitor
- shopkeeper / small-business owner
- commuter
- driver / biker
- public-transport user
- devotee / pilgrim
- foodie
- vegetarian user
- non-vegetarian user
- patient / caregiver
- fitness / sports user
- photographer / creator
- history / culture learner
- budget traveller
- premium traveller
- person with reduced mobility / wheelchair needs
- user with weak connectivity or low digital literacy

## Knowledge coverage
V1 must not become a tourism-only corpus. The Ranchi knowledge universe includes:

### Places and navigation
Attractions, waterfalls, dams, parks, lakes, viewpoints, picnic spots, museums, monuments, neighborhoods, landmarks, addresses, coordinates, distance, route context, parking, walking, accessibility and day trips.

### Food and eating
Local dishes, seasonal foods, restaurants, cafes, street food, vegetarian/non-vegetarian food, pork/mutton/fish availability, family dining, cuisine, price range, locality, opening information and provenance.

### Markets and shopping
Haat, bazar, mandi, vegetable/fish/meat markets, handicrafts, souvenirs, malls, shopping areas, weekly-market days, typical offerings and locality-level discovery.

### Religion and spirituality
Temples, churches, mosques, gurudwaras and other important places of worship; history, location, accessibility, parking and source-backed practical information. Live ritual/event schedules require fresh evidence.

### Family, children and elderly
Parks, play areas, family-friendly places, low-walking options, stairs, seating, toilets, parking and practical accessibility.

### Health and emergency
Hospitals, clinics, pharmacies when supported, police stations, fire services, emergency services and source-backed contact/location information. High-stakes medical advice is not inferred from local place data.

### Transport and mobility
Railway station, airport, bus terminals, major transit points, auto/taxi context where sourceable, fuel/charging where supported, parking and locality-to-locality travel. Live fares/traffic require fresh evidence.

### Education and student life
Universities, colleges, important educational institutions, libraries, student-oriented areas and source-backed practical discovery.

### Work and business
Business districts/areas, industrial areas, coworking/work-oriented places when sourceable, government/business institutions, convention/event locations, logistics-relevant landmarks and services useful to business visitors.

### Civic and government
Important government offices, public facilities, administrative geography and verified public-service information. Current officeholders, scheme status, political claims and election information are freshness-sensitive and must be dated/source-backed.

### Culture, tribes and history
Munda, Oraon/Kurukh, other communities relevant to Ranchi; Birsa Munda; Chota Nagpur history; regional movements; language; music; dance; crafts; folklore; museums and heritage. Historical sources remain historical evidence and are not used as proof of current conditions.

### Festivals and events
Sarhul, Karma and other locally relevant festivals, fairs and cultural events. Meaning/history can be offline/static; current procession routes, dates and schedules require fresh evidence.

### Nature and environment
Forests, rivers, waterfalls, biodiversity, wildlife, seasonality and weather-aware travel context.

### Weather
Current weather and forecasts are live/fresh data. General season/climate knowledge may be offline.

### Safety and practical needs
Safe-access notes when verified, emergency support, toilets, accessibility, lighting/closing constraints when sourceable, women/family considerations, child suitability and trip practicality. Avoid unsupported claims that a place is categorically safe/unsafe.

### Recreation and lifestyle
Sports grounds, gyms/fitness where sourceable, parks, cinemas, recreation, photography spots, evening activities and other everyday leisure needs.

### Everyday local services
ATMs/banks, pharmacies, fuel, repair/service categories, public toilets and similar local utility entities where reliable open data is available. Static existence/location must not be presented as live availability.

## Query styles
The benchmark must include:
- English
- Hindi
- Hinglish
- short fragments: `mutton kahan`, `child park`, `mandir near lalpur`
- misspellings
- conversational follow-ups
- `near me` and locality-dependent questions
- preference/constraint queries: parents, kids, budget, low walking, vegetarian, parking
- recommendation queries
- factual queries
- current/live queries
- ambiguous queries that require a short follow-up

## Freshness classes
Every useful fact should effectively belong to one of these classes:
1. STATIC_OR_SLOW: history, identity, geography, cultural meaning.
2. PRACTICAL_CHANGEABLE: address, facilities, accessibility, typical market day, restaurant/market metadata; refresh periodically.
3. LIVE: weather, open-now, current inventory, traffic, current fare, current event route/status, current officeholder; never imply freshness without fresh evidence.

## Retrieval/answer principle
Johar should answer from the strongest available evidence instead of reflexively asking to go online.

- Strong offline evidence -> answer directly with source cues.
- Partial evidence -> answer the known part and identify the missing part.
- Location missing for a near-me request -> ask only for location/locality.
- Live fact without fresh evidence -> clearly label the live portion as unavailable/stale; do not discard useful static context.
- No evidence -> say that the local corpus does not yet support the claim; never fabricate a top list, shop, timing, address or availability.

## Prototype success metric
Do not optimize for corpus megabytes.

Primary scorecard:
- broad persona coverage
- broad domain coverage
- locality coverage
- answerability rate
- partial-answer rate
- no-evidence rate
- missing-fact groups
- freshness-required rate
- wrong-result-type rate
- source/provenance completeness
- retrieval quality on the frozen retrieval suite

The Ranchi Coverage Eval is a separate product-coverage benchmark. Do not tune the frozen retrieval benchmark merely to improve its score.

## Data priority
Prioritize high-density practical Ranchi knowledge before adding more historical-book volume. Historical/cultural books remain valuable supporting sources, but the prototype is considered useful only when it can answer realistic daily-life and visitor questions.
