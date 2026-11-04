package com.earningslens.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every metric of spec 11.2 for one config, from per-item results (roadmap E5.3). Pure functions, no I/O.
 *
 * Choices (DECISIONS.md, E5.3):
 * - Items with an error (model unavailable, judge output invalid twice) are left out of every metric and listed.
 * - Correctness counts every item: an answer gets the judge's 1 to 5; an abstention gets 5 on an unanswerable
 *   item and 1 on an answerable one (rubric task 3), so a config cannot look better just by declining more.
 *   The judge-only average over answered answerable items is reported as well.
 * - Faithfulness is the mean of per-answer scores (supported / claims); "fully faithful" is the share at 1.0.
 * - Citation precision and number grounding are pooled over all pairs / numbers of all answers.
 * - Abstention precision = abstentions on unanswerable items / all abstentions; recall = unanswerable items
 *   abstained / unanswerable items; false-abstention rate = answerable items abstained / answerable items.
 * - Latency p50 / p95 use the nearest-rank method over retrieval + generation time (not the judge).
 */
final class FullEvalMetrics {

    private FullEvalMetrics() {
    }

    /**
     * One item under one config.
     *
     * @param retrieval         retrieval scores (gold quotes found, rank, periods)
     * @param judgeFaithfulness supported / claims, or null when not judged (abstained, error)
     * @param judgeCorrectness  1 to 5 from the judge, or null when not judged
     * @param citationPairs     sentence/marker pairs judged, or null when not judged
     * @param numbers           numbers found in the answer by NumberGroundingCheck, or null when abstained
     * @param latencyMs         retrieval + answer time
     * @param costUsd           embedding + answer-model cost of this query (not the judge)
     * @param judgeCostUsd      judge cost for this item (0 when not judged)
     * @param error             why the item was left out, or null
     */
    record ItemResult(String id, String category, boolean answerable, boolean abstained, String abstainReason,
                      RetrievalMetrics.ItemScore retrieval, Double judgeFaithfulness, Integer judgeCorrectness,
                      String correctnessReason, Integer supportingCitations, Integer citationPairs,
                      Integer groundedNumbers, Integer numbers, List<String> unsupportedClaims,
                      List<String> ungroundedNumbers, long latencyMs, double costUsd, double judgeCostUsd,
                      String answer, String error) {

        ItemResult {
            unsupportedClaims = unsupportedClaims == null ? List.of() : List.copyOf(unsupportedClaims);
            ungroundedNumbers = ungroundedNumbers == null ? List.of() : List.copyOf(ungroundedNumbers);
        }

        boolean ok() {
            return error == null;
        }

        boolean judged() {
            return judgeCorrectness != null;
        }

        /** The correctness this item counts with (rule above), or null if it cannot be scored. */
        Integer effectiveCorrectness() {
            if (!ok()) {
                return null;
            }
            if (abstained) {
                return answerable ? 1 : 5;
            }
            return judgeCorrectness;
        }
    }

    record Generation(int judged, Double faithfulness, Double fullyFaithful, Double citationPrecision,
                      int citationPairs, Double numberGroundingRate, int numbers, Double correctness,
                      Double correctnessAnsweredAnswerable, Map<String, Double> correctnessByCategory) {
    }

    record Abstention(int unanswerable, int unanswerableAbstained, int answerable, int answerableAbstained,
                      Double precision, Double recall, Double falseAbstentionRate, Map<String, Integer> byReason) {
    }

    record Operations(int queries, long latencyP50Ms, long latencyP95Ms, double meanCostUsd, double totalCostUsd,
                      double judgeCostUsd) {
    }

    record Summary(String config, int items, int errors, RetrievalMetrics.Summary retrieval,
                   Map<String, RetrievalMetrics.Summary> retrievalByCategory, Generation generation,
                   Abstention abstention, Operations operations) {
    }

    static Summary summarize(String config, List<ItemResult> all) {
        List<ItemResult> ok = all.stream().filter(ItemResult::ok).toList();
        List<RetrievalMetrics.ItemScore> scores = ok.stream().map(ItemResult::retrieval).toList();
        Map<String, RetrievalMetrics.Summary> byCategory = new LinkedHashMap<>();
        for (EvalCategory category : EvalCategory.values()) {
            if (category == EvalCategory.UNANSWERABLE) {
                continue;
            }
            List<RetrievalMetrics.ItemScore> of = scores.stream().filter(s -> s.category() == category).toList();
            if (!of.isEmpty()) {
                byCategory.put(category.jsonName(), RetrievalMetrics.summarize(of));
            }
        }
        return new Summary(config, all.size(), all.size() - ok.size(), RetrievalMetrics.summarize(scores), byCategory,
                generation(ok), abstention(ok), operations(ok));
    }

