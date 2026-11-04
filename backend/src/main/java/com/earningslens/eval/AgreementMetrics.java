package com.earningslens.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * Agreement between the person's labels and the judge (spec 11.3, roadmap E5.2). Pure maths, no I/O.
 *
 * Faithfulness is compared as yes/no: the judge says "faithful" when ALL its claims are supported
 * (faithfulness = 1.0), the same question the person answers. Reported as percentage agreement and
 * Cohen's kappa, which corrects for the agreement two raters would reach by chance:
 *   kappa = (observed - expected) / (1 - expected),
 *   expected = P(person yes) * P(judge yes) + P(person no) * P(judge no).
 * 1 = perfect, 0 = no better than chance. Undefined (null) when both raters gave the same single label to
 * every item (expected = 1).
 *
 * Correctness (1 to 5) is reported as exact agreement, agreement within one point, the mean difference
 * (judge minus person: positive = the judge is more generous) and a 5 x 5 table.
 */
final class AgreementMetrics {

    private AgreementMetrics() {
    }

    /** One answer scored by both. */
    record Pair(boolean humanFaithful, boolean judgeFaithful, int humanCorrectness, int judgeCorrectness) {
    }

    /**
     * @param bothYes          person and judge both faithful
     * @param bothNo           both not faithful
     * @param humanYesJudgeNo  the person said faithful, the judge did not (judge stricter)
     * @param humanNoJudgeYes  the judge said faithful, the person did not (judge too lenient)
     */
    record Faithfulness(int bothYes, int bothNo, int humanYesJudgeNo, int humanNoJudgeYes, double agreement,
                        Double kappa) {
    }

    /** matrix.get(h - 1).get(j - 1) = answers the person scored h and the judge scored j. */
    record Correctness(double exact, double withinOne, double meanJudgeMinusHuman, List<List<Integer>> matrix) {
    }

    record Summary(int n, Faithfulness faithfulness, Correctness correctness) {
    }

    static Summary summarize(List<Pair> pairs) {
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("no labelled answers to compare");
        }
        return new Summary(pairs.size(), faithfulness(pairs), correctness(pairs));
    }

    static Faithfulness faithfulness(List<Pair> pairs) {
        int bothYes = 0;
        int bothNo = 0;
        int humanYesJudgeNo = 0;
        int humanNoJudgeYes = 0;
        for (Pair p : pairs) {
            if (p.humanFaithful() && p.judgeFaithful()) {
                bothYes++;
            } else if (!p.humanFaithful() && !p.judgeFaithful()) {
                bothNo++;
            } else if (p.humanFaithful()) {
                humanYesJudgeNo++;
            } else {
                humanNoJudgeYes++;
            }
        }
        double n = pairs.size();
        double observed = (bothYes + bothNo) / n;
        double humanYes = (bothYes + humanYesJudgeNo) / n;
        double judgeYes = (bothYes + humanNoJudgeYes) / n;
        double expected = humanYes * judgeYes + (1 - humanYes) * (1 - judgeYes);
        Double kappa = expected >= 1.0 - 1e-12 ? null : (observed - expected) / (1 - expected);
        return new Faithfulness(bothYes, bothNo, humanYesJudgeNo, humanNoJudgeYes, observed, kappa);
    }

    static Correctness correctness(List<Pair> pairs) {
        int[][] counts = new int[5][5];
        int exact = 0;
        int withinOne = 0;
        int difference = 0;
        for (Pair p : pairs) {
            int h = check(p.humanCorrectness());
            int j = check(p.judgeCorrectness());
            counts[h - 1][j - 1]++;
            if (h == j) {
                exact++;
            }
            if (Math.abs(h - j) <= 1) {
                withinOne++;
            }
            difference += j - h;
        }
        List<List<Integer>> matrix = new ArrayList<>();
        for (int[] row : counts) {
            matrix.add(List.of(row[0], row[1], row[2], row[3], row[4]));
        }
        double n = pairs.size();
        return new Correctness(exact / n, withinOne / n, difference / n, List.copyOf(matrix));
    }

    private static int check(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("correctness must be 1 to 5, got " + score);
        }
        return score;
    }
}
