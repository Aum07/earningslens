# Retrieval eval: dev split, 2026-11-02

Retrieval only (no LLM calls). 34 items; metrics use the answerable ones.

Settings: k 16, k-per-period 5, max-periods 4, candidates 30 vector / 30 keyword, rrf-k 60, chunks 600 target / 100 overlap / 900 max tokens, corpus 3285 chunks, text-embedding-3-small.

## Overall

| Config | Answerable | Recall@k (all quotes) | Recall@k (any quote) | MRR | Period coverage | Avg chunks | Avg latency ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| C | 25 | 0.720 | 0.880 | 0.440 | 1.000 (27/27) | 16.2 | 363 |
| D | 25 | 0.720 | 0.880 | 0.440 | 1.000 (27/27) | 16.2 | 72 |

## By category

| Config | Category | Items | Recall@k (all) | Recall@k (any) | MRR | Period coverage |
|---|---|---:|---:|---:|---:|---:|
| C | single_fact | 11 | 1.000 | 1.000 | 0.481 | - |
| C | multi_quarter | 8 | 0.125 | 0.625 | 0.234 | 1.000 (27/27) |
| C | qualitative | 6 | 1.000 | 1.000 | 0.640 | - |
| D | single_fact | 11 | 1.000 | 1.000 | 0.481 | - |
| D | multi_quarter | 8 | 0.125 | 0.625 | 0.234 | 1.000 (27/27) |
| D | qualitative | 6 | 1.000 | 1.000 | 0.640 | - |

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

## Config D: items with a gold quote not retrieved (7)

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

## Evidence threshold (E3.7), from config C

Abstain when the best vector similarity is below the threshold. "Abstain" is the prediction for "unanswerable": precision = abstentions that were right, recall = unanswerable questions caught. Questions whose period is not in the corpus abstain at every threshold.

Suggested (highest F1, ties to the lower threshold): **0.584**: catches 8/9 unanswerable, blocks 2/25 answerable (q004, q009).

| Threshold | Unanswerable caught | Answerable blocked | Precision | Recall | F1 | Blocked answerable items |
|---:|---:|---:|---:|---:|---:|---|
| 0.000 | 3/9 | 0/25 | 1.000 | 0.333 | 0.500 |  |
| 0.511 | 4/9 | 0/25 | 1.000 | 0.444 | 0.615 |  |
| 0.531 | 5/9 | 0/25 | 1.000 | 0.556 | 0.714 |  |
| 0.551 | 5/9 | 1/25 | 0.833 | 0.556 | 0.667 | q009 |
| 0.561 | 6/9 | 1/25 | 0.857 | 0.667 | 0.750 | q009 |
| 0.571 | 6/9 | 2/25 | 0.750 | 0.667 | 0.706 | q004, q009 |
| 0.579 | 7/9 | 2/25 | 0.778 | 0.778 | 0.778 | q004, q009 |
| 0.584 | 8/9 | 2/25 | 0.800 | 0.889 | 0.842 | q004, q009 |
| 0.610 | 8/9 | 3/25 | 0.727 | 0.889 | 0.800 | q004, q005, q009 |
| 0.637 | 8/9 | 4/25 | 0.667 | 0.889 | 0.762 | q004, q005, q009, q010 |
| 0.643 | 8/9 | 5/25 | 0.615 | 0.889 | 0.727 | q004, q005, q009, q010, q024 |
| 0.648 | 8/9 | 6/25 | 0.571 | 0.889 | 0.696 | q004, q005, q008, q009, q010, q024 |
| 0.651 | 8/9 | 7/25 | 0.533 | 0.889 | 0.667 | q004, q005, q008, q009, q010, q011, q024 |
| 0.656 | 8/9 | 8/25 | 0.500 | 0.889 | 0.640 | q004, q005, q008, q009, q010, q011, q022, q024 |
| 0.660 | 8/9 | 9/25 | 0.471 | 0.889 | 0.615 | q003, q004, q005, q008, q009, q010, q011, q022, q024 |
| 0.664 | 8/9 | 10/25 | 0.444 | 0.889 | 0.593 | q003, q004, q005, q008, q009, q010, q011, q018, q022, q024 |
| 0.665 | 8/9 | 10/25 | 0.444 | 0.889 | 0.593 | q003, q004, q005, q008, q009, q010, q011, q018, q022, q024 |
| 0.675 | 8/9 | 12/25 | 0.400 | 0.889 | 0.552 | q003, q004, q005, q008, q009, q010, q011, q012, q016, q018, q022, q024 |
| 0.686 | 8/9 | 13/25 | 0.381 | 0.889 | 0.533 | q003, q004, q005, q008, q009, q010, q011, q012, q016, q018, q021, q022, q024 |
| 0.691 | 8/9 | 14/25 | 0.364 | 0.889 | 0.516 | q003, q004, q005, q008, q009, q010, q011, q012, q013, q016, q018, q021, q022, q024 |
| 0.696 | 8/9 | 15/25 | 0.348 | 0.889 | 0.500 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q018, q021, q022, q024 |
| 0.699 | 8/9 | 15/25 | 0.348 | 0.889 | 0.500 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q018, q021, q022, q024 |
| 0.701 | 8/9 | 17/25 | 0.320 | 0.889 | 0.471 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q017, q018, q019, q021, q022, q024 |
| 0.706 | 8/9 | 18/25 | 0.308 | 0.889 | 0.457 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q017, q018, q019, q021, q022, q023, q024 |
| 0.711 | 9/9 | 19/25 | 0.321 | 1.000 | 0.486 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q017, q018, q019, q021, q022, q023, q024, q025 |
| 0.720 | 9/9 | 19/25 | 0.321 | 1.000 | 0.486 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q017, q018, q019, q021, q022, q023, q024, q025 |
| 0.737 | 9/9 | 20/25 | 0.310 | 1.000 | 0.474 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q016, q017, q018, q019, q020, q021, q022, q023, q024, q025 |
| 0.748 | 9/9 | 21/25 | 0.300 | 1.000 | 0.462 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q014, q016, q017, q018, q019, q020, q021, q022, q023, q024, q025 |
| 0.752 | 9/9 | 22/25 | 0.290 | 1.000 | 0.450 | q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q014, q015, q016, q017, q018, q019, q020, q021, q022, q023, q024, q025 |
| 0.758 | 9/9 | 23/25 | 0.281 | 1.000 | 0.439 | q001, q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q014, q015, q016, q017, q018, q019, q020, q021, q022, q023, q024, q025 |
| 0.771 | 9/9 | 24/25 | 0.273 | 1.000 | 0.429 | q001, q002, q003, q004, q005, q007, q008, q009, q010, q011, q012, q013, q014, q015, q016, q017, q018, q019, q020, q021, q022, q023, q024, q025 |
| 0.781 | 9/9 | 25/25 | 0.265 | 1.000 | 0.419 | q001, q002, q003, q004, q005, q006, q007, q008, q009, q010, q011, q012, q013, q014, q015, q016, q017, q018, q019, q020, q021, q022, q023, q024, q025 |

