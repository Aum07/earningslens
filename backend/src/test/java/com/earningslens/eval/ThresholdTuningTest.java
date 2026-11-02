package com.earningslens.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Roadmap E3.7: threshold sweep with hand-checked counts. No database. */
class ThresholdTuningTest {

    // Answerable: 0.80, 0.70, 0.55. Unanswerable: 0.50, 0.60, and one whose period is not in the corpus.
    private static final List<ThresholdTuning.ItemSimilarity> ITEMS = List.of(
            new ThresholdTuning.ItemSimilarity("a1", true, 0.80, false),
            new ThresholdTuning.ItemSimilarity("a2", true, 0.70, false),
            new ThresholdTuning.ItemSimilarity("a3", true, 0.55, false),
            new ThresholdTuning.ItemSimilarity("u1", false, 0.50, false),
            new ThresholdTuning.ItemSimilarity("u2", false, 0.60, false),
            new ThresholdTuning.ItemSimilarity("u3", false, null, true));

    @Test
    void candidatesAreZeroMidpointsAndJustAboveTheTop() {
        assertThat(ThresholdTuning.sweep(ITEMS)).extracting(ThresholdTuning.Row::threshold)
                .containsExactly(0.0, 0.525, 0.575, 0.65, 0.75, 0.801);
    }

    @Test
    void countsAndScoresAtOneThreshold() {
        // At 0.575: u3 (anyway), u1 (0.50) and a3 (0.55) abstain: 2 of 3 unanswerable caught, 1 answerable blocked.
        ThresholdTuning.Row row = ThresholdTuning.evaluate(ITEMS, 0.575);

        assertThat(row.unanswerableAbstained()).isEqualTo(2);
        assertThat(row.unanswerable()).isEqualTo(3);
        assertThat(row.answerableBlocked()).isEqualTo(1);
        assertThat(row.blockedAnswerable()).containsExactly("a3");
        assertThat(row.precision()).isCloseTo(2.0 / 3, within(1e-12));
        assertThat(row.recall()).isCloseTo(2.0 / 3, within(1e-12));
        assertThat(row.f1()).isCloseTo(2.0 / 3, within(1e-12));
    }

    @Test
    void zeroThresholdOnlyCatchesQuestionsThatAbstainAnyway() {
        ThresholdTuning.Row row = ThresholdTuning.evaluate(ITEMS, 0.0);

        assertThat(row.unanswerableAbstained()).isEqualTo(1);   // u3
        assertThat(row.answerableBlocked()).isZero();
        assertThat(row.precision()).isEqualTo(1.0);
        assertThat(row.recall()).isCloseTo(1.0 / 3, within(1e-12));
    }

    @Test
    void bestIsHighestF1WithTiesGoingToTheLowerThreshold() {
        // 0.525: u3, u1 caught, none blocked: P 1, R 2/3, F1 0.8.
        // 0.65:  u3, u1, u2 caught, a3 blocked: P 3/4, R 1, F1 6/7 = 0.857. Highest.
        ThresholdTuning.Row best = ThresholdTuning.best(ThresholdTuning.sweep(ITEMS));

        assertThat(best.threshold()).isEqualTo(0.65);
        assertThat(best.f1()).isCloseTo(6.0 / 7, within(1e-12));

        List<ThresholdTuning.Row> tied = List.of(
                ThresholdTuning.evaluate(ITEMS, 0.65), ThresholdTuning.evaluate(ITEMS, 0.66));
        assertThat(ThresholdTuning.best(tied).threshold()).isEqualTo(0.65);
    }

    @Test
    void noAbstentionsGivesZeroPrecisionNotAnError() {
        List<ThresholdTuning.ItemSimilarity> answerableOnly = List.of(
                new ThresholdTuning.ItemSimilarity("a1", true, 0.8, false));

        ThresholdTuning.Row row = ThresholdTuning.evaluate(answerableOnly, 0.0);
        assertThat(row.precision()).isZero();
        assertThat(row.f1()).isZero();
    }
}
