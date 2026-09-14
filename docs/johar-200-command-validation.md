# Johar 200-command validation matrix

Baseline: `feat/johar-real-chat-integration` after the green `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` build reported on 2026-09-14.

## How to use

Run these on a real installed debug build, preferably in a fresh conversation for each category when stateful clarification could affect later commands.

For each command record:
- **PASS** when the intent is routed correctly, the UI type is appropriate, and the answer does not invent unsupported facts.
- **DATA GAP** when routing/intent is correct but the offline corpus has no matching entity/fact.
- **PARSER GAP** when the app clearly routes to the wrong capability or fails to understand an intended supported form.
- **UI GAP** when the result is correct but rendered in the wrong card/state.
- **SAFETY/GROUNDING FAIL** when Johar invents live status, rating, price, availability, safety, ETA, place, or route.

A DATA GAP is not automatically a product bug. Keep it separate from parser/runtime/UI failures.

## Expected capability families

1. Knowledge/culture -> grounded text-first answer, retaining available evidence.
2. Place lookup -> strong real spatial match; one place card or compact multi-result UI.
3. Nearby -> explicit origin required; max 5 km straight-line discovery; never infer GPS.
4. Utilities/services -> real utility/service matches only; no invented opening/status.
5. Routing -> both endpoints must resolve uniquely; real offline A* route only.
6. Comparison -> both real places must resolve; comparison UI, no invented qualitative winner.
7. Itinerary -> explicit real stops only; preserve requested order unless a real optimizer exists.
8. Live guardrails -> clearly mark current/live data as unconfirmed unless a real live source is wired.
9. Ambiguity/typos -> conservative behavior; typo support may pass or surface a parser gap, but must never guess unsafe entities.
10. Negative/fallback -> no hallucinated place, route, price, status, or guarantee.

## Knowledge & culture
1. [ ] `Rugra kya hai?` — Result: ___
2. [ ] `Dhuska kya hota hai?` — Result: ___
3. [ ] `Sarhul kya hai?` — Result: ___
4. [ ] `Karma festival kya hai?` — Result: ___
5. [ ] `Sohrai art kya hai?` — Result: ___
6. [ ] `Khovar painting kya hai?` — Result: ___
7. [ ] `Chhau dance kya hai?` — Result: ___
8. [ ] `Johar ka matlab kya hai?` — Result: ___
9. [ ] `Jharkhand ka state animal kya hai?` — Result: ___
10. [ ] `Jharkhand ka state bird kya hai?` — Result: ___
11. [ ] `Jharkhand ka state tree kya hai?` — Result: ___
12. [ ] `Jharkhand ka state flower kya hai?` — Result: ___
13. [ ] `Ranchi kis liye famous hai?` — Result: ___
14. [ ] `Tagore Hill ka history batao` — Result: ___
15. [ ] `Pahari Mandir ka history batao` — Result: ___
16. [ ] `Ranchi me tribal culture ke bare me batao` — Result: ___
17. [ ] `Jharkhand ki local food list batao` — Result: ___
18. [ ] `Ranchi me kaun se local dishes try karni chahiye?` — Result: ___
19. [ ] `Sarhul festival kab hota hai?` — Result: ___
20. [ ] `Rugra seasonal food hai kya?` — Result: ___

## Place lookup
21. [ ] `Tagore Hill kahan hai?` — Result: ___
22. [ ] `Pahari Mandir kahan hai?` — Result: ___
23. [ ] `Ranchi railway station kahan hai?` — Result: ___
24. [ ] `Rock Garden kahan hai?` — Result: ___
25. [ ] `Kanke Dam kahan hai?` — Result: ___
26. [ ] `Jagannath Temple Ranchi kahan hai?` — Result: ___
27. [ ] `Hundru Falls kahan hai?` — Result: ___
28. [ ] `Dassam Falls kahan hai?` — Result: ___
29. [ ] `Jonha Falls kahan hai?` — Result: ___
30. [ ] `Birsa Munda Airport kahan hai?` — Result: ___
31. [ ] `Ranchi University kahan hai?` — Result: ___
32. [ ] `Morabadi Ground kahan hai?` — Result: ___
33. [ ] `Nakshatra Van kahan hai?` — Result: ___
34. [ ] `Tagore hill map pe dikhao` — Result: ___
35. [ ] `Ranchi Junction map pe dikhao` — Result: ___
36. [ ] `Pahari Mandir ka location dikhao` — Result: ___
37. [ ] `Lalpur kahan hai?` — Result: ___
38. [ ] `Doranda kahan hai?` — Result: ___
39. [ ] `Kanke kahan hai?` — Result: ___
40. [ ] `Hatia kahan hai?` — Result: ___

