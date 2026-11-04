package com.earningslens.eval;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the eval job (spec 11.5), read from "eval.*", usually given on the command line:
 * --eval.config=A,B,C --eval.split=dev --eval.confirm=true
 *
 * @param task      what to run: "retrieval" (E3.6), "judge-sample" (E5.1), "label-prep", "judge-agreement" (E5.2) or "full" (E5.3); all dev only
 * @param config    comma-separated ablation configs (A, B, C, D)
 * @param split     dev or test
 * @param confirm   must be true before any paid API call; without it the job only prints the cost estimate
 * @param label     optional suffix for the report file name, for example "k10" while tuning
 * @param sweep     optional list of k x k-per-period settings to compare in one run, for example "8x3,12x4";
 *                  one report per setting, named with the setting
 * @param embeddingPricePerMillionTokens USD per 1M tokens for the embedding model (text-embedding-3-small: 0.02)
 * @param chatInputPricePerMillionTokens  USD per 1M input tokens for the answer model (gpt-4o-mini: 0.15)
 * @param chatOutputPricePerMillionTokens USD per 1M output tokens for the answer model (gpt-4o-mini: 0.60)
 * @param humanLabelsPath   E5.2: the person's labels, one JSON line per saved answer
 * @param labelAnswersPath  E5.2: answers saved by label-prep, judged by judge-agreement
 * @param labelReviewPath   E5.2: Markdown sheet to label from
 * @param labelCount        E5.2: how many answers to label (spec 11.3: 30)
 * @param finalTestRun      E5.4: must be true (with task full and all four configs) to run the test split, once
 * @param judgeAgreementReport E5.4: the judge-agreement report whose numbers latest.json quotes
 * Judge settings (model, prices, rubric) are in {@link JudgeProperties} ("eval.judge.*").
 */
@ConfigurationProperties(prefix = "eval")
public record EvalProperties(
        @DefaultValue("retrieval") String task,
        @DefaultValue("A,B,C") String config,
        @DefaultValue("dev") String split,
        @DefaultValue("false") boolean confirm,
        @DefaultValue("") String label,
        @DefaultValue("") String sweep,
        @DefaultValue("../eval/dataset.jsonl") String datasetPath,
        @DefaultValue("../eval/reports") String reportsDir,
        @DefaultValue("0.02") double embeddingPricePerMillionTokens,
        @DefaultValue("0.15") double chatInputPricePerMillionTokens,
        @DefaultValue("0.60") double chatOutputPricePerMillionTokens,
        @DefaultValue("../eval/human-labels.jsonl") String humanLabelsPath,
        @DefaultValue("../eval/human-labels-answers.json") String labelAnswersPath,
        @DefaultValue("../eval/human-labels-review.md") String labelReviewPath,
        @DefaultValue("30") int labelCount,
        @DefaultValue("false") boolean finalTestRun,
        @DefaultValue("../eval/reports/2026-11-04-judge-agreement-dev-v2.json") String judgeAgreementReport
) {
}
