package com.earningslens.eval;

import org.springframework.stereotype.Component;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the full eval reports (spec 11.5, roadmap E5.3): one {date}-{config}-{split}[-label].json and .md per
 * config, and {date}-ablation-{split}[-label].md with one row per config when more than one ran.
 * Every number in these files comes from the run (spec rule 7).
 */
@Component
public class FullReportWriter {

    /** Settings that produced the numbers, so a report can be reproduced. */
    record Settings(int k, int kPerPeriod, int maxPeriods, double minSimilarity, int chunkTargetTokens,
                    int chunkOverlapTokens, int corpusChunks, String chatModel, String embeddingModel,
                    String judgeModel, String rubricPromptSha256) {
    }

    record Report(String date, String split, String config, String label, Settings settings,
                  FullEvalMetrics.Summary summary, List<FullEvalMetrics.ItemResult> items) {
    }

    /**
     * eval/reports/latest.json, served by GET /api/v1/eval/latest (spec 11.5). Only the final test-split run
     * writes it. Every number is copied from generated reports: the config summaries from this run and the
     * judge agreement from the judge-agreement report named in eval.judge-agreement-report.
     *
     * @param sameRubric true if the agreement report used the same judge prompt as this run (sha-256 match)
     */
    record Latest(String date, String split, List<String> reports, String ablationReport, Settings settings,
                  List<FullEvalMetrics.Summary> configs, JudgeAgreement judgeAgreement) {
    }

    record JudgeAgreement(String report, Object agreement, Boolean sameRubric) {
    }

    static final String LATEST = "latest.json";

    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    /** Writes one config's .json and .md and returns their paths. */
    List<Path> write(Report report, Path dir) throws IOException {
        Files.createDirectories(dir);
        String base = baseName(report.date(), report.config(), report.split(), report.label());
        Path json = dir.resolve(base + ".json");
        Path md = dir.resolve(base + ".md");
        Files.writeString(json, mapper.writeValueAsString(report) + "\n", StandardCharsets.UTF_8);
        Files.writeString(md, markdown(report), StandardCharsets.UTF_8);
        return List.of(json, md);
    }

    Path writeAblation(List<Report> reports, Path dir) throws IOException {
        Report first = reports.get(0);
        Path md = dir.resolve(baseName(first.date(), "ablation", first.split(), first.label()) + ".md");
        Files.writeString(md, ablation(reports), StandardCharsets.UTF_8);
        return md;
    }

    /** Writes latest.json for a finished test-split run. */
    Path writeLatest(List<Report> reports, Path dir, Path agreementReport) throws IOException {
        Report first = reports.get(0);
        List<String> files = reports.stream()
                .map(r -> baseName(r.date(), r.config(), r.split(), r.label()) + ".json").toList();
        String ablation = reports.size() > 1 ? baseName(first.date(), "ablation", first.split(), first.label()) + ".md" : null;
        Latest latest = new Latest(first.date(), first.split(), files, ablation, first.settings(),
                reports.stream().map(Report::summary).toList(), agreement(agreementReport, first.settings()));
        Path path = dir.resolve(LATEST);
        Files.writeString(path, mapper.writeValueAsString(latest) + "\n", StandardCharsets.UTF_8);
        return path;
    }

    private JudgeAgreement agreement(Path report, Settings settings) {
        if (report == null || !Files.isRegularFile(report)) {
            return null;
        }
        java.util.Map<?, ?> json = mapper.readValue(report.toFile(), java.util.Map.class);
        Object sha = json.get("rubricPromptSha256");
        return new JudgeAgreement(report.getFileName().toString(), json.get("agreement"),
                sha == null ? null : sha.equals(settings.rubricPromptSha256()));
    }

    static String baseName(String date, String config, String split, String label) {
        String name = date + "-" + config + "-" + split;
        if (label != null && !label.isBlank()) {
            name += "-" + label.strip().replaceAll("[^A-Za-z0-9_-]", "_");
        }
        return name;
    }

