package com.earningslens.eval;

import com.earningslens.ingest.TokenCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Roadmap E5.2 step 2 (--eval.task=judge-agreement): judges the answers saved by label-prep, compares the
 * judge with the person's labels in eval/human-labels.jsonl, and writes
 * eval/reports/{date}-judge-agreement-dev[-label].json and .md.
 * Done when: the agreement numbers are in a report file.
 *
 * Paid calls: the judge model only (no retrieval, no answer generation). The estimate uses the exact
 * prompt sizes of the saved answers.
 */
@Component
@Profile("eval")
public class JudgeAgreementRun {

    /** Below these, the rubric is considered not trustworthy yet and should be improved (DECISIONS.md). */
    static final double MIN_KAPPA = 0.6;
    static final double MIN_CORRECTNESS_WITHIN_ONE = 0.8;

    private static final Logger log = LoggerFactory.getLogger(JudgeAgreementRun.class);

    private final EvalProperties eval;
    private final JudgeProperties judgeSettings;
    private final JudgeClient judge;
    private final TokenCounter tokens;
    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    public JudgeAgreementRun(EvalProperties eval, JudgeProperties judgeSettings, JudgeClient judge,
                             TokenCounter tokens) {
        this.eval = eval;
        this.judgeSettings = judgeSettings;
        this.judge = judge;
        this.tokens = tokens;
    }

    /** One answer: the person's labels next to the judge's. */
    record Row(String labelId, String category, boolean humanFaithful, boolean judgeFaithful,
               double judgeFaithfulness, int humanCorrectness, int judgeCorrectness, Double judgeCitationPrecision,
               String humanNote, String judgeCorrectnessReason, List<JudgeVerdict.Claim> unsupportedClaims,
               int judgeAttempts) {

        boolean faithfulDisagrees() {
            return humanFaithful != judgeFaithful;
        }

        boolean correctnessDisagrees() {
            return Math.abs(humanCorrectness - judgeCorrectness) > 1;
        }
    }

    record Report(String date, String split, String judgeModel, String rubricPromptSha256, int answers,
                  long fromConfigC, AgreementMetrics.Summary agreement, boolean rubricNeedsWork,
                  double minKappa, double minCorrectnessWithinOne, double actualUsd, List<Row> rows) {
    }

    public void run(EvalSplit split) throws IOException {
        if (split != EvalSplit.DEV) {
            throw new IllegalArgumentException("judge-agreement runs on the dev split only");
        }
        List<LabelSample> samples = mapper.readValue(Path.of(eval.labelAnswersPath()).toFile(),
                new TypeReference<List<LabelSample>>() { });
        List<HumanLabel> labels = HumanLabels.load(Path.of(eval.humanLabelsPath()));
        HumanLabels.requireComplete(labels);
        Map<String, HumanLabel> byId = match(samples, labels);

        long inTokens = 0;
        String system = judge.systemPrompt();
        for (LabelSample sample : samples) {
            inTokens += tokens.count(system) + tokens.count(JudgeClient.userMessage(sample.judgeInput()));
        }
        double typical = (inTokens * judgeSettings.inputPricePerMillionTokens()
                + samples.size() * 800L * judgeSettings.outputPricePerMillionTokens()) / 1_000_000.0;
        double worst = judgeSettings.maxAttempts() * (inTokens * judgeSettings.inputPricePerMillionTokens()
                + (long) samples.size() * judgeSettings.maxOutputTokens() * judgeSettings.outputPricePerMillionTokens())
                / 1_000_000.0;
        log.info("Judge agreement: {} labelled answers, judge {}. Judge input {} tokens in total. Cost about ${} "
                        + "(worst case ${}). No answer-model calls.", samples.size(), judgeSettings.model(), inTokens,
                String.format(Locale.ROOT, "%.3f", typical), String.format(Locale.ROOT, "%.3f", worst));
        if (!eval.confirm()) {
            log.warn("Nothing was sent to OpenAI. Re-run with --eval.confirm=true to judge the labelled answers.");
            return;
        }

        double[] cost = {0.0};
        List<Row> rows = rows(samples, byId, sample -> {
            JudgeVerdict verdict = judge.judge(sample.judgeInput());
            cost[0] += ((verdict.promptTokens() == null ? 0 : verdict.promptTokens())
                    * judgeSettings.inputPricePerMillionTokens()
                    + (verdict.completionTokens() == null ? 0 : verdict.completionTokens())
                    * judgeSettings.outputPricePerMillionTokens()) / 1_000_000.0;
            log.info("{}: judge faithful {} ({} of {} claims), correctness {}", sample.labelId(),
                    verdict.supportedClaims() == verdict.claims().size(), verdict.supportedClaims(),
                    verdict.claims().size(), verdict.correctness());
            return verdict;
        });

        Report report = report(LocalDate.now().toString(), split.jsonName(), judgeSettings.model(), sha256(system),
                samples, rows, Math.round(cost[0] * 1_000_000.0) / 1_000_000.0);

        Path dir = Path.of(eval.reportsDir());
        Files.createDirectories(dir);
        String suffix = eval.label() == null || eval.label().isBlank() ? "" : "-" + eval.label().strip();
        Path json = dir.resolve(report.date() + "-judge-agreement-dev" + suffix + ".json");
        Path md = dir.resolve(report.date() + "-judge-agreement-dev" + suffix + ".md");
        Files.writeString(json, mapper.writeValueAsString(report) + "\n", StandardCharsets.UTF_8);
        Files.writeString(md, markdown(report), StandardCharsets.UTF_8);

        AgreementMetrics.Faithfulness f = report.agreement().faithfulness();
        AgreementMetrics.Correctness c = report.agreement().correctness();
        log.info("Faithful agreement {} , Cohen's kappa {}. Correctness exact {}, within one {}, judge minus person {}.",
                pct(f.agreement()), f.kappa() == null ? "undefined" : f2(f.kappa()), pct(c.exact()), pct(c.withinOne()),
                f2(c.meanJudgeMinusHuman()));
        if (report.rubricNeedsWork()) {
            log.warn("Agreement is below the bar (kappa >= {}, within-one >= {}): improve the rubric, log the change "
                    + "in its change log, and run again with --eval.label=v2.", MIN_KAPPA, MIN_CORRECTNESS_WITHIN_ONE);
        } else {
            log.info("Agreement meets the bar (kappa >= {}, within-one >= {}).", MIN_KAPPA, MIN_CORRECTNESS_WITHIN_ONE);
        }
        log.info("Report: {} and {}. Actual cost about ${}.", json, md, String.format(Locale.ROOT, "%.4f", cost[0]));
    }

