package com.earningslens.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Roadmap E3.7: choose the evidence threshold (earningslens.retrieval.min-similarity) from dev data.
 *
 * Mode D abstains, without calling the LLM, when the best vector similarity of a question is below the
 * threshold. A good threshold lets answerable questions through and stops unanswerable ones. For every
 * candidate threshold this counts both, treating "abstain" as the prediction for "unanswerable":
 * - precision: of the questions that abstain, the share that really are unanswerable
 * - recall:    of the unanswerable questions, the share that abstain
 * - F1:        the balance of the two
 * Questions that already abstain for another reason (period not in the corpus) abstain at every threshold.
 *
 * Candidate thresholds lie halfway between neighbouring similarity values (rounded to 3 decimals, so the
 * chosen value can be copied into config exactly), plus 0 (never abstain on similarity).
 * A pure function: no database, no Spring.
 */
public final class ThresholdTuning {

    private ThresholdTuning() {
    }

    /** One dev question: is it answerable, its best vector similarity, and does it abstain anyway. */
    public record ItemSimilarity(String id, boolean answerable, Double bestSimilarity, boolean abstainsAnyway) {
    }

    public record Row(double threshold, int unanswerableAbstained, int unanswerable, int answerableBlocked,
                      int answerable, double precision, double recall, double f1, List<String> blockedAnswerable) {
    }

    public static List<Row> sweep(List<ItemSimilarity> items) {
        TreeSet<Double> values = new TreeSet<>();
        for (ItemSimilarity i : items) {
            if (!i.abstainsAnyway() && i.bestSimilarity() != null) {
                values.add(i.bestSimilarity());
            }
        }
        TreeSet<Double> candidates = new TreeSet<>();
        candidates.add(0.0);
        Double previous = null;
        for (double v : values) {
            if (previous != null) {
                candidates.add(round3((previous + v) / 2));
            }
            previous = v;
        }
        if (previous != null) {
            candidates.add(round3(Math.min(1.0, previous + 0.001)));
        }

        List<Row> rows = new ArrayList<>();
        for (double t : candidates) {
            rows.add(evaluate(items, t));
        }
        return rows;
    }

    /** The row with the highest F1; on a tie the lower threshold, which blocks fewer answerable questions. */
    public static Row best(List<Row> rows) {
        return rows.stream()
                .max(Comparator.comparingDouble(Row::f1).thenComparing(Comparator.comparingDouble(Row::threshold).reversed()))
                .orElse(null);
    }

    static Row evaluate(List<ItemSimilarity> items, double threshold) {
        int unanswerable = 0, unanswerableAbstained = 0, answerable = 0, answerableBlocked = 0;
        List<String> blocked = new ArrayList<>();
        for (ItemSimilarity i : items) {
            boolean abstains = abstains(i, threshold);
            if (i.answerable()) {
                answerable++;
                if (abstains) {
                    answerableBlocked++;
                    blocked.add(i.id());
                }
            } else {
                unanswerable++;
                if (abstains) {
                    unanswerableAbstained++;
                }
            }
        }
        int abstained = unanswerableAbstained + answerableBlocked;
        double precision = abstained == 0 ? 0 : unanswerableAbstained / (double) abstained;
        double recall = unanswerable == 0 ? 0 : unanswerableAbstained / (double) unanswerable;
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        return new Row(threshold, unanswerableAbstained, unanswerable, answerableBlocked, answerable,
                precision, recall, f1, blocked);
    }

    static boolean abstains(ItemSimilarity item, double threshold) {
        return item.abstainsAnyway() || item.bestSimilarity() == null || item.bestSimilarity() < threshold;
    }

    private static double round3(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
