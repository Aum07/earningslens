package com.earningslens.eval;

import com.earningslens.config.ChunkingProperties;
import com.earningslens.config.LlmProperties;
import com.earningslens.config.RetrievalProperties;
import com.earningslens.generation.Answer;
import com.earningslens.generation.AnswerService;
import com.earningslens.generation.CitationCheck;
import com.earningslens.generation.CitationValidator;
import com.earningslens.generation.GroundingCheck;
import com.earningslens.generation.NumberGroundingCheck;
import com.earningslens.ingest.TokenCounter;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Roadmap E5.3 (--eval.task=full): runs every item of the split through retrieval, the answer model, the citation
 * and number checks and the judge, for each config, and writes a full report per config (spec 11.2, 11.5).
 *
 * Exactly what a user gets: the same Retriever, AnswerService, CitationValidator and NumberGroundingCheck as the
 * chat API. Latency is measured the same way as ChatService (retrieval + answer, not the judge).
 *
 * Paid calls: embeddings, the answer model and the judge. The cost estimate is printed first and nothing is sent
 * without --eval.confirm=true. The test split needs --eval.final-test-run=true as well (EvalRunner.checkAllowed),
 * all four configs, and no earlier test report (checkFinalTestRun). Only that run writes eval/reports/latest.json:
 * the UI only shows the final test-split numbers.
 */
@Component
@Profile("eval")
public class FullEvalRun {

    /** Typical sizes measured in E5.2 (answer prompt about 9k tokens; judge about 3k in, 600 out). */
    static final int TYPICAL_ANSWER_IN = 9_000;
    static final int TYPICAL_ANSWER_OUT = 200;
    static final int TYPICAL_JUDGE_IN = 3_000;
    static final int TYPICAL_JUDGE_OUT = 600;

    private static final Logger log = LoggerFactory.getLogger(FullEvalRun.class);

    private final EvalProperties eval;
    private final JudgeProperties judgeSettings;
    private final EvalDatasetLoader loader;
    private final Retriever retriever;
    private final AnswerService answers;
    private final CitationValidator citations;
    private final NumberGroundingCheck grounding;
    private final JudgeClient judge;
    private final LlmProperties llm;
    private final RetrievalProperties retrieval;
    private final ChunkingProperties chunking;
    private final TokenCounter tokens;
    private final JdbcTemplate jdbc;
    private final FullReportWriter writer;

    public FullEvalRun(EvalProperties eval, JudgeProperties judgeSettings, EvalDatasetLoader loader,
                       Retriever retriever, AnswerService answers, CitationValidator citations,
                       NumberGroundingCheck grounding, JudgeClient judge, LlmProperties llm,
                       RetrievalProperties retrieval, ChunkingProperties chunking, TokenCounter tokens,
                       JdbcTemplate jdbc, FullReportWriter writer) {
        this.eval = eval;
        this.judgeSettings = judgeSettings;
        this.loader = loader;
        this.retriever = retriever;
        this.answers = answers;
        this.citations = citations;
        this.grounding = grounding;
        this.judge = judge;
        this.llm = llm;
        this.retrieval = retrieval;
        this.chunking = chunking;
        this.tokens = tokens;
        this.jdbc = jdbc;
        this.writer = writer;
    }