    /** Every sample needs exactly one label with the same id, item and config, and there are no extra labels. */
    static Map<String, HumanLabel> match(List<LabelSample> samples, List<HumanLabel> labels) {
        Map<String, HumanLabel> byId = new LinkedHashMap<>();
        labels.forEach(l -> byId.put(l.labelId(), l));
        for (LabelSample sample : samples) {
            HumanLabel label = byId.get(sample.labelId());
            if (label == null) {
                throw new IllegalArgumentException("no human label for " + sample.labelId());
            }
            if (!label.itemId().equals(sample.itemId()) || !label.config().equals(sample.config())) {
                throw new IllegalArgumentException("label " + label.labelId() + " has item/config " + label.itemId()
                        + "/" + label.config() + " but the saved answer is " + sample.itemId() + "/" + sample.config());
            }
        }
        if (byId.size() != samples.size()) {
            List<String> extra = byId.keySet().stream()
                    .filter(id -> samples.stream().noneMatch(s -> s.labelId().equals(id))).toList();
            throw new IllegalArgumentException("labels without a saved answer: " + extra);
        }
        return byId;
    }

    /** Judges every sample (the function is the judge, a fake in tests) and pairs it with its label. */
    static List<Row> rows(List<LabelSample> samples, Map<String, HumanLabel> labels,
                          Function<LabelSample, JudgeVerdict> judgeFn) {
        List<Row> rows = new ArrayList<>();
        for (LabelSample sample : samples) {
            HumanLabel label = labels.get(sample.labelId());
            JudgeVerdict verdict = judgeFn.apply(sample);
            boolean judgeFaithful = verdict.supportedClaims() == verdict.claims().size();
            List<JudgeVerdict.Claim> unsupported = verdict.claims().stream().filter(c -> !c.supported()).toList();
            rows.add(new Row(sample.labelId(), sample.category(), label.faithful(), judgeFaithful,
                    verdict.faithfulness(), label.correctness(), verdict.correctness(), verdict.citationPrecision(),
                    label.note(), verdict.correctnessReason(), unsupported, verdict.attempts()));
        }
        return rows;
    }