## Nearby discovery
41. [ ] `Lalpur ke paas mandir` — Result: ___
42. [ ] `Lalpur ke paas hospital` — Result: ___
43. [ ] `Lalpur ke paas pharmacy` — Result: ___
44. [ ] `Lalpur ke paas ATM` — Result: ___
45. [ ] `Lalpur ke paas bank` — Result: ___
46. [ ] `Lalpur ke paas petrol pump` — Result: ___
47. [ ] `Lalpur ke paas restaurant` — Result: ___
48. [ ] `Lalpur ke paas cafe` — Result: ___
49. [ ] `Lalpur ke paas park` — Result: ___
50. [ ] `Lalpur ke paas market` — Result: ___
51. [ ] `Ranchi railway station ke paas hotel` — Result: ___
52. [ ] `Ranchi railway station ke paas restaurant` — Result: ___
53. [ ] `Ranchi railway station ke paas ATM` — Result: ___
54. [ ] `Doranda ke paas hospital` — Result: ___
55. [ ] `Doranda ke paas pharmacy` — Result: ___
56. [ ] `Kanke ke paas park` — Result: ___
57. [ ] `Kanke ke paas cafe` — Result: ___
58. [ ] `Morabadi ke paas mandir` — Result: ___
59. [ ] `Morabadi ke paas restaurant` — Result: ___
60. [ ] `mere aas paas mandir?` — Result: ___

## Utilities & services
61. [ ] `Ranchi me hospital dhoondo` — Result: ___
62. [ ] `Ranchi me pharmacy dhoondo` — Result: ___
63. [ ] `Ranchi me police station dhoondo` — Result: ___
64. [ ] `Ranchi me fire station dhoondo` — Result: ___
65. [ ] `Ranchi me ATM dhoondo` — Result: ___
66. [ ] `Ranchi me bank dhoondo` — Result: ___
67. [ ] `Ranchi me petrol pump dhoondo` — Result: ___
68. [ ] `Ranchi me EV charging station dhoondo` — Result: ___
69. [ ] `Ranchi me public toilet dhoondo` — Result: ___
70. [ ] `Ranchi me parking dhoondo` — Result: ___
71. [ ] `Ranchi me library dhoondo` — Result: ___
72. [ ] `Ranchi me school dhoondo` — Result: ___
73. [ ] `Ranchi me college dhoondo` — Result: ___
74. [ ] `Ranchi me university dhoondo` — Result: ___
75. [ ] `Ranchi me hotel dhoondo` — Result: ___
76. [ ] `Ranchi me cafe dhoondo` — Result: ___
77. [ ] `Ranchi me restaurant dhoondo` — Result: ___
78. [ ] `Ranchi me market dhoondo` — Result: ___
79. [ ] `Ranchi me mall dhoondo` — Result: ___
80. [ ] `Ranchi me cinema dhoondo` — Result: ___

## Routing
81. [ ] `Tagore Hill se Ranchi railway station kaise jaye?` — Result: ___
82. [ ] `Ranchi railway station se Tagore Hill kaise jaye?` — Result: ___
83. [ ] `Tagore Hill se Pahari Mandir ka route batao` — Result: ___
84. [ ] `Pahari Mandir se Ranchi railway station kaise jaye?` — Result: ___
85. [ ] `Ranchi railway station se Birsa Munda Airport kaise jaye?` — Result: ___
86. [ ] `Birsa Munda Airport se Ranchi railway station ka route batao` — Result: ___
87. [ ] `Tagore Hill se Rock Garden kaise jaye?` — Result: ___
88. [ ] `Rock Garden se Tagore Hill ka route batao` — Result: ___
89. [ ] `Kanke Dam se Tagore Hill kaise jaye?` — Result: ___
90. [ ] `Tagore Hill se Kanke Dam ka route batao` — Result: ___
91. [ ] `Ranchi Junction to Pahari Mandir` — Result: ___
92. [ ] `route from Ranchi Junction to Tagore Hill` — Result: ___
93. [ ] `route from Tagore Hill to Ranchi station` — Result: ___
94. [ ] `navigate from Tagore Hill to Ranchi station` — Result: ___
95. [ ] `Ranchi station se Jagannath Temple kaise jaye?` — Result: ___
96. [ ] `Jagannath Temple se Ranchi station ka route batao` — Result: ___
97. [ ] `Morabadi Ground se Ranchi Junction kaise jaye?` — Result: ___
98. [ ] `Ranchi Junction se Morabadi Ground ka route batao` — Result: ___
99. [ ] `Lalpur se Ranchi railway station kaise jaye?` — Result: ___
100. [ ] `Doranda se Ranchi railway station kaise jaye?` — Result: ___

