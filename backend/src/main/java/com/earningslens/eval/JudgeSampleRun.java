package com.earningslens.eval;

import com.earningslens.config.ChunkingProperties;
import com.earningslens.config.LlmProperties;
import com.earningslens.config.RetrievalProperties;
import com.earningslens.generation.Answer;
import com.earningslens.generation.AnswerService;
import com.earningslens.generation.CitationCheck;
import com.earningslens.generation.CitationCheck.CitedSentence;
import com.earningslens.generation.CitationValidator;
import com.earningslens.ingest.TokenCounter;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Roadmap E5.1 smoke run (--eval.task=judge-sample): answers a few dev questions with config C, judges each
 * answer, logs the scores and writes eval/reports/{date}-judge-sample-dev.json.
 * Done when: the judge's output parses for every answer.
 *
 * Paid calls: question embeddings, the answer model (gpt-4o-mini) and the judge model. The worst-case cost
 * is printed first and nothing is sent unless --eval.confirm=true. Dev split only.
 */
@Component
@Profile("eval")
public class JudgeSampleRun {

    private static final Logger log = LoggerFactory.getLogger(JudgeSampleRun.class);

    private final EvalProperties eval;
    private final JudgeProperties judgeSettings;
    private final EvalDatasetLoader loader;
    private final Retriever retriever;
    private final AnswerService answers;
    private final CitationValidator citations;
    private final JudgeClient judge;
    private final LlmProperties llm;
    private final RetrievalProperties retrieval;
    private final ChunkingProperties chunking;
    private final TokenCounter tokens;
    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    public JudgeSampleRun(EvalProperties eval, JudgeProperties judgeSettings, EvalDatasetLoader loader,
                          Retriever retriever, AnswerService answers, CitationValidator citations, JudgeClient judge,
                          LlmProperties llm, RetrievalProperties retrieval, ChunkingProperties chunking,
                          TokenCounter tokens) {
        this.eval = eval;
        this.judgeSettings = judgeSettings;
        this.loader = loader;
        this.retriever = retriever;
        this.answers = answers;
        this.citations = citations;
        this.judge = judge;
        this.llm = llm;
        this.retrieval = retrieval;
        this.chunking = chunking;
        this.tokens = tokens;
    }

    /** One judged (or skipped) item in the report. */
    record Row(String id, String category, String question, String goldAnswer, String answer, boolean abstained,
               String abstainReason, List<CitedSentence> sentences, List<String> citationWarnings,
               Integer answerPromptTokens, Integer answerCompletionTokens, JudgeVerdict verdict,
               Double faithfulness, Double citationPrecision) {
    }

    record Report(String date, String split, String config, String answerModel, String judgeModel,
                  int items, int judged, int parsed, double worstCaseUsd, double actualUsd, List<Row> rows) {
    }

