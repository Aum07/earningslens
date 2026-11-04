package com.earningslens.eval;

import com.earningslens.eval.FullEvalMetrics.ItemResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Roadmap E5.3: every spec 11.2 metric on a small hand-checked set of items. No network, no database. */
class FullEvalMetricsTest {

    /** 6 items: 3 answered, 1 false abstention, 1 right abstention, 1 unanswerable answered, plus 1 error. */
    static List<ItemResult> sample() {
        return List.of(
                answered("q1", "single_fact", true, 1.0, 5, 2, 2, 3, 3, 1000),
                answered("q2", "multi_quarter", true, 0.5, 3, 1, 2, 1, 2, 3000),
                answered("q3", "qualitative", true, 1.0, 4, 1, 1, 0, 0, 2000),
                abstained("q4", "single_fact", true, "INSUFFICIENT_EVIDENCE", 1500),
                abstained("q5", "unanswerable", false, "PERIOD_NOT_IN_CORPUS", 10),
                answered("q6", "unanswerable", false, 1.0, 1, 1, 1, 1, 1, 2500),
                new ItemResult("q7", "single_fact", true, false, null, null, null, null, null, null, null, null, null,
                        null, null, 0, 0, 0, null, "LlmUnavailableException"));
    }

    @Test
    void errorsAreCountedAndLeftOut() {
        FullEvalMetrics.Summary s = FullEvalMetrics.summarize("C", sample());

        assertThat(s.items()).isEqualTo(7);
        assertThat(s.errors()).isEqualTo(1);
        assertThat(s.operations().queries()).isEqualTo(6);
    }

    @Test
    void generationMetrics() {
        FullEvalMetrics.Generation g = FullEvalMetrics.generation(ok());

        assertThat(g.judged()).isEqualTo(4);
        assertThat(g.faithfulness()).isCloseTo((1.0 + 0.5 + 1.0 + 1.0) / 4, within(1e-9));   // 0.875
        assertThat(g.fullyFaithful()).isEqualTo(0.75);
        assertThat(g.citationPrecision()).isCloseTo(5.0 / 6, within(1e-9));    // (2 + 1 + 1 + 1) / (2 + 2 + 1 + 1)
        assertThat(g.citationPairs()).isEqualTo(6);
        assertThat(g.numberGroundingRate()).isCloseTo(5.0 / 6, within(1e-9));  // (3 + 1 + 0 + 1) / (3 + 2 + 0 + 1)
        // Correctness over all items: 5, 3, 4, 1 (false abstention), 5 (right abstention), 1 (answered unanswerable)
        assertThat(g.correctness()).isCloseTo(19.0 / 6, within(1e-9));
        assertThat(g.correctnessAnsweredAnswerable()).isCloseTo(4.0, within(1e-9));   // (5 + 3 + 4) / 3
        assertThat(g.correctnessByCategory()).containsEntry("single_fact", 3.0)          // (5 + 1) / 2
                .containsEntry("unanswerable", 3.0);                                     // (5 + 1) / 2
    }

    @Test
    void abstentionMetrics() {
        FullEvalMetrics.Abstention a = FullEvalMetrics.abstention(ok());

        assertThat(a.unanswerable()).isEqualTo(2);
        assertThat(a.unanswerableAbstained()).isEqualTo(1);
        assertThat(a.answerable()).isEqualTo(4);
        assertThat(a.answerableAbstained()).isEqualTo(1);
        assertThat(a.precision()).isEqualTo(0.5);           // 1 right of 2 abstentions
        assertThat(a.recall()).isEqualTo(0.5);              // 1 of 2 unanswerable
        assertThat(a.falseAbstentionRate()).isEqualTo(0.25); // 1 of 4 answerable
        assertThat(a.byReason()).containsEntry("INSUFFICIENT_EVIDENCE", 1).containsEntry("PERIOD_NOT_IN_CORPUS", 1);
    }

    @Test
    void noAbstentionsGivesNullPrecision() {
        FullEvalMetrics.Abstention a = FullEvalMetrics.abstention(List.of(
                answered("q1", "single_fact", true, 1.0, 5, 1, 1, 1, 1, 100)));

        assertThat(a.precision()).isNull();
        assertThat(a.recall()).isNull();
        assertThat(a.falseAbstentionRate()).isEqualTo(0.0);
    }

    @Test
    void latencyPercentilesAndCost() {
        FullEvalMetrics.Operations o = FullEvalMetrics.operations(ok());

        // sorted: 10, 1000, 1500, 2000, 2500, 3000 -> p50 = 3rd value, p95 = 6th value (nearest rank)
        assertThat(o.latencyP50Ms()).isEqualTo(1500);
        assertThat(o.latencyP95Ms()).isEqualTo(3000);
        assertThat(o.totalCostUsd()).isCloseTo(6 * 0.001, within(1e-12));
        assertThat(o.meanCostUsd()).isCloseTo(0.001, within(1e-12));
        assertThat(o.judgeCostUsd()).isCloseTo(4 * 0.01, within(1e-12));
    }

    @Test
    void percentileEdgeCases() {
        assertThat(FullEvalMetrics.percentile(List.of(), 0.5)).isZero();
        assertThat(FullEvalMetrics.percentile(List.of(7L), 0.95)).isEqualTo(7);
        assertThat(FullEvalMetrics.percentile(List.of(1L, 2L, 3L, 4L), 0.5)).isEqualTo(2);
    }

    private static List<ItemResult> ok() {
        return sample().stream().filter(ItemResult::ok).toList();
    }

    static ItemResult answered(String id, String category, boolean answerable, double faithfulness, int correctness,
                               int supporting, int pairs, int grounded, int numbers, long latencyMs) {
        RetrievalMetrics.ItemScore score = new RetrievalMetrics.ItemScore(id, EvalCategory.fromJson(category), 16,
                answerable ? 1 : 0, answerable ? 1 : 0, answerable ? 1 : null, answerable ? 1 : 0, answerable ? 1 : 0,
                List.of());
        return new ItemResult(id, category, answerable, false, null, score, faithfulness, correctness, "reason",
                supporting, pairs, grounded, numbers, faithfulness < 1.0 ? List.of("claim (no source)") : List.of(),
                grounded < numbers ? List.of("number not in sources") : List.of(), latencyMs, 0.001, 0.01,
                "answer text", null);
    }

    static ItemResult abstained(String id, String category, boolean answerable, String reason, long latencyMs) {
        RetrievalMetrics.ItemScore score = new RetrievalMetrics.ItemScore(id, EvalCategory.fromJson(category), 0,
                answerable ? 1 : 0, 0, null, 0, 0, List.of());
        return new ItemResult(id, category, answerable, true, reason, score, null, null, null, null, null, null,
                null, null, null, latencyMs, 0.001, 0, "I can't answer this ...", null);
    }
}