## Comparison
101. [ ] `Tagore Hill vs Rock Garden` — Result: ___
102. [ ] `Tagore Hill ya Rock Garden` — Result: ___
103. [ ] `compare Tagore Hill vs Rock Garden` — Result: ___
104. [ ] `Pahari Mandir vs Jagannath Temple` — Result: ___
105. [ ] `Kanke Dam vs Rock Garden` — Result: ___
106. [ ] `Hundru Falls vs Dassam Falls` — Result: ___
107. [ ] `Dassam Falls vs Jonha Falls` — Result: ___
108. [ ] `Hundru Falls ya Jonha Falls` — Result: ___
109. [ ] `Tagore Hill vs Pahari Mandir which is better` — Result: ___
110. [ ] `Ranchi Junction vs Hatia station` — Result: ___
111. [ ] `Lalpur vs Doranda` — Result: ___
112. [ ] `Kanke vs Lalpur` — Result: ___
113. [ ] `Rock Garden vs Kanke Dam which is better` — Result: ___
114. [ ] `Pahari Mandir ya Tagore Hill` — Result: ___
115. [ ] `Hundru Falls versus Dassam Falls` — Result: ___
116. [ ] `Birsa Munda Airport vs Ranchi Junction` — Result: ___
117. [ ] `Morabadi Ground vs Tagore Hill` — Result: ___
118. [ ] `Jagannath Temple vs Pahari Mandir` — Result: ___
119. [ ] `Kanke Dam ya Tagore Hill` — Result: ___
120. [ ] `Tagore Hill or Rock Garden` — Result: ___

## Itinerary
121. [ ] `Plan Tagore Hill, Rock Garden and Kanke Dam` — Result: ___
122. [ ] `Plan Tagore Hill, Pahari Mandir and Ranchi Junction` — Result: ___
123. [ ] `itinerary Tagore Hill, Rock Garden, Kanke Dam` — Result: ___
124. [ ] `plan Pahari Mandir, Jagannath Temple, Tagore Hill` — Result: ___
125. [ ] `plan Ranchi Junction, Tagore Hill, Rock Garden` — Result: ___
126. [ ] `plan Tagore Hill and Rock Garden` — Result: ___
127. [ ] `plan Hundru Falls, Dassam Falls and Jonha Falls` — Result: ___
128. [ ] `plan Kanke Dam, Rock Garden and Tagore Hill` — Result: ___
129. [ ] `plan Birsa Munda Airport, Ranchi Junction and Tagore Hill` — Result: ___
130. [ ] `plan Lalpur, Tagore Hill and Ranchi Junction` — Result: ___
131. [ ] `plan Doranda, Ranchi Junction and Pahari Mandir` — Result: ___
132. [ ] `plan Morabadi Ground, Tagore Hill and Rock Garden` — Result: ___
133. [ ] `itinerary Pahari Mandir, Tagore Hill` — Result: ___
134. [ ] `itinerary Ranchi Junction, Pahari Mandir, Tagore Hill` — Result: ___
135. [ ] `plan Jagannath Temple, Pahari Mandir and Tagore Hill` — Result: ___
136. [ ] `plan Tagore Hill, Ranchi Junction, Birsa Munda Airport` — Result: ___
137. [ ] `plan Rock Garden, Kanke Dam` — Result: ___
138. [ ] `plan Ranchi Junction and Birsa Munda Airport` — Result: ___
139. [ ] `itinerary Tagore Hill, Rock Garden, Ranchi Junction` — Result: ___
140. [ ] `plan Pahari Mandir, Morabadi Ground, Ranchi Junction` — Result: ___

