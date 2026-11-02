# Retrieval eval: dev split, 2026-11-02

Retrieval only (no LLM calls). 34 items; metrics use the answerable ones.

Settings: k 16, k-per-period 5, max-periods 4, candidates 30 vector / 30 keyword, rrf-k 60, chunks 600 target / 100 overlap / 900 max tokens, corpus 3285 chunks, text-embedding-3-small.

## Overall

| Config | Answerable | Recall@k (all quotes) | Recall@k (any quote) | MRR | Period coverage | Avg chunks | Avg latency ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| A | 25 | 0.560 | 0.720 | 0.343 | 1.000 (27/27) | 16.0 | 19 |
| B | 25 | 0.560 | 0.760 | 0.388 | 1.000 (27/27) | 16.0 | 72 |
| C | 25 | 0.720 | 0.880 | 0.440 | 1.000 (27/27) | 16.2 | 74 |

## By category

| Config | Category | Items | Recall@k (all) | Recall@k (any) | MRR | Period coverage |
|---|---|---:|---:|---:|---:|---:|
| A | single_fact | 11 | 0.727 | 0.727 | 0.297 | - |
| A | multi_quarter | 8 | 0.125 | 0.625 | 0.246 | 1.000 (27/27) |
| A | qualitative | 6 | 0.833 | 0.833 | 0.557 | - |
| B | single_fact | 11 | 0.818 | 0.818 | 0.437 | - |
| B | multi_quarter | 8 | 0.000 | 0.625 | 0.287 | 1.000 (27/27) |
| B | qualitative | 6 | 0.833 | 0.833 | 0.431 | - |
| C | single_fact | 11 | 1.000 | 1.000 | 0.481 | - |
| C | multi_quarter | 8 | 0.125 | 0.625 | 0.234 | 1.000 (27/27) |
| C | qualitative | 6 | 1.000 | 1.000 | 0.640 | - |

## Config A: items with a gold quote not retrieved (11)

| Item | Category | Found | First hit rank | Strategy | Searched | Missed quote |
|---|---|---:|---:|---|---|---|
| q001 | single_fact | 0/1 | - | SINGLE |  | Revenue was $81.3 billion and increased 17% (up 15% in constant currency) |
| q007 | single_fact | 0/1 | - | SINGLE |  | DAP was 3.58 billion on average for December 2025, an increase of 7% year-ove... |
| q009 | single_fact | 0/1 | - | SINGLE |  | This total includes approximately 8,000 employees impacted by the May 2026 he... |
| q012 | multi_quarter | 3/4 | 1 | SINGLE |  | to be in the range of $130-145 billion, narrowed from our prior outlook of $1... |
| q013 | multi_quarter | 3/4 | 3 | SINGLE |  | our 2026 CapEx investments are anticipated to be in the range of $175 billion... |
| q015 | multi_quarter | 0/4 | - | SINGLE |  | revenues increased 48% to $17.7 billion |
| q016 | multi_quarter | 0/3 | - | SINGLE |  | Revenue was $56.31 billion, an increase of 33% year-over-year. |
| q017 | multi_quarter | 1/3 | 10 | SINGLE |  | Revenue was $82.9 billion and increased 18% (up 15% in constant currency) |
| q018 | multi_quarter | 0/3 | - | SINGLE |  | Capital expenditures, including principal payments on finance leases, were $1... |
| q019 | multi_quarter | 2/3 | 5 | SINGLE |  | we expect FY27 capital expenditures will grow year-over-year given demand sig... |
| q022 | qualitative | 0/1 | - | SINGLE |  | we're seeing strong demand for our TPU systems across a wide range of custome... |

## Config B: items with a gold quote not retrieved (11)

