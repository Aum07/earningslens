package com.earningslens.eval;

import com.earningslens.config.ChunkingProperties;
import com.earningslens.config.LlmProperties;
import com.earningslens.config.RetrievalProperties;
import com.earningslens.generation.Answer;
import com.earningslens.generation.AnswerService;
import com.earningslens.generation.CitationCheck;
import com.earningslens.generation.CitationCheck.CitedSentence;
import com.earningslens.generation.CitationValidator;
import com.earningslens.generation.PromptBuilder;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.RetrievedChunk;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Roadmap E5.2 step 1 (--eval.task=label-prep): answers every dev question with config C and config A,
 * keeps up to eval.label-count answers (all C answers first, then A answers that differ from C's, weakest
 * categories first), and writes:
 * - eval/human-labels-answers.json: the saved answers with their sources (the judge scores these in step 2);
 * - eval/human-labels-review.md: one section per answer with the cited source text, for labelling by hand;
 * - eval/human-labels.jsonl: one blank line per answer to fill in.
 *
 * No judge calls here, so the person labels blind. Refuses to run if human-labels.jsonl already holds any
 * label, so finished work is never overwritten. Paid calls: embeddings and the answer model.
 */
@Component
@Profile("eval")
public class LabelPrepRun {

    /** Configs used: C is the system under test, A (vector only) adds weaker answers so both labels occur. */
    static final List<RetrievalMode> CONFIGS = List.of(RetrievalMode.C, RetrievalMode.A);

    private static final Logger log = LoggerFactory.getLogger(LabelPrepRun.class);

    private final EvalProperties eval;
    private final EvalDatasetLoader loader;
    private final Retriever retriever;
    private final AnswerService answers;
    private final CitationValidator citations;
    private final LlmProperties llm;
    private final RetrievalProperties retrieval;
    private final ChunkingProperties chunking;
    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    public LabelPrepRun(EvalProperties eval, EvalDatasetLoader loader, Retriever retriever, AnswerService answers,
                        CitationValidator citations, LlmProperties llm, RetrievalProperties retrieval,
                        ChunkingProperties chunking) {
        this.eval = eval;
        this.loader = loader;
        this.retriever = retriever;
        this.answers = answers;
        this.citations = citations;
        this.llm = llm;
        this.retrieval = retrieval;
        this.chunking = chunking;
    }

    public void run(EvalSplit split) throws IOException {
        if (split != EvalSplit.DEV) {
            throw new IllegalArgumentException("label-prep runs on the dev split only");
        }
        Path labelsPath = Path.of(eval.humanLabelsPath());
        if (HumanLabels.hasWork(labelsPath)) {
            throw new IllegalStateException(labelsPath + " already has labels in it. label-prep would replace the "
                    + "answers they belong to. Move the file away first if you really want new answers.");
        }
        List<EvalItem> items = loader.load(Path.of(eval.datasetPath()), split);

        int calls = items.size() * CONFIGS.size();
        int maxChunks = Math.max(retrieval.k(), retrieval.kPerPeriod() * retrieval.maxPeriods());
        long maxIn = (long) maxChunks * chunking.maxTokens() + 1_000;
        double worstCase = calls * (maxIn * eval.chatInputPricePerMillionTokens()
                + llm.maxOutputTokens() * eval.chatOutputPricePerMillionTokens()) / 1_000_000.0;
        log.info("Label prep: {} dev items x configs {} = at most {} answer-model calls ({}). Worst-case cost about "
                        + "${} (typical about a third of that). No judge calls.", items.size(), CONFIGS, calls,
                llm.chatModel(), String.format(Locale.ROOT, "%.3f", worstCase));
        if (!eval.confirm()) {
            log.warn("Nothing was sent to OpenAI. Re-run with --eval.confirm=true to generate the answers.");
            return;
        }

        Map<RetrievalMode, List<LabelSample>> byConfig = new HashMap<>();
        double actual = 0.0;
        for (RetrievalMode mode : CONFIGS) {
            List<LabelSample> answered = new ArrayList<>();
            for (EvalItem item : items) {
                RetrievalResult retrieved = retriever.retrieve(item.question(), EvalRunner.toFilters(item.filters()), mode);
                Answer answer = answers.answer(item.question(), retrieved);
                actual += ((answer.promptTokens() == null ? 0 : answer.promptTokens()) * eval.chatInputPricePerMillionTokens()
                        + (answer.completionTokens() == null ? 0 : answer.completionTokens())
                        * eval.chatOutputPricePerMillionTokens()) / 1_000_000.0;
                if (answer.abstained()) {
                    log.info("{} config {}: abstained ({}), not labelled", item.id(), mode, answer.abstainReason());
                    continue;
                }
                CitationCheck check = citations.validate(answer.text(), answer.sources().size());
                answered.add(new LabelSample(LabelSample.labelId(item.id(), mode.name()), item.id(), mode.name(),
                        item.category().jsonName(), item.question(), item.goldAnswer(), check.text(), check.sentences(),
                        answer.sources(), answer.model(), answer.promptTokens(), answer.completionTokens()));
            }
            log.info("Config {}: {} of {} questions answered", mode, answered.size(), items.size());
            byConfig.put(mode, answered);
        }

        List<LabelSample> chosen = select(byConfig.get(RetrievalMode.C), byConfig.get(RetrievalMode.A),
                eval.labelCount());
        if (chosen.size() < eval.labelCount()) {
            log.warn("Only {} distinct answers were available (wanted {})", chosen.size(), eval.labelCount());
        }

        Path answersPath = Path.of(eval.labelAnswersPath());
        Path reviewPath = Path.of(eval.labelReviewPath());
        Files.createDirectories(answersPath.toAbsolutePath().getParent());
        Files.writeString(answersPath, mapper.writeValueAsString(chosen) + "\n", StandardCharsets.UTF_8);
        Files.writeString(reviewPath, reviewSheet(chosen), StandardCharsets.UTF_8);
        HumanLabels.writeTemplate(labelsPath, chosen);

        long fromC = chosen.stream().filter(s -> s.config().equals("C")).count();
        log.info("Saved {} answers ({} from C, {} from A). Actual cost about ${}.", chosen.size(), fromC,
                chosen.size() - fromC, String.format(Locale.ROOT, "%.4f", actual));
        log.info("Next: read {} and fill in {}, then run --eval.task=judge-agreement.", reviewPath, labelsPath);
    }