Best vector similarity per item, lowest first:

| Item | Answerable | Best similarity | Note |
|---|---|---:|---|
| q026 | no | - | period not in corpus |
| q030 | no | - | period not in corpus |
| q031 | no | - | period not in corpus |
| q034 | no | 0.504 |  |
| q028 | no | 0.517 |  |
| q009 | yes | 0.545 |  |
| q033 | no | 0.557 |  |
| q004 | yes | 0.566 |  |
| q027 | no | 0.576 |  |
| q029 | no | 0.582 |  |
| q005 | yes | 0.587 |  |
| q010 | yes | 0.634 |  |
| q024 | yes | 0.639 |  |
| q008 | yes | 0.647 |  |
| q011 | yes | 0.648 |  |
| q022 | yes | 0.654 |  |
| q003 | yes | 0.657 |  |
| q018 | yes | 0.663 |  |
| q012 | yes | 0.665 |  |
| q016 | yes | 0.665 |  |
| q021 | yes | 0.685 |  |
| q013 | yes | 0.688 |  |
| q007 | yes | 0.694 |  |
| q017 | yes | 0.699 |  |
| q019 | yes | 0.699 |  |
| q023 | yes | 0.702 |  |
| q025 | yes | 0.710 |  |
| q032 | no | 0.711 |  |
| q020 | yes | 0.729 |  |
| q014 | yes | 0.746 |  |
| q015 | yes | 0.749 |  |
| q001 | yes | 0.755 |  |
| q002 | yes | 0.762 |  |
| q006 | yes | 0.780 |  |

## Unanswerable items

Best vector similarity per config (input for the E3.7 threshold), and any abstention.

| Item | C | D |
|---|---|---|
| q026 | - (PERIOD_NOT_IN_CORPUS) | - (PERIOD_NOT_IN_CORPUS) |
| q027 | 0.576 | 0.576 |
| q028 | 0.517 | 0.517 (LOW_SIMILARITY) |
| q029 | 0.582 | 0.582 |
| q030 | - (PERIOD_NOT_IN_CORPUS) | - (PERIOD_NOT_IN_CORPUS) |
| q031 | - (PERIOD_NOT_IN_CORPUS) | - (PERIOD_NOT_IN_CORPUS) |
| q032 | 0.711 | 0.711 |
| q033 | 0.557 | 0.557 |
| q034 | 0.504 | 0.504 (LOW_SIMILARITY) |