    public void run(EvalSplit split, List<RetrievalMode> modes) throws IOException {
        if (split == EvalSplit.TEST) {
            checkFinalTestRun(modes, Path.of(eval.reportsDir()));
            log.warn("FINAL TEST-SPLIT RUN (E5.4): this runs once. Settings are frozen; do not tune after it.");
        }
        List<EvalItem> items = loader.load(Path.of(eval.datasetPath()), split);
        Integer corpusChunks = jdbc.queryForObject("SELECT count(*) FROM chunks", Integer.class);
        if (corpusChunks == null || corpusChunks == 0) {
            throw new IllegalStateException("The chunks table is empty: run ingestion first (docker compose up -d?)");
        }

        double[] estimate = estimate(items.size() * modes.size());
        log.info("Full eval: {} {} items x configs {} = {} queries. Answer model {}, judge {}. Cost about ${} "
                        + "(worst case ${}).", items.size(), split.jsonName(), modes, items.size() * modes.size(),
                llm.chatModel(), judgeSettings.model(), usd(estimate[0]), usd(estimate[1]));
        if (!eval.confirm()) {
            log.warn("Nothing was sent to OpenAI. Re-run with --eval.confirm=true to run the full eval.");
            return;
        }

        String system = judge.systemPrompt();
        FullReportWriter.Settings settings = new FullReportWriter.Settings(retrieval.k(), retrieval.kPerPeriod(),
                retrieval.maxPeriods(), retrieval.minSimilarity(), chunking.targetTokens(), chunking.overlapTokens(),
                corpusChunks, llm.chatModel(), llm.embeddingModel(), judgeSettings.model(),
                JudgeAgreementRun.sha256(system));
        String date = LocalDate.now().toString();
        Path dir = Path.of(eval.reportsDir());

        List<FullReportWriter.Report> reports = new ArrayList<>();
        for (RetrievalMode mode : modes) {
            List<FullEvalMetrics.ItemResult> results = new ArrayList<>();
            for (EvalItem item : items) {
                FullEvalMetrics.ItemResult result = runItem(item, mode);
                results.add(result);
                log.info("{} {}: {}", mode, item.id(), describe(result));
            }
            FullEvalMetrics.Summary summary = FullEvalMetrics.summarize(mode.name(), results);
            FullReportWriter.Report report = new FullReportWriter.Report(date, split.jsonName(), mode.name(),
                    eval.label(), settings, summary, results);
            reports.add(report);
            log.info("Config {}: {}", mode, headline(summary));
            log.info("Report: {}", writer.write(report, dir));
        }
        if (reports.size() > 1) {
            log.info("Ablation table: {}", writer.writeAblation(reports, dir));
        }
        if (split == EvalSplit.TEST) {
            Path agreement = Path.of(eval.judgeAgreementReport());
            if (!java.nio.file.Files.isRegularFile(agreement)) {
                log.warn("Judge agreement report {} not found: latest.json will have no judge agreement", agreement);
            }
            log.info("latest.json: {}", writer.writeLatest(reports, dir, agreement));
        }
        double spent = reports.stream().mapToDouble(r -> r.summary().operations().totalCostUsd()
                + r.summary().operations().judgeCostUsd()).sum();
        log.info("Actual cost about ${} (answers, embeddings and judge).", usd(spent));
    }

    /**
     * The test split runs once (spec 11.1, roadmap E5.4): all four configs together, and only if no test report or
     * latest.json exists yet. A second run must be a deliberate, clearly labelled decision, not an accident.
     */
    static void checkFinalTestRun(List<RetrievalMode> modes, Path reportsDir) throws IOException {
        if (modes.size() != 4 || !modes.containsAll(List.of(RetrievalMode.values()))) {
            throw new IllegalArgumentException("The final test run must include all configs A, B, C and D "
                    + "(--eval.config=A,B,C,D), got " + modes);
        }
        if (!java.nio.file.Files.isDirectory(reportsDir)) {
            return;
        }
        try (var files = java.nio.file.Files.list(reportsDir)) {
            List<String> existing = files.map(p -> p.getFileName().toString())
                    .filter(name -> name.equals(FullReportWriter.LATEST)
                            || name.matches("\\d{4}-\\d{2}-\\d{2}-[ABCD]-test.*\\.json"))
                    .sorted().toList();
            if (!existing.isEmpty()) {
                throw new IllegalStateException("The test split has already been run (found " + existing
                        + "). It is run once; do not re-tune or re-run it.");
            }
        }
    }

    private FullEvalMetrics.ItemResult runItem(EvalItem item, RetrievalMode mode) {
        String category = item.category().jsonName();
        long start = System.nanoTime();
        RetrievalResult retrieved;
        Answer answer;
        try {
            retrieved = retriever.retrieve(item.question(), EvalRunner.toFilters(item.filters()), mode);
            answer = answers.answer(item.question(), retrieved);
        } catch (RuntimeException e) {
            log.warn("{} {}: left out, {}", mode, item.id(), e.toString());
            return new FullEvalMetrics.ItemResult(item.id(), category, item.answerable(), false, null, null, null,
                    null, null, null, null, null, null, null, null, 0, 0, 0, null, e.toString());
        }
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        RetrievalMetrics.ItemScore score = RetrievalMetrics.score(item, retrieved.chunks());
        double cost = queryCost(item, retrieved, answer);

        if (answer.abstained()) {
            return new FullEvalMetrics.ItemResult(item.id(), category, item.answerable(), true,
                    String.valueOf(answer.abstainReason()), score, null, null, null, null, null, null, null,
                    null, null, latencyMs, cost, 0, answer.text(), null);
        }

        CitationCheck check = citations.validate(answer.text(), answer.sources().size());
        GroundingCheck numbers = grounding.check(check, answer.sources());
        JudgeVerdict verdict;
        try {
            verdict = judge.judge(new JudgeInput(item.question(), item.goldAnswer(), check.text(), answer.sources(),
                    check.sentences()));
        } catch (RuntimeException e) {
            log.warn("{} {}: judge failed, left out: {}", mode, item.id(), e.toString());
            return new FullEvalMetrics.ItemResult(item.id(), category, item.answerable(), false, null, score, null,
                    null, null, null, null, null, null, null, null, latencyMs, cost, 0, check.text(),
                    "judge: " + e.getMessage());
        }
        double judgeCost = cost(verdict.promptTokens(), verdict.completionTokens(),
                judgeSettings.inputPricePerMillionTokens(), judgeSettings.outputPricePerMillionTokens());
        List<String> unsupported = verdict.claims().stream().filter(c -> !c.supported())
                .map(c -> c.claim() + " (" + c.reason() + ")").toList();
        return new FullEvalMetrics.ItemResult(item.id(), category, item.answerable(), false, null, score,
                verdict.faithfulness(), verdict.correctness(), verdict.correctnessReason(),
                (int) verdict.supportingCitations(), verdict.citations().size(), (int) numbers.groundedCount(),
                numbers.numbers().size(), unsupported, numbers.warnings(), latencyMs, cost, judgeCost,
                check.text(), null);
    }