    static String markdown(Report r) {
        FullEvalMetrics.Summary s = r.summary();
        RetrievalMetrics.Summary ret = s.retrieval();
        FullEvalMetrics.Generation g = s.generation();
        FullEvalMetrics.Abstention a = s.abstention();
        FullEvalMetrics.Operations o = s.operations();
        StringBuilder md = new StringBuilder();
        md.append("# Full eval: config ").append(r.config()).append(", ").append(r.split()).append(" split, ")
                .append(r.date()).append("\n\n");
        md.append(s.items()).append(" items");
        if (s.errors() > 0) {
            md.append(", **").append(s.errors()).append(" left out because of errors** (see the end)");
        }
        Settings st = r.settings();
        md.append(". Settings: k ").append(st.k()).append(", k per period ").append(st.kPerPeriod())
                .append(", min similarity ").append(st.minSimilarity()).append(", chunks ").append(st.chunkTargetTokens())
                .append("/").append(st.chunkOverlapTokens()).append(" tokens (").append(st.corpusChunks())
                .append(" in corpus). Answer model ").append(st.chatModel()).append(", judge ").append(st.judgeModel())
                .append(" (rubric sha-256 `").append(st.rubricPromptSha256(), 0, 12).append("`).\n");

        md.append("\n## Retrieval (").append(ret.answerableItems()).append(" answerable items)\n\n");
        md.append("| | Recall@k all quotes | Recall@k any quote | MRR | Period coverage |\n|---|---|---|---|---|\n");
        md.append(retrievalRow("All", ret));
        s.retrievalByCategory().forEach((category, sum) -> md.append(retrievalRow(category, sum)));

        md.append("\n## Generation (").append(g.judged()).append(" answers judged)\n\n");
        md.append("| Metric | Value |\n|---|---|\n");
        md.append("| Faithfulness (mean supported / claims) | ").append(f(g.faithfulness())).append(" |\n");
        md.append("| Fully faithful answers | ").append(pct(g.fullyFaithful())).append(" |\n");
        md.append("| Citation precision (").append(g.citationPairs()).append(" sentence/citation pairs) | ")
                .append(pct(g.citationPrecision())).append(" |\n");
        md.append("| Number grounding rate (").append(g.numbers()).append(" numbers) | ")
                .append(pct(g.numberGroundingRate())).append(" |\n");
        md.append("| Correctness, all items (abstentions: 5 if unanswerable, 1 if answerable) | ")
                .append(f(g.correctness())).append(" |\n");
        md.append("| Correctness, answered answerable items (judge only) | ")
                .append(f(g.correctnessAnsweredAnswerable())).append(" |\n");
        g.correctnessByCategory().forEach((category, value) ->
                md.append("| Correctness, ").append(category).append(" | ").append(f(value)).append(" |\n"));

        md.append("\n## Abstention\n\n");
        md.append("- Unanswerable items abstained: ").append(a.unanswerableAbstained()).append(" of ")
                .append(a.unanswerable()).append(" (recall ").append(pct(a.recall())).append(")\n");
        md.append("- Answerable items abstained: ").append(a.answerableAbstained()).append(" of ").append(a.answerable())
                .append(" (false-abstention rate ").append(pct(a.falseAbstentionRate())).append(")\n");
        md.append("- Precision (abstentions that were right): ").append(pct(a.precision())).append('\n');
        md.append("- By reason: ").append(a.byReason().isEmpty() ? "none" : a.byReason().toString()).append('\n');

        md.append("\n## Operations\n\n");
        md.append("- Latency (retrieval + answer): p50 ").append(o.latencyP50Ms()).append(" ms, p95 ")
                .append(o.latencyP95Ms()).append(" ms\n");
        md.append("- Cost per query (embedding + answer model): $").append(usd(o.meanCostUsd()))
                .append(", total $").append(usd(o.totalCostUsd())).append('\n');
        md.append("- Judge cost for this run (not part of a user query): $").append(usd(o.judgeCostUsd())).append('\n');

        md.append("\n## Items\n\n");
        md.append("| Item | Category | Result | Gold quotes found | Faithfulness | Correctness | Citations | Numbers grounded | Latency ms |\n");
        md.append("|---|---|---|---|---|---|---|---|---|\n");
        for (FullEvalMetrics.ItemResult item : r.items()) {
            md.append("| ").append(item.id()).append(" | ").append(item.category()).append(" | ")
                    .append(!item.ok() ? "error" : item.abstained() ? "abstained: " + item.abstainReason() : "answered")
                    .append(" | ").append(item.retrieval() == null || item.retrieval().goldQuotes() == 0 ? "-"
                            : item.retrieval().goldFound() + "/" + item.retrieval().goldQuotes())
                    .append(" | ").append(f(item.judgeFaithfulness()))
                    .append(" | ").append(item.effectiveCorrectness() == null ? "-" : item.effectiveCorrectness())
                    .append(" | ").append(item.citationPairs() == null ? "-" : item.supportingCitations() + "/" + item.citationPairs())
                    .append(" | ").append(item.numbers() == null || item.numbers() == 0 ? "-" : item.groundedNumbers() + "/" + item.numbers())
                    .append(" | ").append(item.latencyMs()).append(" |\n");
        }

        List<String> problems = new ArrayList<>();
        for (FullEvalMetrics.ItemResult item : r.items()) {
            if (!item.ok()) {
                problems.add("- **" + item.id() + "** error: " + oneLine(item.error()));
            }
            item.unsupportedClaims().forEach(c -> problems.add("- **" + item.id() + "** unsupported claim: " + oneLine(c)));
            item.ungroundedNumbers().forEach(n -> problems.add("- **" + item.id() + "** " + oneLine(n)));
        }
        md.append("\n## Problems found\n\n");
        md.append(problems.isEmpty() ? "None.\n" : String.join("\n", problems) + "\n");
        return md.toString();
    }