    static Generation generation(List<ItemResult> ok) {
        List<ItemResult> judged = ok.stream().filter(ItemResult::judged).toList();
        Double faithfulness = mean(judged.stream().map(ItemResult::judgeFaithfulness).toList());
        Double fullyFaithful = judged.isEmpty() ? null
                : judged.stream().filter(r -> r.judgeFaithfulness() != null && r.judgeFaithfulness() >= 1.0).count()
                / (double) judged.size();

        int pairs = judged.stream().mapToInt(r -> r.citationPairs() == null ? 0 : r.citationPairs()).sum();
        int supporting = judged.stream().mapToInt(r -> r.supportingCitations() == null ? 0 : r.supportingCitations()).sum();
        int numbers = ok.stream().mapToInt(r -> r.numbers() == null ? 0 : r.numbers()).sum();
        int grounded = ok.stream().mapToInt(r -> r.groundedNumbers() == null ? 0 : r.groundedNumbers()).sum();

        List<Integer> effective = new ArrayList<>();
        ok.forEach(r -> {
            if (r.effectiveCorrectness() != null) {
                effective.add(r.effectiveCorrectness());
            }
        });
        List<Integer> answeredAnswerable = judged.stream().filter(ItemResult::answerable)
                .map(ItemResult::judgeCorrectness).toList();
        Map<String, Double> byCategory = new LinkedHashMap<>();
        for (EvalCategory category : EvalCategory.values()) {
            List<Integer> of = ok.stream().filter(r -> r.category().equals(category.jsonName()))
                    .map(ItemResult::effectiveCorrectness).filter(java.util.Objects::nonNull).toList();
            if (!of.isEmpty()) {
                byCategory.put(category.jsonName(), meanInt(of));
            }
        }
        return new Generation(judged.size(), faithfulness, fullyFaithful,
                pairs == 0 ? null : supporting / (double) pairs, pairs,
                numbers == 0 ? null : grounded / (double) numbers, numbers,
                meanInt(effective), meanInt(answeredAnswerable), byCategory);
    }

    static Abstention abstention(List<ItemResult> ok) {
        int unanswerable = (int) ok.stream().filter(r -> !r.answerable()).count();
        int unanswerableAbstained = (int) ok.stream().filter(r -> !r.answerable() && r.abstained()).count();
        int answerable = ok.size() - unanswerable;
        int answerableAbstained = (int) ok.stream().filter(r -> r.answerable() && r.abstained()).count();
        int abstained = unanswerableAbstained + answerableAbstained;
        Map<String, Integer> byReason = new LinkedHashMap<>();
        ok.stream().filter(ItemResult::abstained)
                .forEach(r -> byReason.merge(String.valueOf(r.abstainReason()), 1, Integer::sum));
        return new Abstention(unanswerable, unanswerableAbstained, answerable, answerableAbstained,
                abstained == 0 ? null : unanswerableAbstained / (double) abstained,
                unanswerable == 0 ? null : unanswerableAbstained / (double) unanswerable,
                answerable == 0 ? null : answerableAbstained / (double) answerable, byReason);
    }

    static Operations operations(List<ItemResult> ok) {
        List<Long> latencies = ok.stream().map(ItemResult::latencyMs).sorted().toList();
        double total = ok.stream().mapToDouble(ItemResult::costUsd).sum();
        double judge = ok.stream().mapToDouble(ItemResult::judgeCostUsd).sum();
        return new Operations(ok.size(), percentile(latencies, 0.50), percentile(latencies, 0.95),
                ok.isEmpty() ? 0 : total / ok.size(), total, judge);
    }

    /** Nearest-rank percentile of a sorted list: the value at position ceil(p * n). 0 for an empty list. */
    static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(p * sorted.size());
        return sorted.get(Math.max(0, Math.min(sorted.size(), rank) - 1));
    }

    private static Double mean(List<Double> values) {
        List<Double> present = values.stream().filter(java.util.Objects::nonNull).toList();
        return present.isEmpty() ? null : present.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static Double meanInt(List<Integer> values) {
        return values.isEmpty() ? null : values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}