    static Report report(String date, String split, String judgeModel, String rubricSha, List<LabelSample> samples,
                         List<Row> rows, double actualUsd) {
        List<AgreementMetrics.Pair> pairs = rows.stream()
                .map(r -> new AgreementMetrics.Pair(r.humanFaithful(), r.judgeFaithful(), r.humanCorrectness(),
                        r.judgeCorrectness()))
                .toList();
        AgreementMetrics.Summary summary = AgreementMetrics.summarize(pairs);
        Double kappa = summary.faithfulness().kappa();
        boolean needsWork = kappa == null || kappa < MIN_KAPPA
                || summary.correctness().withinOne() < MIN_CORRECTNESS_WITHIN_ONE;
        long fromC = samples.stream().filter(s -> s.config().equals("C")).count();
        return new Report(date, split, judgeModel, rubricSha, rows.size(), fromC, summary, needsWork, MIN_KAPPA,
                MIN_CORRECTNESS_WITHIN_ONE, actualUsd, rows);
    }

    static String markdown(Report r) {
        AgreementMetrics.Faithfulness f = r.agreement().faithfulness();
        AgreementMetrics.Correctness c = r.agreement().correctness();
        StringBuilder md = new StringBuilder();
        md.append("# Judge agreement with human labels: ").append(r.split()).append(" split, ").append(r.date())
                .append("\n\n");
        md.append(r.answers()).append(" answers labelled by hand (").append(r.fromConfigC()).append(" from config C, ")
                .append(r.answers() - r.fromConfigC()).append(" from config A). Judge: ").append(r.judgeModel())
                .append(", rubric prompt sha-256 `").append(r.rubricPromptSha256(), 0, 12).append("`.\n\n");

        md.append("## Faithfulness (yes/no)\n\n");
        md.append("The judge counts as \"faithful\" when every claim it found is supported.\n\n");
        md.append("| | Judge faithful | Judge not faithful |\n|---|---|---|\n");
        md.append("| **Person faithful** | ").append(f.bothYes()).append(" | ").append(f.humanYesJudgeNo()).append(" |\n");
        md.append("| **Person not faithful** | ").append(f.humanNoJudgeYes()).append(" | ").append(f.bothNo()).append(" |\n\n");
        md.append("- Agreement: ").append(pct(f.agreement())).append('\n');
        md.append("- Cohen's kappa: ").append(f.kappa() == null ? "undefined (one label only)" : f2(f.kappa()))
                .append('\n');

        md.append("\n## Correctness (1 to 5)\n\n");
        md.append("- Exact agreement: ").append(pct(c.exact())).append('\n');
        md.append("- Within one point: ").append(pct(c.withinOne())).append('\n');
        md.append("- Mean judge minus person: ").append(f2(c.meanJudgeMinusHuman()))
                .append(" (positive = the judge is more generous)\n\n");
        md.append("| Person \\ Judge | 1 | 2 | 3 | 4 | 5 |\n|---|---|---|---|---|---|\n");
        for (int h = 0; h < 5; h++) {
            md.append("| **").append(h + 1).append("** |");
            for (int j = 0; j < 5; j++) {
                md.append(' ').append(c.matrix().get(h).get(j)).append(" |");
            }
            md.append('\n');
        }

        md.append("\n## Verdict\n\n");
        md.append(r.rubricNeedsWork()
                ? "Below the bar (kappa at least " + r.minKappa() + " and correctness within one point at least "
                + pct(r.minCorrectnessWithinOne()) + "): improve the rubric before trusting judge numbers.\n"
                : "Meets the bar (kappa at least " + r.minKappa() + " and correctness within one point at least "
                + pct(r.minCorrectnessWithinOne()) + ").\n");

        md.append("\n## Disagreements\n\n");
        boolean any = false;
        for (Row row : r.rows()) {
            if (!row.faithfulDisagrees() && !row.correctnessDisagrees()) {
                continue;
            }
            any = true;
            md.append("- **").append(row.labelId()).append("** (").append(row.category()).append("): faithful person ")
                    .append(yesNo(row.humanFaithful())).append(" / judge ").append(yesNo(row.judgeFaithful()))
                    .append("; correctness person ").append(row.humanCorrectness()).append(" / judge ")
                    .append(row.judgeCorrectness()).append(". Judge: ").append(oneLine(row.judgeCorrectnessReason()));
            for (JudgeVerdict.Claim claim : row.unsupportedClaims()) {
                md.append(" Unsupported: \"").append(oneLine(claim.claim())).append("\" (").append(oneLine(claim.reason()))
                        .append(").");
            }
            if (row.humanNote() != null && !row.humanNote().isBlank()) {
                md.append(" Person's note: ").append(oneLine(row.humanNote()));
            }
            md.append('\n');
        }
        if (!any) {
            md.append("None (faithfulness matches and correctness is within one point everywhere).\n");
        }
        return md.toString();
    }

    static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String oneLine(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ").replace("|", "/");
    }

    private static String pct(double share) {
        return String.format(Locale.ROOT, "%.0f%%", share * 100);
    }

    private static String f2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
