package com.earningslens.eval;

import com.earningslens.retrieval.RetrievalMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roadmap E5.4: the test split runs once, with all configs, and that run writes latest.json. No network. */
class FinalTestRunTest {

    @TempDir
    Path dir;

    private static final List<RetrievalMode> ALL = List.of(RetrievalMode.A, RetrievalMode.B, RetrievalMode.C,
            RetrievalMode.D);

    @Test
    void allFourConfigsAreRequired() {
        assertThatThrownBy(() -> FullEvalRun.checkFinalTestRun(List.of(RetrievalMode.C), dir))
                .hasMessageContaining("A, B, C and D");
        assertThatThrownBy(() -> FullEvalRun.checkFinalTestRun(List.of(RetrievalMode.A, RetrievalMode.B,
                RetrievalMode.C), dir)).hasMessageContaining("A, B, C and D");
    }

    @Test
    void aSecondTestRunIsRefused() throws IOException {
        Files.writeString(dir.resolve("2026-11-04-C-dev.json"), "{}");   // dev reports do not count
        Files.writeString(dir.resolve("2026-11-02-retrieval-ABC-dev.json"), "{}");
        FullEvalRun.checkFinalTestRun(ALL, dir);   // allowed: no test report yet

        Files.writeString(dir.resolve("2026-11-05-B-test.json"), "{}");
        assertThatThrownBy(() -> FullEvalRun.checkFinalTestRun(ALL, dir))
                .hasMessageContaining("already been run").hasMessageContaining("2026-11-05-B-test.json");
    }

    @Test
    void anExistingLatestJsonAlsoCounts() throws IOException {
        Files.writeString(dir.resolve("latest.json"), "{}");

        assertThatThrownBy(() -> FullEvalRun.checkFinalTestRun(ALL, dir)).hasMessageContaining("latest.json");
    }

    @Test
    void latestJsonHoldsEveryConfigAndQuotesTheAgreementReport() throws IOException {
        Path agreement = dir.resolve("2026-11-04-judge-agreement-dev-v2.json");
        Files.writeString(agreement, """
                {"rubricPromptSha256": "%s", "agreement": {"n": 30, "faithfulness": {"kappa": 0.71}}}
                """.formatted(JudgeAgreementRun.sha256("prompt")));
        List<FullReportWriter.Report> reports = List.of(report("A"), report("B"), report("C"), report("D"));

        Path latest = new FullReportWriter().writeLatest(reports, dir, agreement);

        assertThat(latest.getFileName().toString()).isEqualTo("latest.json");
        Map<?, ?> json = JsonMapper.builder().build().readValue(latest.toFile(), Map.class);
        assertThat(json.get("split")).isEqualTo("test");
        assertThat(json.get("reports")).isEqualTo(List.of("2026-11-05-A-test.json", "2026-11-05-B-test.json",
                "2026-11-05-C-test.json", "2026-11-05-D-test.json"));
        assertThat(json.get("ablationReport")).isEqualTo("2026-11-05-ablation-test.md");
        assertThat((List<?>) json.get("configs")).hasSize(4);
        Map<?, ?> judge = (Map<?, ?>) json.get("judgeAgreement");
        assertThat(judge.get("report")).isEqualTo("2026-11-04-judge-agreement-dev-v2.json");
        assertThat(judge.get("sameRubric")).isEqualTo(true);
        assertThat(((Map<?, ?>) ((Map<?, ?>) judge.get("agreement")).get("faithfulness")).get("kappa")).isEqualTo(0.71);
    }

    @Test
    void missingAgreementReportGivesNull() throws IOException {
        Path latest = new FullReportWriter().writeLatest(List.of(report("C")), dir, dir.resolve("missing.json"));

        Map<?, ?> json = JsonMapper.builder().build().readValue(latest.toFile(), Map.class);
        assertThat(json.get("judgeAgreement")).isNull();
        assertThat(json.get("ablationReport")).isNull();
    }

    private static FullReportWriter.Report report(String config) {
        FullReportWriter.Settings settings = new FullReportWriter.Settings(16, 5, 4, 0.531, 600, 100, 3285,
                "gpt-4o-mini", "text-embedding-3-small", "gpt-4.1", JudgeAgreementRun.sha256("prompt"));
        List<FullEvalMetrics.ItemResult> items = FullEvalMetricsTest.sample();
        return new FullReportWriter.Report("2026-11-05", "test", config, "", settings,
                FullEvalMetrics.summarize(config, items), items);
    }
}