    public void run(EvalSplit split) throws IOException {
        if (split != EvalSplit.DEV) {
            throw new IllegalArgumentException("judge-sample runs on the dev split only");
        }
        List<EvalItem> items = pick(loader.load(Path.of(eval.datasetPath()), split), judgeSettings.sampleIds());

        double worstCase = worstCaseUsd(items.size());
        log.info("Judge sample: {} dev items {} with config C. Answer model {}, judge model {} (up to {} calls each). "
                        + "Worst-case cost about ${} (typical is far lower: only cited sources go to the judge).",
                items.size(), items.stream().map(EvalItem::id).toList(), llm.chatModel(), judgeSettings.model(),
                judgeSettings.maxAttempts(), String.format(Locale.ROOT, "%.3f", worstCase));
        if (!eval.confirm()) {
            log.warn("Nothing was sent to OpenAI. Re-run with --eval.confirm=true to run the judge sample.");
            return;
        }

        List<Row> rows = new ArrayList<>();
        double actual = 0.0;
        int judged = 0;
        for (EvalItem item : items) {
            RetrievalResult retrieved = retriever.retrieve(item.question(), EvalRunner.toFilters(item.filters()),
                    RetrievalMode.C);
            Answer answer = answers.answer(item.question(), retrieved);
            actual += chatCost(answer.promptTokens(), answer.completionTokens());
            if (answer.abstained()) {
                log.warn("{}: the system abstained ({}), so there is nothing to judge. Pick another id with "
                        + "--eval.judge.sample-ids.", item.id(), answer.abstainReason());
                rows.add(new Row(item.id(), item.category().jsonName(), item.question(), item.goldAnswer(),
                        answer.text(), true, String.valueOf(answer.abstainReason()), List.of(), List.of(),
                        answer.promptTokens(), answer.completionTokens(), null, null, null));
                continue;
            }
            CitationCheck check = citations.validate(answer.text(), answer.sources().size());
            JudgeVerdict verdict = judge.judge(new JudgeInput(item.question(), item.goldAnswer(), check.text(),
                    answer.sources(), check.sentences()));
            judged++;
            actual += judgeCost(verdict.promptTokens(), verdict.completionTokens());

            log.info("{} ({}): faithfulness {}/{} = {}, citation precision {}, correctness {} ({}); judge attempts {}",
                    item.id(), item.category().jsonName(), verdict.supportedClaims(), verdict.claims().size(),
                    f(verdict.faithfulness()),
                    verdict.citationPrecision() == null ? "- (no citations)"
                            : verdict.supportingCitations() + "/" + verdict.citations().size() + " = "
                            + f(verdict.citationPrecision()),
                    verdict.correctness(), verdict.correctnessReason(), verdict.attempts());
            rows.add(new Row(item.id(), item.category().jsonName(), item.question(), item.goldAnswer(), check.text(),
                    false, null, check.sentences(), check.warnings(), answer.promptTokens(),
                    answer.completionTokens(), verdict, verdict.faithfulness(), verdict.citationPrecision()));
        }

        // Every judged answer has a parsed verdict here: an unparseable reply throws and stops the run.
        Report report = new Report(LocalDate.now().toString(), split.jsonName(), "C", llm.chatModel(),
                judgeSettings.model(), items.size(), judged, judged, round(worstCase), round(actual), rows);
        Path dir = Path.of(eval.reportsDir());
        Files.createDirectories(dir);
        Path file = dir.resolve(LocalDate.now() + "-judge-sample-dev.json");
        Files.writeString(file, mapper.writeValueAsString(report) + "\n", StandardCharsets.UTF_8);
        log.info("Judge output parsed for {}/{} judged answers ({} abstained). Actual cost about ${}. Report: {}",
                judged, judged, items.size() - judged, String.format(Locale.ROOT, "%.4f", actual), file);
    }

    /** The items named in sampleIds, in that order; every id must exist in the split. */
    static List<EvalItem> pick(List<EvalItem> items, String sampleIds) {
        Map<String, EvalItem> byId = new LinkedHashMap<>();
        items.forEach(item -> byId.put(item.id(), item));
        List<EvalItem> picked = new ArrayList<>();
        for (String part : sampleIds.split(",")) {
            String id = part.strip();
            if (id.isEmpty()) {
                continue;
            }
            EvalItem item = byId.get(id);
            if (item == null) {
                throw new IllegalArgumentException("sample id '" + id + "' is not in this split");
            }
            if (!picked.contains(item)) {
                picked.add(item);
            }
        }
        if (picked.isEmpty()) {
            throw new IllegalArgumentException("eval.judge.sample-ids is empty");
        }
        return picked;
    }

    /**
     * Upper bound: the most chunks retrieval can return at the largest chunk size, the full answer limit,
     * and every judge attempt seeing all of that plus the rubric and using its whole output limit.
     */
    double worstCaseUsd(int itemCount) {
        int maxChunks = Math.max(retrieval.k(), retrieval.kPerPeriod() * retrieval.maxPeriods());
        long answerIn = (long) maxChunks * chunking.maxTokens() + 1_000;
        long answerOut = llm.maxOutputTokens();
        long judgeIn = tokens.count(judge.systemPrompt()) + answerIn + answerOut + 1_000;
        long judgeOut = judgeSettings.maxOutputTokens();
        double perItem = chatCost((int) answerIn, (int) answerOut)
                + judgeSettings.maxAttempts() * judgeCost((int) judgeIn, (int) judgeOut);
        return perItem * itemCount;
    }

    private double chatCost(Integer in, Integer out) {
        return cost(in, out, eval.chatInputPricePerMillionTokens(), eval.chatOutputPricePerMillionTokens());
    }

    private double judgeCost(Integer in, Integer out) {
        return cost(in, out, judgeSettings.inputPricePerMillionTokens(), judgeSettings.outputPricePerMillionTokens());
    }

    private static double cost(Integer in, Integer out, double inPrice, double outPrice) {
        return ((in == null ? 0 : in) * inPrice + (out == null ? 0 : out) * outPrice) / 1_000_000.0;
    }

    private static double round(double usd) {
        return Math.round(usd * 1_000_000.0) / 1_000_000.0;
    }

    private static String f(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
