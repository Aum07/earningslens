# Error analysis: final test-split run (E5.5)

Source reports (committed in 92b3df7): `eval/reports/2026-11-04-{A,B,C,D}-test.json` and `2026-11-04-ablation-test.md`. Every number below comes from those files. Settings were frozen and committed (0ca1484) before the run.

Scope: every **config C** test item with any problem (C is the full retrieval pipeline the ablation builds up to), plus the items where D differs from C. An item counts as failed if, on an answerable item, the system abstained, the judge gave correctness 3 or lower, faithfulness was below 1, a number was not found in the cited sources, or retrieval missed a gold quote; or if it answered an unanswerable item. 20 of 50 C items meet at least one of these. Each was read in full (question, gold answer and evidence, retrieved-quote result, answer, judge reasons, ungrounded numbers).

No settings were changed after reading these. Any fix below becomes a new, clearly labelled run on dev (roadmap E5.5 rule).

## Counts (config C, one primary cause per item)

| Group | Items | Count | Real system failure? |
|---|---|---|---|
| Company named only by a product, so no per-quarter retrieval | q067 (Xbox), q068 (LinkedIn) | 2 | Yes |
| Quarter phrase not recognised ("the end of 2025") | q056 | 1 | Yes |
| Evidence chunk not retrieved although the quarter was searched | q051 (invented figure), q057 (false abstention), q060 (one quarter missing) | 3 | Yes |
| Figures placed under the wrong quarter | q061 | 1 | Yes (the judge missed it) |
| Wrong metric (a near-identical figure) | q046 (net income vs net income available to common stockholders) | 1 | Yes |
| Answer misses the key point although the evidence was retrieved | q078 | 1 | Yes |
| Secondary detail of the gold answer left out | q045, q052, q058 | 3 | Minor |
| Embellishment or over-generalisation not in the sources | q076, q077 | 2 | Minor |
| Measurement artefact: exact gold quote not retrieved, answer still correct | q047, q059, q069, q071 | 4 | No |
| Measurement artefact: correct arithmetic or trend marked unsupported | q062, q066 | 2 | No |
| **Total** | | **20** | 12 real, 5 minor, 6 artefacts (q058 counted once) |

The same items in the roadmap's four categories (an item can be in more than one):

| Category | Items | Count |
|---|---|---|
| Retrieval miss (quarter or evidence not retrieved, and it hurt the answer) | q051, q056, q057, q060, q067, q068 | 6 |
| Wrong period | q061 (shifted labels), q067 and q068 (figures from the wrong quarter or document) | 3 |
| Unsupported number | q051 ($365.8 billion invented), q061 ($9.89 billion more precise than its cited source), q062 (4 derived differences) | 3 |
| False abstention | C: q057. D adds q045 and q053 (LOW_SIMILARITY) and q068 | C 1, D 4 |

## Worked example 1: a retrieval miss turns into an invented number (q051)

- **Question:** "What was Microsoft's revenue for the full fiscal year 2026?" Gold: $331.8 billion, up 18% (16% in constant currency), from the FY2026 Q4 press release.
- **What happened:** the right quarter was searched (period coverage 1/1), but the chunk with "Revenue was $331.8 billion" was not among the 16 retrieved. The model answered "$365.8 billion, up 18%" and cited source [6], which contains neither number.
- **What caught it:** NumberGroundingCheck flagged "$365.8 billion" as not found in the cited sources, and the judge gave faithfulness 0.0 and correctness 1. In the API the answer would still be shown, with a grounding warning.
- **Lesson:** a figure that is not in the sources is the most damaging failure for a finance tool. The grounding warning exists but does not block the answer. Two dev-only fixes to evaluate: retrieve full-year summary sentences from Q4 press releases better (the Phase 3 note already says press-release figure sentences rank low), and consider abstaining when an answer's headline number is ungrounded.

## Worked example 2: a product name instead of the company (q068)

- **Question:** "How did LinkedIn revenue growth change across Q2 to Q4 of fiscal 2026?" Gold: 11% (Q2), 12% (Q3), 12% (Q4).
- **What happened:** the question names LinkedIn, not Microsoft. `companies.yaml` has no product aliases, so QueryParser found no company and could not build the three per-quarter searches (period coverage 1/3). The answer got Q2 and Q4 right but gave 11% for Q3, citing an Alphabet source. The judge marked that claim unsupported (faithfulness 0.5) and gave correctness 3. q067 ("Xbox content and services revenue") failed the same way: coverage 1/4 and correctness 2.
- **Lesson:** the parser's company detection is the gate for per-period retrieval, so a missing alias silently turns config C back into config B for that question. Adding product aliases (LinkedIn, Xbox, Azure, YouTube, Instagram, WhatsApp, and so on) was already parked under "Later"; these two items are the evidence for it.

