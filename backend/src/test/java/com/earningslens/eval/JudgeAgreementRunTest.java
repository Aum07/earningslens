package com.earningslens.eval;

import com.earningslens.eval.JudgeVerdict.Claim;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roadmap E5.2 step 2: labels are matched to saved answers, compared with a fake judge, and reported. No network. */
class JudgeAgreementRunTest {

    private final List<LabelSample> samples = List.of(
            LabelPrepRunTest.sample("q001", "C", "A [2]."),
            LabelPrepRunTest.sample("q002", "C", "B [2]."),
            LabelPrepRunTest.sample("q002", "A", "C [2]."));

    private final List<HumanLabel> labels = List.of(
            new HumanLabel("q001-C", "q001", "C", true, 5, ""),
            new HumanLabel("q002-C", "q002", "C", true, 4, ""),
            new HumanLabel("q002-A", "q002", "A", false, 1, "invents a figure"));

    @Test
    void labelsMustMatchTheSavedAnswersExactly() {
        assertThat(JudgeAgreementRun.match(samples, labels)).hasSize(3);

        assertThatThrownBy(() -> JudgeAgreementRun.match(samples, labels.subList(0, 2)))
                .hasMessageContaining("no human label for q002-A");
        List<HumanLabel> wrongConfig = List.of(labels.get(0), labels.get(1),
                new HumanLabel("q002-A", "q002", "C", false, 1, ""));
        assertThatThrownBy(() -> JudgeAgreementRun.match(samples, wrongConfig)).hasMessageContaining("q002-A");
        List<HumanLabel> extra = new java.util.ArrayList<>(labels);
        extra.add(new HumanLabel("q009-C", "q009", "C", true, 5, ""));
        assertThatThrownBy(() -> JudgeAgreementRun.match(samples, extra)).hasMessageContaining("q009-C");
    }

    @Test
    void judgeIsFaithfulOnlyWhenEveryClaimIsSupportedAndTheReportSaysSo() {
        Map<String, HumanLabel> byId = JudgeAgreementRun.match(samples, labels);
        Map<String, JudgeVerdict> verdicts = Map.of(
                "q001-C", verdict(5, true, true),
                "q002-C", verdict(4, true, false),    // one unsupported claim: judge says not faithful
                "q002-A", verdict(2, false, false));

        List<JudgeAgreementRun.Row> rows = JudgeAgreementRun.rows(samples, byId, s -> verdicts.get(s.labelId()));

        assertThat(rows).extracting(JudgeAgreementRun.Row::judgeFaithful).containsExactly(true, false, false);
        assertThat(rows.get(1).unsupportedClaims()).extracting(Claim::claim).containsExactly("claim 2");
        assertThat(rows.get(1).faithfulDisagrees()).isTrue();
        assertThat(rows.get(2).correctnessDisagrees()).isFalse();   // 1 vs 2 is within one point

        JudgeAgreementRun.Report report = JudgeAgreementRun.report("2026-11-04", "dev", "gpt-4.1",
                JudgeAgreementRun.sha256("prompt"), samples, rows, 0.01);
        assertThat(report.fromConfigC()).isEqualTo(2);
        assertThat(report.agreement().faithfulness().agreement()).isEqualTo(2.0 / 3);
        assertThat(report.agreement().correctness().exact()).isEqualTo(2.0 / 3);
        assertThat(report.rubricNeedsWork()).isTrue();   // kappa 0.4 is below 0.6

        String md = JudgeAgreementRun.markdown(report);
        assertThat(md).contains("| **Person faithful** | 1 | 1 |")
                .contains("| **Person not faithful** | 0 | 1 |")
                .contains("Agreement: 67%")
                .contains("Cohen's kappa: 0.40")
                .contains("**q002-C**")
                .contains("Unsupported: \"claim 2\"")
                .contains("Below the bar");
        assertThat(md).doesNotContain("**q001-C**").doesNotContain("—");
    }

    @Test
    void rubricHashIsStable() {
        assertThat(JudgeAgreementRun.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private static JudgeVerdict verdict(int correctness, boolean first, boolean second) {
        return new JudgeVerdict(List.of(new Claim("claim 1", first ? List.of(2) : List.of(), "r", first),
                new Claim("claim 2", second ? List.of(2) : List.of(), "r", second)), List.of(), correctness,
                "reason " + correctness, "gpt-4.1", 100, 50, 1);
    }
}
