package com.earningslens.eval;

import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Roadmap E3.6: retrieval metrics with hand-calculated values. No database. */
class RetrievalMetricsTest {

    private static final FiscalPeriod Q3 = new FiscalPeriod(2026, 3);
    private static final FiscalPeriod Q4 = new FiscalPeriod(2026, 4);
    private static final String QUOTE_A = "Microsoft Cloud revenue was 49.1 billion dollars";
    private static final String QUOTE_B = "Azure and other cloud services revenue grew 40 percent";

    @Test
    void quoteMatchesAfterWhitespaceNormalisation() {
        EvalItem item = single("q901", QUOTE_A);
        List<FusedHit> hits = List.of(
                hit(1, "MSFT", Q4, "Intro only."),
                hit(2, "MSFT", Q4, "Text. Microsoft Cloud\n revenue   was 49.1 billion dollars, up."));

        RetrievalMetrics.ItemScore score = RetrievalMetrics.score(item, hits);

        assertThat(score.goldFound()).isEqualTo(1);
        assertThat(score.firstHitRank()).isEqualTo(2);
        assertThat(score.reciprocalRank()).isEqualTo(0.5);
        assertThat(score.allFound()).isTrue();
        assertThat(score.missedQuotes()).isEmpty();
    }

    @Test
    void firstHitIsTheBestRankOverAllGoldQuotes() {
        EvalItem item = multi("q902", QUOTE_A, Q3, QUOTE_B, Q4);
        List<FusedHit> hits = List.of(
                hit(1, "MSFT", Q4, "nothing"),
                hit(2, "MSFT", Q4, "x " + QUOTE_B + " y"),
                hit(3, "MSFT", Q3, "nothing either"));

        RetrievalMetrics.ItemScore score = RetrievalMetrics.score(item, hits);

        assertThat(score.goldFound()).isEqualTo(1);
        assertThat(score.allFound()).isFalse();
        assertThat(score.anyFound()).isTrue();
        assertThat(score.firstHitRank()).isEqualTo(2);
        assertThat(score.missedQuotes()).containsExactly(QUOTE_A);
        // Both required periods (MSFT FY2026 Q3 and Q4) have a retrieved chunk, even though one quote is missed.
        assertThat(score.requiredPeriods()).isEqualTo(2);
        assertThat(score.coveredPeriods()).isEqualTo(2);
    }

    @Test
    void periodCoverageNeedsTheSameCompany() {
        EvalItem item = multi("q903", QUOTE_A, Q3, QUOTE_B, Q4);
        List<FusedHit> hits = List.of(hit(1, "GOOGL", Q3, "other company"), hit(2, "MSFT", Q4, "same company"));

        assertThat(RetrievalMetrics.score(item, hits).coveredPeriods()).isEqualTo(1);
    }

    @Test
    void summaryAveragesOverAnswerableItemsOnly() {
        // a: found at rank 1 (RR 1). b: found at rank 4 (RR 0.25). c: not found (RR 0). d: unanswerable, ignored.
        RetrievalMetrics.ItemScore a = new RetrievalMetrics.ItemScore("q1", EvalCategory.SINGLE_FACT, 8, 1, 1, 1, 1, 1, List.of());
        RetrievalMetrics.ItemScore b = new RetrievalMetrics.ItemScore("q2", EvalCategory.MULTI_QUARTER, 9, 2, 1, 4, 3, 2, List.of("x"));
        RetrievalMetrics.ItemScore c = new RetrievalMetrics.ItemScore("q3", EvalCategory.MULTI_QUARTER, 7, 1, 0, null, 2, 1, List.of("y"));
        RetrievalMetrics.ItemScore d = new RetrievalMetrics.ItemScore("q4", EvalCategory.UNANSWERABLE, 8, 0, 0, null, 0, 0, List.of());

        RetrievalMetrics.Summary s = RetrievalMetrics.summarize(List.of(a, b, c, d));

        assertThat(s.answerableItems()).isEqualTo(3);
        assertThat(s.recallAll()).isCloseTo(1.0 / 3, within(1e-12));        // only a
        assertThat(s.recallAny()).isCloseTo(2.0 / 3, within(1e-12));        // a and b
        assertThat(s.mrr()).isCloseTo((1 + 0.25 + 0) / 3, within(1e-12));
        assertThat(s.requiredPeriods()).isEqualTo(5);                        // multi_quarter only: 3 + 2
        assertThat(s.coveredPeriods()).isEqualTo(3);
        assertThat(s.periodCoverage()).isCloseTo(0.6, within(1e-12));
        assertThat(s.avgRetrieved()).isCloseTo(8.0, within(1e-12));          // (8 + 9 + 7) / 3
    }

    @Test
    void noMultiQuarterItemsMeansNoPeriodCoverage() {
        RetrievalMetrics.ItemScore a = new RetrievalMetrics.ItemScore("q1", EvalCategory.SINGLE_FACT, 8, 1, 1, 1, 1, 1, List.of());

        assertThat(RetrievalMetrics.summarize(List.of(a)).periodCoverage()).isNull();
        assertThat(RetrievalMetrics.summarize(List.of()).answerableItems()).isZero();
    }

    @Test
    void unanswerableItemScoresNothing() {
        EvalItem item = new EvalItem("q904", EvalSplit.DEV, EvalCategory.UNANSWERABLE, "What was Amazon revenue?",
                null, false, "Not in the corpus.", List.of());

        RetrievalMetrics.ItemScore score = RetrievalMetrics.score(item, List.of(hit(1, "MSFT", Q4, "anything")));

        assertThat(score.goldQuotes()).isZero();
        assertThat(score.allFound()).isFalse();
        assertThat(score.firstHitRank()).isNull();
    }

    // ---- helpers ----

    private static EvalItem single(String id, String quote) {
        return new EvalItem(id, EvalSplit.DEV, EvalCategory.SINGLE_FACT, "A question?", null, true, "An answer.",
                List.of(new GoldEvidence("MSFT", Q4, DocType.PRESS_RELEASE, quote)));
    }

    private static EvalItem multi(String id, String quote1, FiscalPeriod p1, String quote2, FiscalPeriod p2) {
        return new EvalItem(id, EvalSplit.DEV, EvalCategory.MULTI_QUARTER, "A question?", null, true, "An answer.",
                List.of(new GoldEvidence("MSFT", p1, DocType.PRESS_RELEASE, quote1),
                        new GoldEvidence("MSFT", p2, DocType.PRESS_RELEASE, quote2)));
    }

    private static FusedHit hit(int rank, String ticker, FiscalPeriod period, String content) {
        RetrievedChunk chunk = new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), 0, ticker, period.year(),
                period.quarter(), DocType.PRESS_RELEASE, content, 10, null, null, null, null, false);
        return new FusedHit(chunk, 0.01, rank, rank, null, 0.5);
    }
}