## Worked example 3: shifted quarter labels the judge did not catch (q061)

- **Question:** "How did Meta's full-year 2026 total expense outlook change over the year?" Gold: $162-169 billion (Q4 2025), unchanged in Q1, $165-169 billion (Q2), $175-179 billion (Q3).
- **What happened:** all four gold quotes were retrieved, and every range in the answer is right. But the answer places them one quarter late: "unchanged" under the second quarter (it was Q1) and "$165-169 billion" under "the third quarter" (it was Q2), then a second "third quarter" for $175-179 billion. The judge gave faithfulness 1.0 and correctness 5.
- **Lesson:** this is the judge weakness found in E5.2 (shifted quarter labels on multi-quarter answers), now seen on test. Judge correctness for multi_quarter is therefore optimistic. The rubric v2 rule about period labels did not fix it fully.

## What this means for the ablation

1. **A and C give equally correct answers; the "A is best" row is an artefact.** The ablation table shows correctness A 4.51 and C 4.44, but A's average leaves out q057 (its judge reply failed validation twice), while C's includes q057 as a false abstention scored 1. On the same 49 items, both A and C score **4.51** (B 4.25, D 4.33). Per item, C beats A on 11 items and A beats C on 9.
2. **Retrieval improves as designed** (recall@k all quotes A 0.48, B 0.58, C 0.71), but better recall did not translate into more correct answers on this set. Four C items missed the exact gold quote yet answered correctly from another document with the same figure, so recall understates what the model can use. C's multi-quarter correctness (3.85) is below A's (4.17), driven by the product-alias and quarter-phrase failures above. On those questions C searched fewer quarters, not more.
3. **D's evidence threshold does not generalise.** min-similarity 0.531 was chosen on dev, where it blocked no answerable question. On test it blocks q045 and q053 (both single facts) with LOW_SIMILARITY, which raises the false-abstention rate from C's 2% to 9% and lowers correctness. A threshold tuned on 25 answerable dev items was too fragile. The chat API currently runs mode D, so this matters for users, not only for the report (see "Open decision").
4. **Unanswerable questions:** all four configs declined all 5. The difference between configs is only in false abstentions on answerable items.

## Measurement limits found

- **Recall counts only the exact gold quote.** The same figure often appears in a press release, a 10-Q and a transcript. 4 of C's 20 flagged items are correct answers that missed the specific quote.
- **The judge marks correct arithmetic as unsupported** (q062: four sequential differences; q066: "growth slowed each quarter"), although the rubric says arithmetic and simple trends from source figures count as supported. NumberGroundingCheck also flags derived numbers ($222 billion, $51.9 billion, ...), because it only matches numbers that appear in a source.
- **The judge is inconsistent on omitted details:** q045 (missing "550 million on TVs") got 3 under C, while the same omission scored 4 or 5 elsewhere. The rubric says a missing minor detail is a 4.
- **The judge is too generous on quarter labels** (q061 above), as found in E5.2.
- **Configs are averaged over different item sets when an item errors.** The ablation table should compare configs on the items every config completed (paired comparison). This changes only the report, not the system.
- **One judge failure** (A, q057): the reply twice marked a claim supported without naming a source. Validation stopped it as designed, rather than guessing a score.
- **Run-to-run variation:** dev q020 was a false abstention in two runs and answered correctly in a third, at temperature 0. Single-run differences of one or two items between configs are within this noise.

## Proposed fixes (each a new, labelled dev run; no re-tuning on test)

1. Product and segment aliases in `companies.yaml` (LinkedIn, Xbox, Azure, Bing, YouTube, Google Cloud, Instagram, WhatsApp, Reality Labs, and so on), with QueryParser tests.
2. Quarter phrases "end of {year}", "year-end {year}" and "{month} {year}" mapped to the right quarter, with PeriodResolver table cases.
3. Paired comparison in the ablation table (items every config completed), plus the current per-config numbers.
4. Judge rubric: an explicit example that sequential differences and "slowed or accelerated" trends computed from source figures are supported. Any change must be re-checked against the human labels (E5.2 process).
5. Revisit the D threshold with more answerable dev items, or drop D's threshold and rely on the model's INSUFFICIENT_EVIDENCE (C already declines all unanswerable items on dev and test).

## Decision taken after this analysis

The chat API's default mode was D. On test, D wrongly declined 4 of 45 answerable questions versus 1 for C, while both declined all 5 unanswerable ones. On 2026-11-04 the API default was switched to C (`earningslens.retrieval.mode: C`). This is a product decision taken after seeing test results, recorded in DECISIONS.md as a post-test change. It is not a re-tune of the reported numbers: the committed test reports and the ablation table stay exactly as they are.
