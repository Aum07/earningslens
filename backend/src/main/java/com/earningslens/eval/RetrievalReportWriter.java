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
import java.util.Map;

/** Writes a retrieval eval report as <name>.json (everything) and <name>.md (tables to read and commit). */
@Component
public class RetrievalReportWriter {

    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    /** @return the paths written: JSON first, then Markdown */
    public List<Path> write(RetrievalEvalReport report, Path dir, String baseName) throws IOException {
        Files.createDirectories(dir);
        Path json = dir.resolve(baseName + ".json");
        Path md = dir.resolve(baseName + ".md");
        Files.writeString(json, mapper.writeValueAsString(report) + "\n", StandardCharsets.UTF_8);
        Files.writeString(md, markdown(report), StandardCharsets.UTF_8);
        return List.of(json, md);
    }

    static String markdown(RetrievalEvalReport r) {
        RetrievalEvalReport.Settings s = r.settings();
        StringBuilder md = new StringBuilder();
        md.append("# Retrieval eval: ").append(r.split()).append(" split, ").append(r.date()).append("\n\n");
        md.append("Retrieval only (no LLM calls). ").append(r.items()).append(" items; metrics use the answerable ones.\n\n");
        md.append("Settings: k ").append(s.k()).append(", k-per-period ").append(s.kPerPeriod())
                .append(", max-periods ").append(s.maxPeriods()).append(", candidates ").append(s.vectorCandidates())
                .append(" vector / ").append(s.keywordCandidates()).append(" keyword, rrf-k ").append(s.rrfK())
                .append(", chunks ").append(s.chunkTargetTokens()).append(" target / ").append(s.chunkOverlapTokens())
                .append(" overlap / ").append(s.chunkMaxTokens()).append(" max tokens, corpus ").append(s.corpusChunks())
                .append(" chunks, ").append(s.embeddingModel()).append(".\n\n");

        md.append("## Overall\n\n");
        md.append("| Config | Answerable | Recall@k (all quotes) | Recall@k (any quote) | MRR | Period coverage | Avg chunks | Avg latency ms |\n");
        md.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (RetrievalEvalReport.ConfigRun run : r.runs()) {
            RetrievalMetrics.Summary o = run.overall();
            md.append("| ").append(run.config()).append(" | ").append(o.answerableItems())
                    .append(" | ").append(num(o.recallAll())).append(" | ").append(num(o.recallAny()))
                    .append(" | ").append(num(o.mrr())).append(" | ").append(coverage(o))
                    .append(" | ").append(String.format(Locale.ROOT, "%.1f", o.avgRetrieved()))
                    .append(" | ").append(String.format(Locale.ROOT, "%.0f", run.avgLatencyMs())).append(" |\n");
        }

        md.append("\n## By category\n\n");
        md.append("| Config | Category | Items | Recall@k (all) | Recall@k (any) | MRR | Period coverage |\n");
        md.append("|---|---|---:|---:|---:|---:|---:|\n");
        for (RetrievalEvalReport.ConfigRun run : r.runs()) {
            for (Map.Entry<String, RetrievalMetrics.Summary> e : run.byCategory().entrySet()) {
                RetrievalMetrics.Summary c = e.getValue();
                md.append("| ").append(run.config()).append(" | ").append(e.getKey()).append(" | ").append(c.answerableItems())
                        .append(" | ").append(num(c.recallAll())).append(" | ").append(num(c.recallAny()))
                        .append(" | ").append(num(c.mrr())).append(" | ").append(coverage(c)).append(" |\n");
            }
        }

        for (RetrievalEvalReport.ConfigRun run : r.runs()) {
            List<RetrievalEvalReport.ItemRow> misses = new ArrayList<>();
            for (RetrievalEvalReport.ItemRow row : run.items()) {
                if (row.score().goldQuotes() > 0 && !row.score().allFound()) {
                    misses.add(row);
                }
            }
            md.append("\n## Config ").append(run.config()).append(": items with a gold quote not retrieved (")
                    .append(misses.size()).append(")\n\n");
            if (misses.isEmpty()) {
                md.append("None.\n");
                continue;
            }
            md.append("| Item | Category | Found | First hit rank | Strategy | Searched | Missed quote |\n");
            md.append("|---|---|---:|---:|---|---|---|\n");
            for (RetrievalEvalReport.ItemRow row : misses) {
                RetrievalMetrics.ItemScore sc = row.score();
                md.append("| ").append(row.id()).append(" | ").append(row.category()).append(" | ")
                        .append(sc.goldFound()).append("/").append(sc.goldQuotes()).append(" | ")
                        .append(sc.firstHitRank() == null ? "-" : sc.firstHitRank()).append(" | ")
                        .append(row.strategy()).append(" | ").append(cell(String.join(", ", row.searchedTargets())))
                        .append(" | ").append(cell(sc.missedQuotes().isEmpty() ? "" : sc.missedQuotes().get(0))).append(" |\n");
            }
        }

        md.append("\n## Where the gold chunks rank within their own quarter\n\n");
        md.append("Vector and keyword search limited to the quote's company and quarter, with the full question. ")
                .append("A rank above k means fusion cannot pick that chunk; \"-\" means not found at all.\n\n");
        List<RetrievalEvalReport.GoldRank> ranks = r.goldRanks() == null ? List.of() : r.goldRanks();
        md.append("Vector rank of the gold chunk: ").append(bucketLine(ranks.stream().map(RetrievalEvalReport.GoldRank::vectorRank).toList()))
                .append("  \nKeyword rank of the gold chunk: ").append(bucketLine(ranks.stream().map(RetrievalEvalReport.GoldRank::keywordRank).toList()))
                .append("\n\n");
        md.append("Replaying the real search for that quarter (top ").append(r.settings().vectorCandidates())
                .append(" vector + top ").append(r.settings().keywordCandidates()).append(" keyword, RRF, dedupe), gold chunk in top ")
                .append(r.settings().k()).append(": ")
                .append(ranks.stream().filter(g -> g.fusedRank() != null && g.fusedRank() <= r.settings().k()).count())
                .append(" of ").append(ranks.size()).append(" before dedupe, ")
                .append(ranks.stream().filter(g -> g.dedupedRank() != null && g.dedupedRank() <= r.settings().k()).count())
                .append(" after dedupe.\n\n");
        md.append("| Item | Evidence | Vector rank (all) | Vector rank (top ").append(r.settings().vectorCandidates())
                .append(") | Keyword rank | Fused rank | After dedupe | Chunks in quarter | Quote |\n");
        md.append("|---|---|---:|---:|---:|---:|---:|---:|---|\n");
        for (RetrievalEvalReport.GoldRank g : ranks) {
            md.append("| ").append(g.itemId()).append(" | ").append(g.evidence()).append(" | ")
                    .append(dash(g.vectorRank())).append(" | ").append(dash(g.vectorRankTopN())).append(" | ")
                    .append(dash(g.keywordRank())).append(" | ").append(dash(g.fusedRank())).append(" | ")
                    .append(dash(g.dedupedRank())).append(" | ")
                    .append(g.chunksInQuarter()).append(" | ").append(cell(g.quote())).append(" |\n");
        }

        appendThreshold(md, r.threshold());

        md.append("\n## Unanswerable items\n\n");
        md.append("Best vector similarity per config (input for the E3.7 threshold), and any abstention.\n\n");
        StringBuilder header = new StringBuilder("| Item |");
        StringBuilder rule = new StringBuilder("|---|");
        for (RetrievalEvalReport.ConfigRun run : r.runs()) {
            header.append(" ").append(run.config()).append(" |");
            rule.append("---|");
        }
        md.append(header).append("\n").append(rule).append("\n");
        if (!r.runs().isEmpty()) {
            List<RetrievalEvalReport.ItemRow> first = r.runs().get(0).items();
            for (int i = 0; i < first.size(); i++) {
                if (!first.get(i).category().equals(EvalCategory.UNANSWERABLE.jsonName())) {
                    continue;
                }
                md.append("| ").append(first.get(i).id()).append(" |");
                for (RetrievalEvalReport.ConfigRun run : r.runs()) {
                    RetrievalEvalReport.ItemRow row = run.items().get(i);
                    String sim = row.bestSimilarity() == null ? "-" : num(row.bestSimilarity());
                    md.append(" ").append(sim).append(row.abstainReason() == null ? "" : " (" + row.abstainReason() + ")").append(" |");
                }
                md.append("\n");
            }
        }
        return md.toString();
    }