    /** Embedding (unless retrieval stopped before embedding) plus the answer model's tokens. */
    private double queryCost(EvalItem item, RetrievalResult retrieved, Answer answer) {
        boolean embedded = retrieved.abstainReason() != RetrievalResult.AbstainReason.PERIOD_NOT_IN_CORPUS;
        double embedding = embedded ? tokens.count(item.question()) * eval.embeddingPricePerMillionTokens() / 1_000_000.0 : 0;
        return embedding + cost(answer.promptTokens(), answer.completionTokens(),
                eval.chatInputPricePerMillionTokens(), eval.chatOutputPricePerMillionTokens());
    }

    /** {typical, worst case} in USD for this many queries; the judge is assumed to run on every one. */
    double[] estimate(int queries) {
        double typical = queries * (cost(TYPICAL_ANSWER_IN, TYPICAL_ANSWER_OUT, eval.chatInputPricePerMillionTokens(),
                eval.chatOutputPricePerMillionTokens()) + cost(TYPICAL_JUDGE_IN, TYPICAL_JUDGE_OUT,
                judgeSettings.inputPricePerMillionTokens(), judgeSettings.outputPricePerMillionTokens()));
        int maxChunks = Math.max(retrieval.k(), retrieval.kPerPeriod() * retrieval.maxPeriods());
        int answerIn = maxChunks * chunking.maxTokens() + 1_000;
        int judgeIn = tokens.count(judge.systemPrompt()) + answerIn + llm.maxOutputTokens() + 1_000;
        double worst = queries * (cost(answerIn, llm.maxOutputTokens(), eval.chatInputPricePerMillionTokens(),
                eval.chatOutputPricePerMillionTokens()) + judgeSettings.maxAttempts() * cost(judgeIn,
                judgeSettings.maxOutputTokens(), judgeSettings.inputPricePerMillionTokens(),
                judgeSettings.outputPricePerMillionTokens()));
        return new double[] {typical, worst};
    }

    private static double cost(Integer in, Integer out, double inPrice, double outPrice) {
        return ((in == null ? 0 : in) * inPrice + (out == null ? 0 : out) * outPrice) / 1_000_000.0;
    }

    private static String describe(FullEvalMetrics.ItemResult r) {
        if (!r.ok()) {
            return "error";
        }
        if (r.abstained()) {
            return "abstained (" + r.abstainReason() + "), " + r.latencyMs() + " ms";
        }
        return String.format(Locale.ROOT, "faithfulness %.2f, correctness %d, citations %d/%d, numbers %d/%d, %d ms",
                r.judgeFaithfulness(), r.judgeCorrectness(), r.supportingCitations(), r.citationPairs(),
                r.groundedNumbers(), r.numbers(), r.latencyMs());
    }

    private static String headline(FullEvalMetrics.Summary s) {
        return String.format(Locale.ROOT, "recall@k all %s, faithfulness %s, correctness %s, citation precision %s, "
                        + "number grounding %s, abstention precision %s / recall %s, false abstention %s, "
                        + "p50 %d ms, p95 %d ms, $%s per query, %d errors",
                FullReportWriter.f(s.retrieval().recallAll()), FullReportWriter.f(s.generation().faithfulness()),
                FullReportWriter.f(s.generation().correctness()), FullReportWriter.pct(s.generation().citationPrecision()),
                FullReportWriter.pct(s.generation().numberGroundingRate()), FullReportWriter.pct(s.abstention().precision()),
                FullReportWriter.pct(s.abstention().recall()), FullReportWriter.pct(s.abstention().falseAbstentionRate()),
                s.operations().latencyP50Ms(), s.operations().latencyP95Ms(), usd(s.operations().meanCostUsd()),
                s.errors());
    }

    private static String usd(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
