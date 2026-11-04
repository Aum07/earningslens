# Judge agreement with human labels: dev split, 2026-11-04

30 answers labelled by hand (24 from config C, 6 from config A). Judge: gpt-4.1, rubric prompt sha-256 `68dde8982907`.

## Faithfulness (yes/no)

The judge counts as "faithful" when every claim it found is supported.

| | Judge faithful | Judge not faithful |
|---|---|---|
| **Person faithful** | 22 | 1 |
| **Person not faithful** | 2 | 5 |

- Agreement: 90%
- Cohen's kappa: 0.71

## Correctness (1 to 5)

- Exact agreement: 70%
- Within one point: 93%
- Mean judge minus person: 0.33 (positive = the judge is more generous)

| Person \ Judge | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| **1** | 1 | 1 | 0 | 0 | 0 |
| **2** | 0 | 0 | 2 | 0 | 1 |
| **3** | 0 | 0 | 2 | 0 | 1 |
| **4** | 0 | 0 | 1 | 4 | 3 |
| **5** | 0 | 0 | 0 | 0 | 14 |

## Verdict

Meets the bar (kappa at least 0.6 and correctness within one point at least 80%).

## Disagreements

- **q012-C** (multi_quarter): faithful person no / judge no; correctness person 2 / judge 5. Judge: The answer gives the correct capex outlook ranges for each of the last four quarters, matching the reference answer's sequence: $115-135B (Q4 2025), $125-145B (Q1 2026), $130-145B (Q2), $135-145B (Q3). The answer labels the quarters as Q1-Q4 of 2026, but the reference answer's Q4 2025 is the same as the first quarter in the sequence (the Q4 2025 guidance for 2026 capex). All key facts are present and correct, with no contradiction. Unsupported: "The capital expenditure outlook increased from $115-135 billion in Q1 to $135-145 billion by Q4, reflecting ongoing investments in infrastructure and technology" (This is a summary statement; the specific figures are supported above, but the phrase 'reflecting ongoing investments in infrastructure and technology' is not directly stated in the sources.). Person's note: quarter labels are shifted: each range is assigned to the following quarter
- **q017-C** (multi_quarter): faithful person yes / judge no; correctness person 4 / judge 3. Judge: The answer gives the correct total revenue for Q4 FY2026 and Q1 FY2027, but the Q3 FY2026 figure ($82.9B) is not supported by the sources, and thus the total sum is also not fully supported. The reference answer lists all three quarters, so missing or unsupported data for one quarter is a key omission. Thus, the answer is only partly correct. Unsupported: "FY2026 Q3 total revenue was $82.9 billion" (Source [5] states 'Revenue was $90.0 billion' for Q4, but for Q3, the answer uses $82.9 billion, which is not directly stated in [1] or [4].). Unsupported: "Total revenue over the last three quarters is $265.2 billion" (This is a sum of the three quarterly revenues, but since the Q3 figure is not supported, the total is not supported.). Person's note: omits the year-over-year growth rates
- **q018-C** (multi_quarter): faithful person no / judge yes; correctness person 2 / judge 3. Judge: The answer gives the correct Q1 figure ($19.84B), but for Q2 and Q3 it does not provide the actual quarterly capital expenditures as in the reference answer. Instead, it only gives the full-year outlook ranges for those quarters, which is not what was asked. The reference answer provides specific figures for Q2 ($31.08B) and Q3 ($34.91B), but the answer says the information is insufficient for Q2 and gives only a full-year range for Q3. Thus, the main fact for Q1 is correct, but the key facts for Q2 and Q3 are missing or incomplete. Person's note: confuses annual CapEx guidance with quarterly spend; Q2 and Q3 amounts are not answered
- **q012-A** (multi_quarter): faithful person no / judge yes; correctness person 4 / judge 5. Judge: All four quarters' capex outlooks are present and match the reference answer: $115-135B (Q4 2025), raised to $125-145B (Q1 2026), narrowed to $130-145B (Q2), narrowed to $135-145B (Q3). No key fact is missing or incorrect. Person's note: Q2 should be described as narrowed from $125-145B, not unchanged/remained
- **q014-A** (multi_quarter): faithful person yes / judge yes; correctness person 3 / judge 5. Judge: The answer correctly states the Azure and other cloud services growth rates for Q2 (39%) and Q4 (43%), matching the reference answer. The answer omits Q3, but the question only asks about Q2 to Q4, so this is not a required detail. All key facts are present and correct. Person's note: omits the Q3 FY2026 growth rate of 40%
