package com.earningslens.eval;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the LLM judge (spec 11.2, roadmap E5.1), read from "eval.judge.*".
 * Defaults are given here too, so the record binds in every profile (the judge is only used by the eval job).
 *
 * @param model                         judge model; gpt-4.1 was chosen on 2026-11-04 (DECISIONS.md)
 * @param temperature                   0, so the same input gives (nearly) the same scores
 * @param maxOutputTokens               room for the claim list, the citation list and the reasons
 * @param maxAttempts                   calls per answer: the first, plus retries when the output fails validation
 * @param rubricPath                    eval/judge-rubric.md; the part after "## Judge prompt" is the system message
 * @param inputPricePerMillionTokens    USD per 1M input tokens of the judge model (gpt-4.1: 2.00)
 * @param outputPricePerMillionTokens   USD per 1M output tokens of the judge model (gpt-4.1: 8.00)
 * @param sampleIds                     dev items for the E5.1 smoke run (--eval.task=judge-sample)
 */
@ConfigurationProperties(prefix = "eval.judge")
public record JudgeProperties(
        @DefaultValue("gpt-4.1") String model,
        @DefaultValue("0.0") double temperature,
        @DefaultValue("2000") int maxOutputTokens,
        @DefaultValue("2") int maxAttempts,
        @DefaultValue("../eval/judge-rubric.md") String rubricPath,
        @DefaultValue("2.00") double inputPricePerMillionTokens,
        @DefaultValue("8.00") double outputPricePerMillionTokens,
        @DefaultValue("q001,q015,q021") String sampleIds
) {
}
