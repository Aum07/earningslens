package com.earningslens.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap E5.3: the report files exist, are named per spec 11.5 and show every spec 11.2 metric. */
class FullReportWriterTest {

    @TempDir
    Path dir;

    private static FullReportWriter.Report report(String config) {
        FullReportWriter.Settings settings = new FullReportWriter.Settings(16, 5, 4, 0.531, 600, 100, 3285,
                "gpt-4o-mini", "text-embedding-3-small", "gpt-4.1", JudgeAgreementRun.sha256("prompt"));
        List<FullEvalMetrics.ItemResult> items = FullEvalMetricsTest.sample();
        return new FullReportWriter.Report("2026-11-04", "dev", config, "", settings,
                FullEvalMetrics.summarize(config, items), items);
    }

    @Test
    void writesJsonAndMarkdownNamedBySpec() throws IOException {
        List<Path> written = new FullReportWriter().write(report("C"), dir);

        assertThat(written).extracting(p -> p.getFileName().toString())
                .containsExactly("2026-11-04-C-dev.json", "2026-11-04-C-dev.md");
        assertThat(Files.readString(written.get(0))).contains("\"config\" : \"C\"", "\"falseAbstentionRate\"",
                "\"latencyP95Ms\"", "\"numberGroundingRate\"");
        assertThat(FullReportWriter.baseName("2026-11-04", "C", "dev", "after fix"))
                .isEqualTo("2026-11-04-C-dev-after_fix");
    }

    @Test
    void markdownShowsEverySpecMetric() {
        String md = FullReportWriter.markdown(report("C"));

        assertThat(md).contains("Recall@k all quotes", "MRR", "Period coverage",
                "Faithfulness (mean supported / claims) | 0.88",
                "Citation precision (6 sentence/citation pairs) | 83%",
                "Number grounding rate (6 numbers) | 83%",
                "Correctness, all items", "| 3.17 |",
                "Unanswerable items abstained: 1 of 2 (recall 50%)",
                "false-abstention rate 25%",
                "Precision (abstentions that were right): 50%",
                "p50 1500 ms, p95 3000 ms",
                "Cost per query (embedding + answer model): $0.0010",
                "1 left out because of errors",
                "**q7** error: LlmUnavailableException",
                "**q2** unsupported claim");
        assertThat(md).doesNotContain("—");
    }

    @Test
    void ablationHasOneRowPerConfigAndNamesItsSources() throws IOException {
        Path md = new FullReportWriter().writeAblation(List.of(report("A"), report("C")), dir);

        String text = Files.readString(md);
        assertThat(md.getFileName().toString()).isEqualTo("2026-11-04-ablation-dev.md");
        assertThat(text).contains("`2026-11-04-A-dev.json`", "`2026-11-04-C-dev.json`")
                .contains("| A | ").contains("| C | ")
                .contains("2 items were left out because of errors");
    }
}
