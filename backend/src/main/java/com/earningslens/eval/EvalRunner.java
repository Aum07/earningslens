package com.earningslens.eval;

import com.earningslens.config.ChunkingProperties;
import com.earningslens.config.LlmProperties;
import com.earningslens.config.RetrievalProperties;
import com.earningslens.ingest.TextNormalizer;
import com.earningslens.ingest.TokenCounter;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.KeywordSearchRepository;
import com.earningslens.retrieval.PeriodResolver;
import com.earningslens.retrieval.PeriodTarget;
import com.earningslens.retrieval.QueryEmbedder;
import com.earningslens.retrieval.QueryParser;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.Retriever;
import com.earningslens.retrieval.SearchFilters;
import com.earningslens.retrieval.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The eval job (spec 11.5), profile "eval". Task "judge-sample" (E5.1) is handed to {@link JudgeSampleRun}.
 * Task "retrieval" (roadmap E3.6):
 * runs the retriever for every item of the split under each config and writes a report to
 * eval/reports/. No LLM calls; the only paid calls are the question embeddings.
 *
 * Safety rules:
 * - Retrieval-only runs are refused on the test split: test is run once, in E5.4, and never used for tuning.
 * - The cost estimate is printed first; nothing is sent to OpenAI unless --eval.confirm=true.
 * - Each question is embedded once and reused for every config.
 */
