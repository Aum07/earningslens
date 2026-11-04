package com.earningslens.eval;

import com.earningslens.eval.JudgeVerdict.Pair;
import com.earningslens.generation.LlmReply;
import com.earningslens.generation.PromptBuilder;
import com.earningslens.retrieval.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * The LLM judge (spec 11.2, roadmap E5.1): scores one answered question for faithfulness, correctness
 * and citation precision, following eval/judge-rubric.md.
 *
 * One call per answer. The system message is the rubric's "## Judge prompt" section; the user message holds
 * the question, the gold answer, the answer, the sources the answer cites (only those) and the
 * sentence/marker pairs to check. The reply is validated by {@link JudgeResponseParser}; if it fails, the
 * judge is called again with the error appended, up to eval.judge.max-attempts calls in total.
 */
@Component
public class JudgeClient {

    static final String PROMPT_HEADING = "## Judge prompt";

    private static final Logger log = LoggerFactory.getLogger(JudgeClient.class);

    private final JudgeLlm llm;
    private final int maxAttempts;
    private final Supplier<String> rubric;
    private String systemPrompt;   // read on first use, so the app starts even where the rubric file is absent (prod)

    /**
     * The constructor Spring uses. @Autowired is required because this class has two constructors
     * (lesson from E4.6: without it Spring looks for a no-argument constructor and fails).
     */
    @Autowired
    public JudgeClient(JudgeLlm llm, JudgeProperties judge) {
        this(llm, judge.maxAttempts(), () -> readFile(Path.of(judge.rubricPath())));
    }

    /** For tests: the rubric text is given directly. */
    JudgeClient(JudgeLlm llm, int maxAttempts, Supplier<String> rubric) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("max attempts must be at least 1");
        }
        this.llm = llm;
        this.maxAttempts = maxAttempts;
        this.rubric = rubric;
    }

    /**
     * @throws JudgeOutputException if no attempt produced a valid reply
     * @throws RuntimeException     if the judge model call itself fails (network, HTTP error)
     */
    public JudgeVerdict judge(JudgeInput input) {
        String system = systemPrompt();
        String user = userMessage(input);
        List<Integer> cited = input.citedMarkers();
        List<Pair> pairs = input.pairs();

        int promptTokens = 0;
        int completionTokens = 0;
        boolean tokensKnown = true;
        String model = null;
        String message = user;
        JudgeOutputException last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            LlmReply reply = llm.complete(system, message, JudgeResponseParser.SCHEMA);
            model = reply.model();
            if (reply.promptTokens() == null || reply.completionTokens() == null) {
                tokensKnown = false;
            } else {
                promptTokens += reply.promptTokens();
                completionTokens += reply.completionTokens();
            }
            try {
                if (reply.truncated()) {
                    throw new JudgeOutputException("the reply was cut off at the output token limit");
                }
                JudgeResponseParser.Parsed parsed = JudgeResponseParser.parse(reply.text(), cited, pairs);
                return new JudgeVerdict(parsed.claims(), parsed.citations(), parsed.correctness(),
                        parsed.correctnessReason(), model, tokensKnown ? promptTokens : null,
                        tokensKnown ? completionTokens : null, attempt);
            } catch (JudgeOutputException e) {
                last = e;
                log.warn("Judge reply rejected (attempt {} of {}): {}", attempt, maxAttempts, e.getMessage());
                message = user + "\n\nYOUR PREVIOUS REPLY WAS REJECTED: " + e.getMessage()
                        + "\nReply again with corrected JSON.";
            }
        }
        throw new JudgeOutputException("No valid judge reply after " + maxAttempts + " attempts; last problem: "
                + last.getMessage(), last);
    }

    /** The user message: question, reference, answer, cited sources, sentences and the pairs to check. */
    static String userMessage(JudgeInput input) {
        StringBuilder user = new StringBuilder();
        user.append("QUESTION:\n").append(input.question().strip()).append("\n\n");
        user.append("REFERENCE ANSWER:\n").append(input.goldAnswer().strip()).append("\n\n");
        user.append("ANSWER:\n").append(input.answerText().strip()).append("\n\n");

        user.append("SOURCES:\n");
        List<Integer> cited = input.citedMarkers();
        if (cited.isEmpty()) {
            user.append("(the answer cites no sources)\n\n");
        }
        for (int marker : cited) {
            RetrievedChunk chunk = input.sources().get(marker - 1);
            user.append('[').append(marker).append("] (").append(PromptBuilder.label(chunk)).append(")\n")
                    .append(PromptBuilder.body(chunk)).append("\n\n");
        }

        user.append("SENTENCES:\n");
        if (input.sentences().isEmpty()) {
            user.append("(none)\n");
        }
        for (int i = 0; i < input.sentences().size(); i++) {
            var sentence = input.sentences().get(i);
            user.append('S').append(i + 1).append(' ').append(markers(sentence.markers())).append(": ")
                    .append(sentence.text().strip()).append('\n');
        }

        user.append("\nPAIRS TO JUDGE (").append(input.pairs().size()).append("):\n");
        if (input.pairs().isEmpty()) {
            user.append("(none: return an empty citations list)\n");
        }
        for (Pair pair : input.pairs()) {
            user.append('S').append(pair.sentenceId()).append(" [").append(pair.marker()).append("]\n");
        }
        return user.toString();
    }

    private static String markers(List<Integer> markers) {
        if (markers.isEmpty()) {
            return "(no citation)";
        }
        StringBuilder out = new StringBuilder();
        markers.forEach(m -> out.append('[').append(m).append(']'));
        return out.toString();
    }

    synchronized String systemPrompt() {
        if (systemPrompt == null) {
            systemPrompt = extractPrompt(rubric.get());
        }
        return systemPrompt;
    }

    /** Everything after the "## Judge prompt" heading line. */
    static String extractPrompt(String rubricFile) {
        String normalized = rubricFile.replace("\r\n", "\n");
        int heading = normalized.indexOf("\n" + PROMPT_HEADING + "\n");
        if (heading < 0) {
            throw new IllegalStateException("The rubric file has no line '" + PROMPT_HEADING + "'");
        }
        String prompt = normalized.substring(heading + PROMPT_HEADING.length() + 2).strip();
        if (prompt.isEmpty()) {
            throw new IllegalStateException("The rubric's '" + PROMPT_HEADING + "' section is empty");
        }
        return prompt;
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the judge rubric at " + path.toAbsolutePath(), e);
        }
    }
}
