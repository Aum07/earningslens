package com.earningslens.eval;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the eval job (spec 11.5), read from "eval.*", usually given on the command line:
 * --eval.config=A,B,C --eval.split=dev --eval.confirm=true
 *
 * @param task      what to run; only "retrieval" exists so far (E3.6), generation comes in Phase 5
 * @param config    comma-separated ablation configs (A, B, C, D)
 * @param split     dev or test
 * @param confirm   must be true before any paid API call; without it the job only prints the cost estimate
 * @param label     optional suffix for the report file name, for example "k10" while tuning
 * @param sweep     optional list of k x k-per-period settings to compare in one run, for example "8x3,12x4";
 *                  one report per setting, named with the setting
 * @param embeddingPricePerMillionTokens USD per 1M tokens for the embedding model (text-embedding-3-small: 0.02)
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
        @DefaultValue("0.02") double embeddingPricePerMillionTokens
) {
}
