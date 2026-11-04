# Ablation: test split, 2026-11-04

One row per config (spec 11.4). Source files: `2026-11-04-A-test.json`, `2026-11-04-B-test.json`, `2026-11-04-C-test.json`, `2026-11-04-D-test.json`.

| Config | Recall@k all | Recall@k any | MRR | Period coverage | Faithfulness | Fully faithful | Citation precision | Number grounding | Correctness | Abstention precision | Abstention recall | False abstention | Latency p50 / p95 ms | Cost per query |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A | 0.48 | 0.59 | 0.26 | 1.00 | 0.95 | 86% | 91% | 92% | 4.51 | 100% | 100% | 0% | 2045 / 5728 | $0.0009 |
| B | 0.58 | 0.67 | 0.31 | 1.00 | 0.91 | 79% | 85% | 89% | 4.20 | 71% | 100% | 4% | 2051 / 5629 | $0.0012 |
| C | 0.71 | 0.78 | 0.39 | 0.83 | 0.93 | 84% | 89% | 96% | 4.44 | 83% | 100% | 2% | 2475 / 5569 | $0.0012 |
| D | 0.71 | 0.78 | 0.39 | 0.83 | 0.94 | 88% | 85% | 97% | 4.26 | 56% | 100% | 9% | 1760 / 3119 | $0.0011 |

1 items were left out because of errors; see each config's report.
