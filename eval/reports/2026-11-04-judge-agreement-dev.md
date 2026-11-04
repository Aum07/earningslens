# Judge agreement with human labels: dev split, 2026-11-04

30 answers labelled by hand (24 from config C, 6 from config A). Judge: gpt-4.1, rubric prompt sha-256 `60091ff02757`.

## Faithfulness (yes/no)

The judge counts as "faithful" when every claim it found is supported.

| | Judge faithful | Judge not faithful |
|---|---|---|
| **Person faithful** | 22 | 1 |
| **Person not faithful** | 3 | 4 |

- Agreement: 87%
- Cohen's kappa: 0.59

## Correctness (1 to 5)

- Exact agreement: 70%
- Within one point: 90%
- Mean judge minus person: 0.37 (positive = the judge is more generous)

| Person \ Judge | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| **1** | 1 | 0 | 1 | 0 | 0 |
| **2** | 0 | 0 | 2 | 0 | 1 |
| **3** | 0 | 0 | 2 | 0 | 1 |
| **4** | 0 | 0 | 1 | 4 | 3 |
| **5** | 0 | 0 | 0 | 0 | 14 |

## Verdict

Below the bar (kappa at least 0.6 and correctness within one point at least 80%): improve the rubric before trusting judge numbers.

## Disagreements

- **q012-C** (multi_quarter): faithful person no / judge no; correctness person 2 / judge 5. Judge: The answer gives the correct sequence of capex outlook changes for each quarter, matching the reference answer in both the ranges and the narrowing/raising pattern. The only minor difference is that the answer includes some extra detail (reasons for the Q2 increase), but this does not detract from correctness. All key facts are present and correct. Unsupported: "The capital expenditure outlook increased from $115-135 billion in Q1 to $135-145 billion by Q4, reflecting ongoing investments in infrastructure and technology" (The increase in capex range is supported, but the explicit statement about 'ongoing investments in infrastructure and technology' as the reason is not directly stated in the sources for Q4.). Person's note: quarter labels are shifted: each range is assigned to the following quarter
- **q017-C** (multi_quarter): faithful person yes / judge no; correctness person 4 / judge 3. Judge: The answer gives the correct Q4 FY2026 and Q1 FY2027 total revenue figures, but the Q3 FY2026 total revenue ($82.9B) is not supported by any source provided. The sum total is thus also not fully supported. The reference answer lists all three quarters' revenues, so a key fact is missing or unsupported. Unsupported: "FY2026 Q3 total revenue was $82.9 billion" (Source [5] states 'Revenue was $90.0 billion' for Q4, but for Q3, the answer is not directly found in [1] or [4].). Unsupported: "Total revenue over the last three quarters is $265.2 billion" (This is a sum of the three quarterly revenues, but since the Q3 figure is not supported, the total is not fully supported.). Person's note: omits the year-over-year growth rates
- **q019-C** (multi_quarter): faithful person no / judge yes; correctness person 1 / judge 3. Judge: The answer provides general commentary about capital expenditures and investment focus for each quarter, but omits the specific forward-looking statements and quantitative guidance given by Amy Hood on the last three calls, such as next-quarter CapEx to rise to over $40B, FY27 CapEx to grow year over year, and Q2 CapEx over $50B. The answer instead focuses on past quarter figures and general investment themes, missing the key forward-looking details present in the reference answer. Person's note: Q4 $41B and Q1 $45.2B actual CapEx figures and related details are source-supported; however, the Q3 statements are attributed to Amy Hood, which the cited Q3 filings/press release do not support.
- **q022-C** (qualitative): faithful person no / judge yes; correctness person 4 / judge 5. Judge: The answer covers all key points from the reference: strong demand across a wide range of customers, expectation of continued scaling, and that TPU system sales contributed meaningfully to Cloud revenue growth. It adds some extra detail but does not contradict or omit any key fact. Person's note: the cited sources compare performance with prior generations, not competitors; omits Cloud revenue contribution
- **q012-A** (multi_quarter): faithful person no / judge yes; correctness person 4 / judge 5. Judge: The answer gives all four quarterly outlooks and their changes, matching the reference answer exactly in both figures and sequence. No key fact is missing or incorrect. Person's note: Q2 should be described as narrowed from $125-145B, not unchanged/remained
- **q014-A** (multi_quarter): faithful person yes / judge yes; correctness person 3 / judge 5. Judge: The answer gives the correct Azure and other cloud services growth rates for Q2 (39%) and Q4 (43%), matching the reference answer. It omits Q3, but the question only asks about Q2 and Q4, so this is not a required detail. All key facts are present and correct. Person's note: omits the Q3 FY2026 growth rate of 40%