| Item | Category | Found | First hit rank | Strategy | Searched | Missed quote |
|---|---|---:|---:|---|---|---|
| q001 | single_fact | 0/1 | - | SINGLE |  | Revenue was $81.3 billion and increased 17% (up 15% in constant currency) |
| q009 | single_fact | 0/1 | - | SINGLE |  | This total includes approximately 8,000 employees impacted by the May 2026 he... |
| q012 | multi_quarter | 3/4 | 1 | SINGLE |  | to be in the range of $130-145 billion, narrowed from our prior outlook of $1... |
| q013 | multi_quarter | 3/4 | 1 | SINGLE |  | our 2026 CapEx investments are anticipated to be in the range of $175 billion... |
| q014 | multi_quarter | 1/3 | 13 | SINGLE |  | Azure and other cloud services revenue increased 40% (up 39% in constant curr... |
| q015 | multi_quarter | 0/4 | - | SINGLE |  | revenues increased 48% to $17.7 billion |
| q016 | multi_quarter | 0/3 | - | SINGLE |  | Revenue was $56.31 billion, an increase of 33% year-over-year. |
| q017 | multi_quarter | 0/3 | - | SINGLE |  | Revenue was $82.9 billion and increased 18% (up 15% in constant currency) |
| q018 | multi_quarter | 1/3 | 9 | SINGLE |  | Capital expenditures, including principal payments on finance leases, were $1... |
| q019 | multi_quarter | 2/3 | 9 | SINGLE |  | we expect FY27 capital expenditures will grow year-over-year given demand sig... |
| q022 | qualitative | 0/1 | - | SINGLE |  | we're seeing strong demand for our TPU systems across a wide range of custome... |

## Config C: items with a gold quote not retrieved (7)

| Item | Category | Found | First hit rank | Strategy | Searched | Missed quote |
|---|---|---:|---:|---|---|---|
| q013 | multi_quarter | 3/4 | 2 | PER_PERIOD | GOOGL FY2025 Q4, GOOGL FY2026 Q1, GOOGL FY2026 Q2, GOOGL FY2026 Q3 | our 2026 CapEx investments are anticipated to be in the range of $175 billion... |
| q014 | multi_quarter | 0/3 | - | PER_PERIOD | MSFT FY2026 Q2, MSFT FY2026 Q3, MSFT FY2026 Q4 | Azure and other cloud services revenue increased 39% (up 38% in constant curr... |
| q015 | multi_quarter | 1/4 | 20 | PER_PERIOD | GOOGL FY2025 Q4, GOOGL FY2026 Q1, GOOGL FY2026 Q2, GOOGL FY2026 Q3 | revenues increased 48% to $17.7 billion |
| q016 | multi_quarter | 0/3 | - | PER_PERIOD | META FY2026 Q1, META FY2026 Q2, META FY2026 Q3 | Revenue was $56.31 billion, an increase of 33% year-over-year. |
| q017 | multi_quarter | 2/3 | 5 | PER_PERIOD | MSFT FY2026 Q3, MSFT FY2026 Q4, MSFT FY2027 Q1 | Revenue was $82.9 billion and increased 18% (up 15% in constant currency) |
| q018 | multi_quarter | 0/3 | - | PER_PERIOD | META FY2026 Q1, META FY2026 Q2, META FY2026 Q3 | Capital expenditures, including principal payments on finance leases, were $1... |
| q019 | multi_quarter | 1/3 | 8 | PER_PERIOD | MSFT FY2026 Q3, MSFT FY2026 Q4, MSFT FY2027 Q1 | We expect CapEx spend to increase to over $40 billion as we continue to bring... |

## Where the gold chunks rank within their own quarter

Vector and keyword search limited to the quote's company and quarter, with the full question. A rank above k means fusion cannot pick that chunk; "-" means not found at all.

Vector rank of the gold chunk: 46 quotes: 24 at rank 1-3, 6 at 4-8, 9 at 9-30, 7 below 30 or not found  
Keyword rank of the gold chunk: 46 quotes: 16 at rank 1-3, 12 at 4-8, 14 at 9-30, 4 below 30 or not found

Replaying the real search for that quarter (top 30 vector + top 30 keyword, RRF, dedupe), gold chunk in top 16: 40 of 46 before dedupe, 40 after dedupe.

| Item | Evidence | Vector rank (all) | Vector rank (top 30) | Keyword rank | Fused rank | After dedupe | Chunks in quarter | Quote |
|---|---|---:|---:|---:|---:|---:|---:|---|
| q001 | MSFT FY2026 Q2 PRESS_RELEASE | 12 | 12 | 21 | 13 | 13 | 226 | Revenue was $81.3 billion and increased 17% (up 15% in constant cur... |
| q002 | MSFT FY2027 Q1 PRESS_RELEASE | 2 | 2 | 4 | 2 | 2 | 224 | Microsoft Cloud revenue was $63.9 billion and increased 30% (up 31%... |
| q003 | MSFT FY2026 Q4 PRESS_RELEASE | 1 | 1 | 6 | 1 | 1 | 320 | a $3.2 billion gain from our investment in Anthropic |
| q004 | GOOGL FY2026 Q1 PRESS_RELEASE | 1 | 1 | 1 | 1 | 1 | 205 | Waymo surpass 500,000 fully autonomous rides a week |
| q005 | GOOGL FY2026 Q2 PRESS_RELEASE | 4 | 4 | 4 | 3 | 3 | 234 | the Gemini App has 950 million monthly active users |
| q006 | GOOGL FY2026 Q1 PRESS_RELEASE | 2 | 2 | 1 | 1 | 1 | 205 | The company announced a 5% increase to the dividend, resulting in a... |
| q007 | META FY2025 Q4 PRESS_RELEASE | 18 | 18 | 2 | 6 | 6 | 385 | DAP was 3.58 billion on average for December 2025, an increase of 7... |
| q008 | META FY2026 Q3 PRESS_RELEASE | 5 | 5 | 37 | 14 | 14 | 304 | Third quarter 2026 costs and expenses includes a $9.89 billion char... |
| q009 | META FY2026 Q2 TRANSCRIPT | 30 | 30 | 12 | 6 | 6 | 289 | This total includes approximately 8,000 employees impacted by the M... |
| q010 | META FY2026 Q3 TRANSCRIPT | 1 | 1 | 1 | 1 | 1 | 304 | more than 10 million people in the US and Canada have set up Muse p... |
| q011 | GOOGL FY2026 Q3 FORM_10Q | 2 | 2 | 28 | 6 | 6 | 247 | In the three and nine months ended September 30, 2026, there were n... |
| q012 | META FY2025 Q4 PRESS_RELEASE | 1 | 1 | 1 | 1 | 1 | 385 | to be in the range of $115-135 billion, with year-over-year growth ... |
| q012 | META FY2026 Q1 PRESS_RELEASE | 1 | 1 | 2 | 1 | 1 | 285 | to be in the range of $125-145 billion, increased from our prior ra... |
| q012 | META FY2026 Q2 PRESS_RELEASE | 16 | 16 | 3 | 1 | 1 | 289 | to be in the range of $130-145 billion, narrowed from our prior out... |
| q012 | META FY2026 Q3 PRESS_RELEASE | 2 | 2 | 2 | 1 | 1 | 304 | to be in the range of $135-145 billion, narrowed from our prior out... |
| q013 | GOOGL FY2025 Q4 TRANSCRIPT | 32 | - | 14 | 25 | 25 | 339 | our 2026 CapEx investments are anticipated to be in the range of $1... |
| q013 | GOOGL FY2026 Q1 TRANSCRIPT | 2 | 2 | 4 | 1 | 1 | 205 | we are updating our full year 2026 CapEx guidance range to $180 to ... |
| q013 | GOOGL FY2026 Q2 TRANSCRIPT | 2 | 2 | 3 | 1 | 1 | 234 | we are updating our full year 2026 CapEx guidance range to $195-205... |
| q013 | GOOGL FY2026 Q3 TRANSCRIPT | 1 | 1 | 1 | 1 | 1 | 247 | our full-year 2026 CapEx guidance range of $195-$205 billion remain... |
| q014 | MSFT FY2026 Q2 PRESS_RELEASE | 2 | 2 | 15 | 7 | 7 | 226 | Azure and other cloud services revenue increased 39% (up 38% in con... |
| q014 | MSFT FY2026 Q3 PRESS_RELEASE | 2 | 2 | 19 | 9 | 9 | 227 | Azure and other cloud services revenue increased 40% (up 39% in con... |
| q014 | MSFT FY2026 Q4 PRESS_RELEASE | 3 | 3 | 27 | 9 | 9 | 320 | Azure and other cloud services revenue increased 43% |
| q015 | GOOGL FY2025 Q4 PRESS_RELEASE | 24 | 24 | 3 | 10 | 10 | 339 | revenues increased 48% to $17.7 billion |
| q015 | GOOGL FY2026 Q1 PRESS_RELEASE | 20 | 20 | 6 | 12 | 12 | 205 | revenues increased 63% to $20.0 billion |
| q015 | GOOGL FY2026 Q2 PRESS_RELEASE | 17 | 17 | 9 | 12 | 12 | 234 | revenues increased 82% to $24.8 billion |
| q015 | GOOGL FY2026 Q3 PRESS_RELEASE | 11 | 11 | 8 | 5 | 5 | 247 | revenues increased 103% to $30.7 billion |
| q016 | META FY2026 Q1 PRESS_RELEASE | 114 | - | 16 | 30 | 30 | 285 | Revenue was $56.31 billion, an increase of 33% year-over-year. |
| q016 | META FY2026 Q2 PRESS_RELEASE | 84 | - | 15 | 30 | 30 | 289 | Revenue was $60.80 billion, an increase of 28% year-over-year. |
| q016 | META FY2026 Q3 PRESS_RELEASE | 109 | - | 51 | - | - | 304 | Revenue was $64.60 billion, an increase of 26% year-over-year |
| q017 | MSFT FY2026 Q3 PRESS_RELEASE | 8 | 8 | 15 | 6 | 6 | 227 | Revenue was $82.9 billion and increased 18% (up 15% in constant cur... |
| q017 | MSFT FY2026 Q4 PRESS_RELEASE | 3 | 3 | 6 | 2 | 2 | 320 | Revenue was $90.0 billion and increased 18% (up 17% in constant cur... |
| q017 | MSFT FY2027 Q1 PRESS_RELEASE | 1 | 1 | 14 | 4 | 4 | 224 | Revenue was $92.3 billion and increased 19% |
| q018 | META FY2026 Q1 PRESS_RELEASE | 122 | - | 2 | 11 | 11 | 285 | Capital expenditures, including principal payments on finance lease... |
| q018 | META FY2026 Q2 PRESS_RELEASE | 92 | - | 1 | 9 | 9 | 289 | Capital expenditures, including principal payments on finance lease... |
| q018 | META FY2026 Q3 PRESS_RELEASE | 113 | - | 15 | 31 | 31 | 304 | Capital expenditures, including principal payments on finance lease... |
| q019 | MSFT FY2026 Q3 TRANSCRIPT | 2 | 2 | 32 | 10 | 10 | 227 | We expect CapEx spend to increase to over $40 billion as we continu... |
| q019 | MSFT FY2026 Q4 TRANSCRIPT | 7 | 7 | 20 | 3 | 3 | 320 | we expect FY27 capital expenditures will grow year-over-year given ... |
| q019 | MSFT FY2027 Q1 TRANSCRIPT | 2 | 2 | 49 | 17 | 17 | 224 | In Q2, we expect CapEx spend will be over $50 billion. |
| q020 | MSFT FY2027 Q1 TRANSCRIPT | 1 | 1 | 2 | 1 | 1 | 224 | We think of Copilot as the new OS for work. |
| q020 | MSFT FY2027 Q1 TRANSCRIPT | 1 | 1 | 2 | 1 | 1 | 224 | bringing together Chat, Cowork, Autopilot, and Code into one unifie... |
| q021 | META FY2026 Q3 TRANSCRIPT | 4 | 4 | 5 | 4 | 4 | 304 | Muse is the product form of this vision. |
| q021 | META FY2026 Q3 TRANSCRIPT | 4 | 4 | 5 | 4 | 4 | 304 | We had one of our most successful launches ever with the Muse perso... |
| q022 | GOOGL FY2026 Q3 TRANSCRIPT | 28 | 28 | 8 | 11 | 11 | 247 | we're seeing strong demand for our TPU systems across a wide range ... |
| q023 | MSFT FY2026 Q2 TRANSCRIPT | 1 | 1 | 5 | 1 | 1 | 226 | rising memory prices would impact capital expenditures, though the ... |
| q024 | META FY2026 Q1 PRESS_RELEASE | 1 | 1 | 7 | 1 | 1 | 285 | This reflects our expectations for higher component pricing this ye... |
| q025 | GOOGL FY2026 Q1 TRANSCRIPT | 2 | 2 | 3 | 2 | 2 | 205 | As a result, we expect our 2027 CapEx to significantly increase com... |

## Unanswerable items

Best vector similarity per config (input for the E3.7 threshold), and any abstention.

| Item | A | B | C |
|---|---|---|---|
| q026 | 0.742 | 0.742 | - (PERIOD_NOT_IN_CORPUS) |
| q027 | 0.576 | 0.576 | 0.576 |
| q028 | 0.517 | 0.517 | 0.517 |
| q029 | 0.721 | 0.721 | 0.582 |
| q030 | 0.662 | 0.662 | - (PERIOD_NOT_IN_CORPUS) |
| q031 | 0.696 | 0.696 | - (PERIOD_NOT_IN_CORPUS) |
| q032 | 0.711 | 0.711 | 0.711 |
| q033 | 0.557 | 0.557 | 0.557 |
| q034 | 0.504 | 0.504 | 0.504 |
