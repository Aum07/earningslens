package com.earningslens.eval;

import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.SearchFilters;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roadmap E3.6: the eval runner's guard rails and small helpers. No database, no Spring. */
class EvalRunnerRulesTest {

    @Test
    void retrievalRunsAreRefusedOnTheTestSplit() {
        assertThatThrownBy(() -> EvalRunner.checkAllowed("retrieval", EvalSplit.TEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test split");
        EvalRunner.checkAllowed("retrieval", EvalSplit.DEV);   // allowed
    }

    @Test
    void testSplitOpensOnlyForTheFinalFullRun() {
        EvalRunner.checkAllowed("full", EvalSplit.TEST, true);   // the one final run (E5.4)
        assertThatThrownBy(() -> EvalRunner.checkAllowed("full", EvalSplit.TEST, false))
                .hasMessageContaining("--eval.final-test-run=true");
        for (String task : List.of("retrieval", "judge-sample", "label-prep", "judge-agreement")) {
            assertThatThrownBy(() -> EvalRunner.checkAllowed(task, EvalSplit.TEST, true)).as(task)
                    .hasMessageContaining("test split");
        }
    }

    @Test
    void unknownTasksAreRefused() {
        assertThatThrownBy(() -> EvalRunner.checkAllowed("bogus", EvalSplit.DEV))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void judgeAndLabelTasksRunOnDevOnly() {
        for (String task : List.of("judge-sample", "label-prep", "judge-agreement", "full")) {
            EvalRunner.checkAllowed(task, EvalSplit.DEV);   // allowed
            assertThatThrownBy(() -> EvalRunner.checkAllowed(task, EvalSplit.TEST))
                    .as(task)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("test split");
        }
    }

    @Test
    void judgeSampleIdsArePickedInOrderAndMustExist() {
        List<EvalItem> dev = List.of(item("q001"), item("q015"), item("q021"));

        assertThat(JudgeSampleRun.pick(dev, " q021, q001 ,q021")).extracting(EvalItem::id).containsExactly("q021", "q001");
        assertThatThrownBy(() -> JudgeSampleRun.pick(dev, "q001,q040"))   // q040 is a test item
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("q040");
        assertThatThrownBy(() -> JudgeSampleRun.pick(dev, " , "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("empty");
    }

    private static EvalItem item(String id) {
        return new EvalItem(id, EvalSplit.DEV, EvalCategory.UNANSWERABLE, "Question " + id + "?", null, false,
                "Not in the documents.", List.of());
    }

    @Test
    void configsAreParsedInOrderWithoutDuplicates() {
        assertThat(EvalRunner.parseModes("A,B,C")).containsExactly(RetrievalMode.A, RetrievalMode.B, RetrievalMode.C);
        assertThat(EvalRunner.parseModes(" c , a,c")).containsExactly(RetrievalMode.C, RetrievalMode.A);
        assertThatThrownBy(() -> EvalRunner.parseModes("A,E")).hasMessageContaining("'E'");
        assertThatThrownBy(() -> EvalRunner.parseModes(" , ")).hasMessageContaining("empty");
    }

    @Test
    void reportNameHasDateConfigsSplitAndOptionalLabel() {
        LocalDate day = LocalDate.of(2026, 11, 2);
        List<RetrievalMode> abc = List.of(RetrievalMode.A, RetrievalMode.B, RetrievalMode.C);

        assertThat(EvalRunner.reportName(day, abc, EvalSplit.DEV, "")).isEqualTo("2026-11-02-retrieval-ABC-dev");
        assertThat(EvalRunner.reportName(day, abc, EvalSplit.DEV, "k10")).isEqualTo("2026-11-02-retrieval-ABC-dev-k10");
        assertThat(EvalRunner.reportName(day, List.of(RetrievalMode.C), EvalSplit.DEV, "chunk 400/80"))
                .isEqualTo("2026-11-02-retrieval-C-dev-chunk_400_80");
    }

    @Test
    void itemFiltersBecomeSearchFilters() {
        assertThat(EvalRunner.toFilters(null)).isEqualTo(SearchFilters.none());

        EvalFilters filters = new EvalFilters(List.of("MSFT"), List.of(new FiscalPeriod(2026, 4)), List.of(DocType.TRANSCRIPT));
        assertThat(EvalRunner.toFilters(filters))
                .isEqualTo(SearchFilters.tickers("MSFT").withPeriods(new FiscalPeriod(2026, 4)).withDocTypes(DocType.TRANSCRIPT));
    }

    @Test
    void markdownHasTheOverallTableMissesAndUnanswerableRows() {
        RetrievalMetrics.ItemScore hit = new RetrievalMetrics.ItemScore("q001", EvalCategory.SINGLE_FACT, 8, 1, 1, 2, 1, 1, List.of());
        RetrievalMetrics.ItemScore miss = new RetrievalMetrics.ItemScore("q002", EvalCategory.SINGLE_FACT, 8, 1, 0, null, 1, 0,
                List.of("a quote | with a pipe"));
        RetrievalMetrics.ItemScore none = new RetrievalMetrics.ItemScore("q026", EvalCategory.UNANSWERABLE, 0, 0, 0, null, 0, 0, List.of());
        List<RetrievalEvalReport.ItemRow> rows = List.of(
                row("q001", "single_fact", hit, null, 0.61),
                row("q002", "single_fact", miss, null, 0.55),
                row("q026", "unanswerable", none, "PERIOD_NOT_IN_CORPUS", null));
        RetrievalEvalReport.ConfigRun run = new RetrievalEvalReport.ConfigRun("C",
                RetrievalMetrics.summarize(List.of(hit, miss, none)),
                Map.of("single_fact", RetrievalMetrics.summarize(List.of(hit, miss))), 12.0, rows);
        RetrievalEvalReport report = new RetrievalEvalReport("2026-11-02", "dev", 3,
                new RetrievalEvalReport.Settings(8, 3, 4, 30, 30, 60, 0.0, 600, 100, 900, 3285, "text-embedding-3-small"),
                List.of(run),
                List.of(new RetrievalEvalReport.GoldRank("q002", "MSFT FY2026 Q4 PRESS_RELEASE", "a quote", 12, null, 280, null, 9, 7)),
                EvalRunner.threshold(List.of(run)));

        String md = RetrievalReportWriter.markdown(report);

        assertThat(md).contains("| C | 2 | 0.500 | 0.500 | 0.250 | - | 8.0 | 12 |");
        assertThat(md).contains("| q002 | single_fact | 0/1 | - |");
        assertThat(md).contains("a quote \\| with a pipe");
        assertThat(md).contains("| q026 | - (PERIOD_NOT_IN_CORPUS) |");
        assertThat(md).doesNotContain("| q001 | single_fact | 1/1");
        assertThat(md).contains("| q002 | MSFT FY2026 Q4 PRESS_RELEASE | 12 | - | - | 9 | 7 | 280 | a quote |");
        assertThat(md).contains("gold chunk in top 8: 0 of 1 before dedupe, 1 after dedupe.");
        assertThat(md).contains("1 quotes: 0 at rank 1-3, 0 at 4-8, 1 at 9-30, 0 below 30 or not found");
        // Threshold section from config C: q001 0.61, q002 0.55 answerable; q026 abstains anyway.
        assertThat(md).contains("## Evidence threshold (E3.7), from config C");
        assertThat(md).contains("| 0.000 | 1/1 | 0/2 | 1.000 | 1.000 | 1.000 |");
        assertThat(md).contains("Suggested (highest F1, ties to the lower threshold): **0.000**");
        // q026 (unanswerable) abstained; q001 and q002 (answerable) did not.
        assertThat(EvalRunner.abstentions(run)).containsExactly(1, 1, 0, 2);
    }

    @Test
    void firstRankFindsTheQuoteAfterNormalisation() {
        assertThat(EvalRunner.firstRank(List.of("no", "a  quote\nhere", "a quote"), "a quote here")).isEqualTo(2);
        assertThat(EvalRunner.firstRank(List.of("no"), "a quote")).isNull();
    }

    @Test
    void sweepIsParsedAsKTimesKPerPeriod() {
        List<int[]> sweep = EvalRunner.parseSweep(" 8x3, 12X4 ,16x5");
        assertThat(sweep).hasSize(3);
        assertThat(sweep.get(1)).containsExactly(12, 4);
        assertThat(EvalRunner.parseSweep("")).isEmpty();
        assertThat(EvalRunner.parseSweep(null)).isEmpty();
        assertThatThrownBy(() -> EvalRunner.parseSweep("12")).hasMessageContaining("'12'");
        assertThatThrownBy(() -> EvalRunner.parseSweep("12x0")).hasMessageContaining("'12x0'");
        assertThatThrownBy(() -> EvalRunner.parseSweep("12x4x5")).hasMessageContaining("'12x4x5'");
    }

    @Test
    void withKChangesOnlyKAndKPerPeriod() {
        var base = new com.earningslens.config.RetrievalProperties(8, 3, 4, 30, 30, 60, 0.4, RetrievalMode.D);
        var changed = EvalRunner.withK(base, 12, 4);
        assertThat(changed).isEqualTo(new com.earningslens.config.RetrievalProperties(12, 4, 4, 30, 30, 60, 0.4, RetrievalMode.D));
    }

    @Test
    void rankBuckets() {
        assertThat(RetrievalReportWriter.bucketLine(java.util.Arrays.asList(1, 3, 4, 8, 9, 30, 31, null)))
                .isEqualTo("8 quotes: 2 at rank 1-3, 2 at 4-8, 2 at 9-30, 2 below 30 or not found");
    }

    private static RetrievalEvalReport.ItemRow row(String id, String category, RetrievalMetrics.ItemScore score,
                                                   String abstain, Double best) {
        return new RetrievalEvalReport.ItemRow(id, category, "question", "SINGLE", abstain, best,
                List.of("MSFT FY2026 Q4"), List.of(), score, List.of());
    }
}