    static String ablation(List<Report> reports) {
        Report first = reports.get(0);
        StringBuilder md = new StringBuilder();
        md.append("# Ablation: ").append(first.split()).append(" split, ").append(first.date()).append("\n\n");
        md.append("One row per config (spec 11.4). Source files: ");
        md.append(String.join(", ", reports.stream()
                .map(r -> "`" + baseName(r.date(), r.config(), r.split(), r.label()) + ".json`").toList()));
        md.append(".\n\n");
        md.append("| Config | Recall@k all | Recall@k any | MRR | Period coverage | Faithfulness | Fully faithful | "
                + "Citation precision | Number grounding | Correctness | Abstention precision | Abstention recall | "
                + "False abstention | Latency p50 / p95 ms | Cost per query |\n");
        md.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (Report r : reports) {
            FullEvalMetrics.Summary s = r.summary();
            RetrievalMetrics.Summary ret = s.retrieval();
            FullEvalMetrics.Generation g = s.generation();
            FullEvalMetrics.Abstention a = s.abstention();
            FullEvalMetrics.Operations o = s.operations();
            md.append("| ").append(r.config()).append(" | ").append(f(ret.recallAll())).append(" | ")
                    .append(f(ret.recallAny())).append(" | ").append(f(ret.mrr())).append(" | ")
                    .append(f(ret.periodCoverage())).append(" | ").append(f(g.faithfulness())).append(" | ")
                    .append(pct(g.fullyFaithful())).append(" | ").append(pct(g.citationPrecision())).append(" | ")
                    .append(pct(g.numberGroundingRate())).append(" | ").append(f(g.correctness())).append(" | ")
                    .append(pct(a.precision())).append(" | ").append(pct(a.recall())).append(" | ")
                    .append(pct(a.falseAbstentionRate())).append(" | ").append(o.latencyP50Ms()).append(" / ")
                    .append(o.latencyP95Ms()).append(" | $").append(usd(o.meanCostUsd())).append(" |\n");
        }
        long errors = reports.stream().mapToInt(r -> r.summary().errors()).sum();
        if (errors > 0) {
            md.append("\n").append(errors).append(" items were left out because of errors; see each config's report.\n");
        }
        return md.toString();
    }

    private static String retrievalRow(String name, RetrievalMetrics.Summary s) {
        return "| " + name + " | " + f(s.recallAll()) + " | " + f(s.recallAny()) + " | " + f(s.mrr()) + " | "
                + f(s.periodCoverage()) + " |\n";
    }

    static String f(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "%.2f", value);
    }

    static String pct(Double share) {
        return share == null ? "-" : String.format(Locale.ROOT, "%.0f%%", share * 100);
    }

    static String usd(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String oneLine(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ").replace("|", "/");
    }
}