    private static void appendThreshold(StringBuilder md, RetrievalEvalReport.Threshold t) {
        if (t == null) {
            return;
        }
        md.append("\n## Evidence threshold (E3.7), from config ").append(t.config()).append("\n\n");
        md.append("Abstain when the best vector similarity is below the threshold. \"Abstain\" is the prediction for ")
                .append("\"unanswerable\": precision = abstentions that were right, recall = unanswerable questions caught. ")
                .append("Questions whose period is not in the corpus abstain at every threshold.\n\n");
        if (t.suggested() != null) {
            ThresholdTuning.Row s = t.suggested();
            md.append("Suggested (highest F1, ties to the lower threshold): **").append(String.format(Locale.ROOT, "%.3f", s.threshold()))
                    .append("**: catches ").append(s.unanswerableAbstained()).append("/").append(s.unanswerable())
                    .append(" unanswerable, blocks ").append(s.answerableBlocked()).append("/").append(s.answerable())
                    .append(" answerable").append(s.blockedAnswerable().isEmpty() ? "" : " (" + String.join(", ", s.blockedAnswerable()) + ")")
                    .append(".\n\n");
        }
        md.append("| Threshold | Unanswerable caught | Answerable blocked | Precision | Recall | F1 | Blocked answerable items |\n");
        md.append("|---:|---:|---:|---:|---:|---:|---|\n");
        for (ThresholdTuning.Row row : t.rows()) {
            md.append("| ").append(String.format(Locale.ROOT, "%.3f", row.threshold())).append(" | ")
                    .append(row.unanswerableAbstained()).append("/").append(row.unanswerable()).append(" | ")
                    .append(row.answerableBlocked()).append("/").append(row.answerable()).append(" | ")
                    .append(num(row.precision())).append(" | ").append(num(row.recall())).append(" | ")
                    .append(num(row.f1())).append(" | ").append(cell(String.join(", ", row.blockedAnswerable()))).append(" |\n");
        }
        md.append("\nBest vector similarity per item, lowest first:\n\n| Item | Answerable | Best similarity | Note |\n|---|---|---:|---|\n");
        t.items().stream()
                .sorted(java.util.Comparator.comparing(
                        (ThresholdTuning.ItemSimilarity i) -> i.bestSimilarity() == null ? -1.0 : i.bestSimilarity()))
                .forEach(i -> md.append("| ").append(i.id()).append(" | ").append(i.answerable() ? "yes" : "no").append(" | ")
                        .append(i.bestSimilarity() == null ? "-" : num(i.bestSimilarity())).append(" | ")
                        .append(i.abstainsAnyway() ? "period not in corpus" : "").append(" |\n"));
    }

    /** "12 quotes: 3 at rank 1-3, 4 at 4-8, 2 at 9-30, 3 below 30 or not found". */
    static String bucketLine(List<Integer> ranks) {
        int top3 = 0, top8 = 0, top30 = 0, rest = 0;
        for (Integer rank : ranks) {
            if (rank == null || rank > 30) {
                rest++;
            } else if (rank <= 3) {
                top3++;
            } else if (rank <= 8) {
                top8++;
            } else {
                top30++;
            }
        }
        return ranks.size() + " quotes: " + top3 + " at rank 1-3, " + top8 + " at 4-8, " + top30 + " at 9-30, "
                + rest + " below 30 or not found";
    }

    private static String dash(Integer value) {
        return value == null ? "-" : value.toString();
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String coverage(RetrievalMetrics.Summary s) {
        return s.periodCoverage() == null ? "-"
                : num(s.periodCoverage()) + " (" + s.coveredPeriods() + "/" + s.requiredPeriods() + ")";
    }

    private static String cell(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
    }
}