@Component
@Profile("eval")
public class EvalRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);

    static final String RETRIEVAL = "retrieval";
    static final String JUDGE_SAMPLE = "judge-sample";
    static final String LABEL_PREP = "label-prep";
    static final String JUDGE_AGREEMENT = "judge-agreement";
    static final String FULL = "full";
    static final List<String> TASKS = List.of(RETRIEVAL, JUDGE_SAMPLE, LABEL_PREP, JUDGE_AGREEMENT, FULL);

    private final EvalProperties eval;
    private final EvalDatasetLoader loader;
    private final QueryParser parser;
    private final PeriodResolver resolver;
    private final VectorSearchRepository vectorSearch;
    private final KeywordSearchRepository keywordSearch;
    private final QueryEmbedder embedder;
    private final RetrievalProperties retrieval;
    private final ChunkingProperties chunking;
    private final LlmProperties llm;
    private final TokenCounter tokens;
    private final JdbcTemplate jdbc;
    private final RetrievalReportWriter writer;
    private final JudgeSampleRun judgeSample;
    private final LabelPrepRun labelPrep;
    private final JudgeAgreementRun judgeAgreement;
    private final FullEvalRun fullEval;

    public EvalRunner(EvalProperties eval, EvalDatasetLoader loader, QueryParser parser, PeriodResolver resolver,
                      VectorSearchRepository vectorSearch, KeywordSearchRepository keywordSearch, QueryEmbedder embedder,
                      RetrievalProperties retrieval, ChunkingProperties chunking, LlmProperties llm, TokenCounter tokens,
                      JdbcTemplate jdbc, RetrievalReportWriter writer, JudgeSampleRun judgeSample,
                      LabelPrepRun labelPrep, JudgeAgreementRun judgeAgreement, FullEvalRun fullEval) {
        this.eval = eval;
        this.loader = loader;
        this.parser = parser;
        this.resolver = resolver;
        this.vectorSearch = vectorSearch;
        this.keywordSearch = keywordSearch;
        this.embedder = embedder;
        this.retrieval = retrieval;
        this.chunking = chunking;
        this.llm = llm;
        this.tokens = tokens;
        this.jdbc = jdbc;
        this.writer = writer;
        this.judgeSample = judgeSample;
        this.labelPrep = labelPrep;
        this.judgeAgreement = judgeAgreement;
        this.fullEval = fullEval;
    }

    @Override
    public void run(String... args) throws Exception {
        EvalSplit split = EvalSplit.fromJson(eval.split());
        checkAllowed(eval.task(), split, eval.finalTestRun());
        if (JUDGE_SAMPLE.equals(eval.task())) {
            judgeSample.run(split);   // E5.1: a few dev answers through the judge
            return;
        }
        if (LABEL_PREP.equals(eval.task())) {
            labelPrep.run(split);     // E5.2 step 1: answers to label by hand
            return;
        }
        if (JUDGE_AGREEMENT.equals(eval.task())) {
            judgeAgreement.run(split);   // E5.2 step 2: judge vs human labels
            return;
        }
        if (FULL.equals(eval.task())) {
            fullEval.run(split, parseModes(eval.config()));   // E5.3: every metric, every item
            return;
        }
        List<RetrievalMode> modes = parseModes(eval.config());

        List<EvalItem> items = loader.load(Path.of(eval.datasetPath()), split);
        Integer corpusChunks = jdbc.queryForObject("SELECT count(*) FROM chunks", Integer.class);
        if (corpusChunks == null || corpusChunks == 0) {
            throw new IllegalStateException("The chunks table is empty: run ingestion first (docker compose up -d?)");
        }

        long questionTokens = items.stream().mapToLong(i -> tokens.count(i.question())).sum();
        double estimate = questionTokens / 1_000_000.0 * eval.embeddingPricePerMillionTokens();
        log.info("Retrieval eval: {} {} items x configs {}. Embedding calls: at most {} ({} tokens, about ${}). No LLM calls.",
                items.size(), split.jsonName(), modes, items.size(), questionTokens,
                String.format(Locale.ROOT, "%.6f", estimate));
        if (!eval.confirm()) {
            log.warn("Nothing was sent to OpenAI. Re-run with --eval.confirm=true to run the eval.");
            return;
        }

        // One embedding per question, shared by every config and every sweep setting.
        Map<String, float[]> cache = new HashMap<>();
        QueryEmbedder cached = question -> cache.computeIfAbsent(question, embedder::embed);

        List<int[]> sweep = parseSweep(eval.sweep());
        if (sweep.isEmpty()) {
            runOnce(retrieval, modes, items, split, corpusChunks, cached, eval.label());
        } else {
            for (int[] kAndKpp : sweep) {
                RetrievalProperties settings = withK(retrieval, kAndKpp[0], kAndKpp[1]);
                String label = "k" + kAndKpp[0] + "-kpp" + kAndKpp[1]
                        + (eval.label() == null || eval.label().isBlank() ? "" : "-" + eval.label().strip());
                runOnce(settings, modes, items, split, corpusChunks, cached, label);
            }
        }
        log.info("Embedding calls made: {}", cache.size());
    }

    private void runOnce(RetrievalProperties settings, List<RetrievalMode> modes, List<EvalItem> items, EvalSplit split,
                         int corpusChunks, QueryEmbedder cached, String label) throws java.io.IOException {
        Retriever retriever = new Retriever(parser, resolver, vectorSearch, keywordSearch, cached, settings);

        List<RetrievalEvalReport.ConfigRun> runs = new ArrayList<>();
        for (RetrievalMode mode : modes) {
            runs.add(runConfig(retriever, mode, items));
        }

        List<RetrievalEvalReport.GoldRank> goldRanks = goldRanks(items, cached, settings);

        RetrievalEvalReport report = new RetrievalEvalReport(LocalDate.now().toString(), split.jsonName(), items.size(),
                new RetrievalEvalReport.Settings(settings.k(), settings.kPerPeriod(), settings.maxPeriods(),
                        settings.vectorCandidates(), settings.keywordCandidates(), settings.rrfK(),
                        settings.minSimilarity(), chunking.targetTokens(), chunking.overlapTokens(),
                        chunking.maxTokens(), corpusChunks, llm.embeddingModel()),
                runs, goldRanks, threshold(runs));
        List<Path> written = writer.write(report, Path.of(eval.reportsDir()),
                reportName(LocalDate.now(), modes, split, label));

        for (RetrievalEvalReport.ConfigRun run : runs) {
            RetrievalMetrics.Summary o = run.overall();
            log.info("[k {} / k-per-period {}] Config {}: recall@k all {} / any {}, MRR {}, period coverage {}, avg chunks {}",
                    settings.k(), settings.kPerPeriod(), run.config(), f(o.recallAll()), f(o.recallAny()), f(o.mrr()),
                    o.periodCoverage() == null ? "-" : f(o.periodCoverage()), f(o.avgRetrieved()));
            int[] abstained = abstentions(run);
            log.info("[k {} / k-per-period {}] Config {}: abstained on {}/{} unanswerable and {}/{} answerable questions",
                    settings.k(), settings.kPerPeriod(), run.config(), abstained[0], abstained[1], abstained[2], abstained[3]);
        }
        log.info("Report: {}", written);
    }

    /** {unanswerable abstained, unanswerable total, answerable abstained, answerable total} for one config run. */
    static int[] abstentions(RetrievalEvalReport.ConfigRun run) {
        int[] counts = new int[4];
        for (RetrievalEvalReport.ItemRow row : run.items()) {
            boolean unanswerable = row.category().equals(EvalCategory.UNANSWERABLE.jsonName());
            boolean abstained = row.abstainReason() != null;
            counts[unanswerable ? 1 : 3]++;
            if (abstained) {
                counts[unanswerable ? 0 : 2]++;
            }
        }
        return counts;
    }

    /** E3.7: threshold sweep over config C's best similarities (or D's if C was not run). */
    static RetrievalEvalReport.Threshold threshold(List<RetrievalEvalReport.ConfigRun> runs) {
        RetrievalEvalReport.ConfigRun run = runs.stream().filter(r -> r.config().equals("C")).findFirst()
                .orElse(runs.stream().filter(r -> r.config().equals("D")).findFirst().orElse(null));
        if (run == null) {
            return null;
        }
        List<ThresholdTuning.ItemSimilarity> items = new ArrayList<>();
        for (RetrievalEvalReport.ItemRow row : run.items()) {
            boolean answerable = !row.category().equals(EvalCategory.UNANSWERABLE.jsonName());
            boolean periodMissing = RetrievalResult.AbstainReason.PERIOD_NOT_IN_CORPUS.name().equals(row.abstainReason());
            items.add(new ThresholdTuning.ItemSimilarity(row.id(), answerable, row.bestSimilarity(), periodMissing));
        }
        List<ThresholdTuning.Row> rows = ThresholdTuning.sweep(items);
        return new RetrievalEvalReport.Threshold(run.config(), rows, ThresholdTuning.best(rows), items);
    }

    /** "8x3, 12x4" -> [[8, 3], [12, 4]]; empty or blank -> no sweep. */
    static List<int[]> parseSweep(String sweep) {
        List<int[]> out = new ArrayList<>();
        if (sweep == null || sweep.isBlank()) {
            return out;
        }
        for (String part : sweep.split(",")) {
            String p = part.strip().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) {
                continue;
            }
            String[] kv = p.split("x");
            try {
                int k = Integer.parseInt(kv[0].strip());
                int kpp = Integer.parseInt(kv[1].strip());
                if (kv.length != 2 || k <= 0 || kpp <= 0) {
                    throw new NumberFormatException();
                }
                out.add(new int[] {k, kpp});
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("bad eval.sweep entry '" + part.strip() + "', expected for example 12x4");
            }
        }
        return out;
    }

    /** A copy of the retrieval settings with another k and k-per-period. */
    static RetrievalProperties withK(RetrievalProperties s, int k, int kPerPeriod) {
        return new RetrievalProperties(k, kPerPeriod, s.maxPeriods(), s.vectorCandidates(), s.keywordCandidates(),
                s.rrfK(), s.minSimilarity(), s.mode());
    }

    private RetrievalEvalReport.ConfigRun runConfig(Retriever retriever, RetrievalMode mode, List<EvalItem> items) {
        List<RetrievalMetrics.ItemScore> scores = new ArrayList<>();
        List<RetrievalEvalReport.ItemRow> rows = new ArrayList<>();
        long totalNanos = 0;
        for (EvalItem item : items) {
            long start = System.nanoTime();
            RetrievalResult result = retriever.retrieve(item.question(), toFilters(item.filters()), mode);
            totalNanos += System.nanoTime() - start;

            RetrievalMetrics.ItemScore score = RetrievalMetrics.score(item, result.chunks());
            scores.add(score);
            rows.add(row(item, result, score));
        }

        Map<String, RetrievalMetrics.Summary> byCategory = new LinkedHashMap<>();
        for (EvalCategory category : EvalCategory.values()) {
            if (category == EvalCategory.UNANSWERABLE) {
                continue;   // no gold quotes, so no recall: see the unanswerable table instead
            }
            List<RetrievalMetrics.ItemScore> ofCategory = scores.stream().filter(s -> s.category() == category).toList();
            if (!ofCategory.isEmpty()) {
                byCategory.put(category.jsonName(), RetrievalMetrics.summarize(ofCategory));
            }
        }
        double avgMs = items.isEmpty() ? 0 : totalNanos / 1_000_000.0 / items.size();
        return new RetrievalEvalReport.ConfigRun(mode.name(), RetrievalMetrics.summarize(scores), byCategory, avgMs, rows);
    }

    /**
     * For every gold quote: rank of the first chunk containing it in vector search and in keyword
     * search, limited to that quote's company and quarter. Shows whether a miss comes from the
     * embedding, the keyword query or the fusion. Uses the cached question embeddings (no new API calls).
     */
    private List<RetrievalEvalReport.GoldRank> goldRanks(List<EvalItem> items, QueryEmbedder cached,
                                                         RetrievalProperties retrieval) {
        List<RetrievalEvalReport.GoldRank> ranks = new ArrayList<>();
        for (EvalItem item : items) {
            String searchText = item.question();
            for (GoldEvidence evidence : item.goldEvidence()) {
                SearchFilters quarter = SearchFilters.tickers(evidence.ticker()).withPeriods(evidence.period());
                Integer inQuarter = jdbc.queryForObject(
                        "SELECT count(*) FROM chunks WHERE ticker = ? AND fiscal_year = ? AND fiscal_quarter = ?",
                        Integer.class, evidence.ticker(), evidence.period().year(), evidence.period().quarter());
                int all = inQuarter == null ? 0 : inQuarter;
                String quote = evidence.normalizedQuote();

                Integer vectorRank = null;
                if (all > 0) {
                    for (var hit : vectorSearch.search(cached.embed(searchText), quarter, all)) {
                        if (TextNormalizer.normalize(hit.chunk().content()).contains(quote)) {
                            vectorRank = hit.rank();
                            break;
                        }
                    }
                }
                Integer keywordRank = null;
                if (all > 0) {
                    for (var hit : keywordSearch.search(searchText, quarter, all)) {
                        if (TextNormalizer.normalize(hit.chunk().content()).contains(quote)) {
                            keywordRank = hit.rank();
                            break;
                        }
                    }
                }
                // Replay one hybrid search over this quarter exactly as the retriever does it.
                List<com.earningslens.retrieval.VectorHit> vectorTop =
                        vectorSearch.search(cached.embed(searchText), quarter, retrieval.vectorCandidates());
                List<com.earningslens.retrieval.KeywordHit> keywordTop =
                        keywordSearch.search(searchText, quarter, retrieval.keywordCandidates());
                Integer vectorRankTopN = firstRank(vectorTop.stream().map(h -> h.chunk().content()).toList(), quote);
                List<FusedHit> fused = com.earningslens.retrieval.RrfFusion.fuse(vectorTop, keywordTop,
                        retrieval.rrfK(), retrieval.k() * 2);
                Integer fusedRank = firstRank(fused.stream().map(h -> h.chunk().content()).toList(), quote);
                Integer dedupedRank = firstRank(com.earningslens.retrieval.ChunkDeduper.dedupe(fused).stream()
                        .map(h -> h.chunk().content()).toList(), quote);

                ranks.add(new RetrievalEvalReport.GoldRank(item.id(),
                        evidence.ticker() + " " + evidence.period() + " " + evidence.docType(),
                        quote.length() <= 70 ? quote : quote.substring(0, 67) + "...",
                        vectorRank, keywordRank, all, vectorRankTopN, fusedRank, dedupedRank));
            }
        }
        return ranks;
    }

    /** 1-based position of the first text containing the quote, or null. */
    static Integer firstRank(List<String> contents, String quote) {
        for (int i = 0; i < contents.size(); i++) {
            if (TextNormalizer.normalize(contents.get(i)).contains(quote)) {
                return i + 1;
            }
        }
        return null;
    }

    private static RetrievalEvalReport.ItemRow row(EvalItem item, RetrievalResult result, RetrievalMetrics.ItemScore score) {
        List<String> quotes = item.goldEvidence().stream().map(GoldEvidence::normalizedQuote).toList();
        List<RetrievalEvalReport.ChunkRow> chunks = new ArrayList<>();
        for (FusedHit h : result.chunks()) {
            String content = TextNormalizer.normalize(h.chunk().content());
            boolean gold = quotes.stream().anyMatch(content::contains);
            chunks.add(new RetrievalEvalReport.ChunkRow(h.rank(), h.chunk().ticker(),
                    "FY" + h.chunk().fiscalYear() + " Q" + h.chunk().fiscalQuarter(), h.chunk().docType().name(),
                    h.chunk().section(), gold, h.chunk().id().toString()));
        }
        return new RetrievalEvalReport.ItemRow(item.id(), item.category().jsonName(), item.question(),
                result.strategy().name(), result.abstainReason() == null ? null : result.abstainReason().name(),
                result.bestSimilarity(), result.searchedTargets().stream().map(PeriodTarget::toString).toList(),
                result.skippedTargets().stream().map(PeriodTarget::toString).toList(), score, chunks);
    }

    /** Retrieval-only runs are for tuning, so they may only use the dev split. */
    static void checkAllowed(String task, EvalSplit split) {
        checkAllowed(task, split, false);
    }

    /**
     * Every task runs on dev. The test split is only for the one final run (E5.4): task "full" with
     * --eval.final-test-run=true. FullEvalRun.checkFinalTestRun then demands all four configs and no earlier test report.
     */
    static void checkAllowed(String task, EvalSplit split, boolean finalTestRun) {
        if (!TASKS.contains(task)) {
            throw new IllegalArgumentException("eval.task '" + task + "' is not available yet; only " + TASKS);
        }
        if (split == EvalSplit.TEST && !(FULL.equals(task) && finalTestRun)) {
            throw new IllegalArgumentException("eval.task '" + task + "' is refused on the test split: tune on dev only. "
                    + "The test split is run once (E5.4) with --eval.task=full --eval.config=A,B,C,D "
                    + "--eval.final-test-run=true.");
        }
    }

    static List<RetrievalMode> parseModes(String config) {
        List<RetrievalMode> modes = new ArrayList<>();
        for (String part : config.split(",")) {
            String name = part.strip().toUpperCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }
            try {
                RetrievalMode mode = RetrievalMode.valueOf(name);
                if (!modes.contains(mode)) {
                    modes.add(mode);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown eval.config '" + part.strip() + "', expected A, B, C or D");
            }
        }
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("eval.config is empty, expected for example A,B,C");
        }
        return modes;
    }

    /** For example 2026-11-02-retrieval-ABC-dev or 2026-11-02-retrieval-ABC-dev-k10. */
    static String reportName(LocalDate date, List<RetrievalMode> modes, EvalSplit split, String label) {
        StringBuilder configs = new StringBuilder();
        modes.forEach(m -> configs.append(m.name()));
        String name = date + "-retrieval-" + configs + "-" + split.jsonName();
        if (label != null && !label.isBlank()) {
            name += "-" + label.strip().replaceAll("[^A-Za-z0-9_-]", "_");
        }
        return name;
    }

    static SearchFilters toFilters(EvalFilters filters) {
        if (filters == null) {
            return SearchFilters.none();
        }
        return new SearchFilters(Set.copyOf(filters.tickers()), Set.copyOf(filters.fiscalPeriods()),
                Set.copyOf(filters.docTypes()));
    }

    private static String f(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