## Live guardrails
141. [ ] `Pahari Mandir open now?` — Result: ___
142. [ ] `Tagore Hill open today?` — Result: ___
143. [ ] `Rock Garden open now?` — Result: ___
144. [ ] `Kanke Dam open today?` — Result: ___
145. [ ] `Ranchi railway station crowded now?` — Result: ___
146. [ ] `Ranchi traffic kaisa hai abhi?` — Result: ___
147. [ ] `Ranchi weather abhi kaisa hai?` — Result: ___
148. [ ] `Pahari Mandir ki aaj timing kya hai?` — Result: ___
149. [ ] `Tagore Hill ki opening hours kya hai?` — Result: ___
150. [ ] `Kanke Dam aaj open hai?` — Result: ___
151. [ ] `Ranchi station pe train availability kya hai?` — Result: ___
152. [ ] `Ranchi me hotel available hai abhi?` — Result: ___
153. [ ] `Ranchi me parking available hai abhi?` — Result: ___
154. [ ] `Ranchi me petrol available hai abhi?` — Result: ___
155. [ ] `Ranchi me pharmacy open now?` — Result: ___
156. [ ] `Ranchi me restaurant open now?` — Result: ___
157. [ ] `Ranchi me ATM working hai abhi?` — Result: ___
158. [ ] `Ranchi me EV charging available hai abhi?` — Result: ___
159. [ ] `Hundru Falls aaj open hai?` — Result: ___
160. [ ] `Dassam Falls ka current status kya hai?` — Result: ___

## Ambiguity & typo tolerance
161. [ ] `Tagore hil kahan hai?` — Result: ___
162. [ ] `Tagor Hill kahan hai?` — Result: ___
163. [ ] `Ranchi staton kahan hai?` — Result: ___
164. [ ] `Pahari mandr kahan hai?` — Result: ___
165. [ ] `Ranchi railwy station kahan hai?` — Result: ___
166. [ ] `Rock Gardn kahan hai?` — Result: ___
167. [ ] `Kanke damm kahan hai?` — Result: ___
168. [ ] `Jagannath templ Ranchi kahan hai?` — Result: ___
169. [ ] `Hundru fall kahan hai?` — Result: ___
170. [ ] `Dassam fall kahan hai?` — Result: ___
171. [ ] `Jonha waterfall kahan hai?` — Result: ___
172. [ ] `mere paas hospital` — Result: ___
173. [ ] `mere paas pharmacy` — Result: ___
174. [ ] `mere paas ATM` — Result: ___
175. [ ] `mere paas restaurant` — Result: ___
176. [ ] `station ke paas hotel` — Result: ___
177. [ ] `mandir ke paas market` — Result: ___
178. [ ] `hospital ke paas pharmacy` — Result: ___
179. [ ] `Tagore Hill se station kaise jaye?` — Result: ___
180. [ ] `Ranchi se Tagore Hill kaise jaye?` — Result: ___

## Negative & fallback
181. [ ] `Pluto Ranchi me kahan hai?` — Result: ___
182. [ ] `Ranchi me Eiffel Tower kahan hai?` — Result: ___
183. [ ] `Ranchi me beach dhoondo` — Result: ___
184. [ ] `Ranchi me ski resort dhoondo` — Result: ___
185. [ ] `Ranchi me metro station dhoondo` — Result: ___
186. [ ] `Ranchi me sea port kahan hai?` — Result: ___
187. [ ] `Moon se Tagore Hill ka route batao` — Result: ___
188. [ ] `Tagore Hill se Mars kaise jaye?` — Result: ___
189. [ ] `compare Tagore Hill vs Eiffel Tower` — Result: ___
190. [ ] `plan Tagore Hill, Mars and Ranchi Junction` — Result: ___
191. [ ] `mere aas paas unicorn park` — Result: ___
192. [ ] `Ranchi me 24x7 guaranteed doctor kahan milega?` — Result: ___
193. [ ] `Ranchi me safest place kaunsa hai?` — Result: ___
194. [ ] `best restaurant with 5 star rating in Ranchi` — Result: ___
195. [ ] `cheapest hotel in Ranchi right now` — Result: ___
196. [ ] `Ranchi me current petrol price kya hai?` — Result: ___
197. [ ] `Ranchi me current train schedule batao` — Result: ___
198. [ ] `Ranchi me live traffic route batao` — Result: ___
199. [ ] `Ranchi me koi fake place batao` — Result: ___
200. [ ] `random gibberish xyzabc` — Result: ___

## Exit criteria

Do not call the 200-command run successful from a raw percentage alone. Before merging, separately summarize:

- parser/runtime failures;
- data-coverage gaps;
- UI presentation gaps;
- grounding/safety failures;
- slow/crash/ANR behavior;
- stateful conversation failures;
- map/routing failures.

A grounding/safety failure has higher priority than a missing-data answer. A crash or fabricated route/live fact is a release blocker.