    /**
     * Every C answer (in item order), then A answers, skipping an A answer whose text is the same as C's for that
     * item, until {@code count} answers are chosen. A answers are taken in {@link #A_PRIORITY} order (then item
     * order): the weaker answers first, because they are the ones that test the judge.
     */
    static List<LabelSample> select(List<LabelSample> fromC, List<LabelSample> fromA, int count) {
        List<LabelSample> chosen = new ArrayList<>();
        Map<String, String> cText = new HashMap<>();
        for (LabelSample sample : fromC) {
            if (chosen.size() < count) {
                chosen.add(sample);
            }
            cText.put(sample.itemId(), normalize(sample.answer()));
        }
        List<LabelSample> aInPriority = fromA.stream()
                .sorted(java.util.Comparator.comparingInt((LabelSample s) -> priority(s.category()))
                        .thenComparing(LabelSample::itemId))
                .toList();
        for (LabelSample sample : aInPriority) {
            if (chosen.size() >= count) {
                break;
            }
            if (!normalize(sample.answer()).equals(cText.get(sample.itemId()))) {
                chosen.add(sample);
            }
        }
        return List.copyOf(chosen);
    }

    /**
     * Order for A answers: answers to unanswerable questions first (the system should have declined), then
     * multi_quarter (A's weakest retrieval in Phase 3), qualitative, single_fact. Fixed 2026-11-04 after the
     * first run picked q001-A to q007-A in item order, all easy single facts.
     */
    static final List<String> A_PRIORITY = List.of("unanswerable", "multi_quarter", "qualitative", "single_fact");

    private static int priority(String category) {
        int index = A_PRIORITY.indexOf(category);
        return index < 0 ? A_PRIORITY.size() : index;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    /** The Markdown sheet the person labels from: no judge scores, only what the judge will also see. */
    static String reviewSheet(List<LabelSample> samples) {
        StringBuilder md = new StringBuilder();
        md.append("# Answers to label by hand (E5.2)\n\n");
        md.append("Fill in `eval/human-labels.jsonl`, one line per answer below, matching `label_id`.\n\n");
        md.append("- `faithful`: `true` if every factual claim in the answer is stated by, or follows by simple ")
                .append("arithmetic from, the cited sources shown (any of them). `false` if even one claim is not. ")
                .append("Ignore whether a marker sits on the right sentence.\n");
        md.append("- `correctness`: 1 to 5 against the gold answer, using task 3 of `eval/judge-rubric.md` ")
                .append("(5 all key facts right; 4 minor detail missing; 3 partly right; 2 mostly wrong; 1 wrong or ")
                .append("a false \"not available\"). If the gold answer says it is not in the documents: 5 if the ")
                .append("answer declines, 1 if it gives a figure as the answer.\n");
        md.append("- `note`: optional, for example which claim is unsupported.\n\n");
        md.append("Write `true`/`false` and numbers without quotes. Do not look at judge output before you finish.\n");

        for (int i = 0; i < samples.size(); i++) {
            LabelSample s = samples.get(i);
            md.append("\n---\n\n## ").append(i + 1).append(". ").append(s.labelId()).append(" (").append(s.category())
                    .append(")\n\n");
            md.append("**Question:** ").append(s.question().strip()).append("\n\n");
            md.append("**Gold answer:** ").append(s.goldAnswer().strip()).append("\n\n");
            md.append("**Answer:**\n\n");
            for (String line : s.answer().strip().split("\n")) {
                md.append("> ").append(line).append('\n');
            }
            TreeSet<Integer> cited = new TreeSet<>();
            for (CitedSentence sentence : s.sentences()) {
                cited.addAll(sentence.markers());
            }
            md.append("\n**Cited sources (").append(cited.size()).append("):**\n");
            if (cited.isEmpty()) {
                md.append("\n(none: the answer cites no sources)\n");
            }
            for (int marker : cited) {
                RetrievedChunk chunk = s.sources().get(marker - 1);
                md.append("\n[").append(marker).append("] ").append(PromptBuilder.label(chunk)).append("\n\n```text\n")
                        .append(PromptBuilder.body(chunk).replace("```", "'''")).append("\n```\n");
            }
        }
        return md.toString();
    }
}
