package com.earningslens.generation;

import com.earningslens.generation.Answer.AbstainReason;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.PeriodTarget;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns retrieved chunks into an answer (spec 9, roadmap E4.2).
 *
 * 1. If retrieval already abstained, or found nothing, the model is not called (no cost).
 * 2. Otherwise the prompt is built (PromptBuilder) and sent once (temperature 0, max 700 output tokens).
 * 3. A reply of INSUFFICIENT_EVIDENCE becomes an abstention with a friendly message listing what was searched.
 * 4. Any model failure, or an empty reply, becomes {@link LlmUnavailableException} (503 in the API).
 *
 * Token counts are kept on the Answer so the API can log them to query_log and the eval can report cost.
 */
@Component
public class AnswerService {

    static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final PromptBuilder promptBuilder;
    private final ChatLlm llm;

    public AnswerService(PromptBuilder promptBuilder, ChatLlm llm) {
        this.promptBuilder = promptBuilder;
        this.llm = llm;
    }

    public Answer answer(String question, RetrievalResult retrieval) {
        if (retrieval.abstained()) {
            AbstainReason reason = switch (retrieval.abstainReason()) {
                case PERIOD_NOT_IN_CORPUS -> AbstainReason.PERIOD_NOT_IN_CORPUS;
                case LOW_SIMILARITY -> AbstainReason.LOW_SIMILARITY;
            };
            return abstainWithoutModel(reason, retrieval);
        }
        if (retrieval.chunks().isEmpty()) {
            return abstainWithoutModel(AbstainReason.NO_SOURCES, retrieval);
        }

        List<RetrievedChunk> sources = retrieval.chunks().stream().map(FusedHit::chunk).toList();
        Prompt prompt = promptBuilder.build(question, sources);

        LlmReply reply;
        try {
            reply = llm.complete(prompt.system(), prompt.user());
        } catch (RuntimeException e) {
            log.warn("Chat model call failed: {}", e.toString());
            throw new LlmUnavailableException("The language model is unavailable right now", e);
        }
        if (reply == null || reply.text() == null || reply.text().isBlank()) {
            throw new LlmUnavailableException("The language model returned an empty reply");
        }

        log.info("Chat model {} used {} prompt + {} completion tokens for {} sources{}",
                reply.model(), reply.promptTokens(), reply.completionTokens(), sources.size(),
                reply.truncated() ? " (answer cut off at the output limit)" : "");

        if (isInsufficientEvidence(reply.text())) {
            return new Answer(abstentionMessage(AbstainReason.INSUFFICIENT_EVIDENCE, retrieval), true,
                    AbstainReason.INSUFFICIENT_EVIDENCE, sources, reply.model(), reply.promptTokens(),
                    reply.completionTokens(), reply.truncated());
        }
        return new Answer(reply.text().strip(), false, null, sources, reply.model(), reply.promptTokens(),
                reply.completionTokens(), reply.truncated());
    }

    private static Answer abstainWithoutModel(AbstainReason reason, RetrievalResult retrieval) {
        return new Answer(abstentionMessage(reason, retrieval), true, reason, List.of(), null, null, null, false);
    }

    /**
     * True if the reply is the abstention token: exactly "INSUFFICIENT_EVIDENCE", allowing surrounding
     * whitespace, quotes, backticks, asterisks or a final full stop, or the token followed by more text
     * ("INSUFFICIENT_EVIDENCE\n\nThe sources only cover ..."). The token in the middle of an answer does not count.
     */
    static boolean isInsufficientEvidence(String reply) {
        String text = trimDecoration(reply);
        if (!text.startsWith(INSUFFICIENT_EVIDENCE)) {
            return false;
        }
        if (text.length() == INSUFFICIENT_EVIDENCE.length()) {
            return true;
        }
        char next = text.charAt(INSUFFICIENT_EVIDENCE.length());
        return !Character.isLetterOrDigit(next) && next != '_';
    }

    private static String trimDecoration(String reply) {
        String decoration = "`*\"'. \t\r\n";
        int start = 0;
        int end = reply.length();
        while (start < end && decoration.indexOf(reply.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && decoration.indexOf(reply.charAt(end - 1)) >= 0) {
            end--;
        }
        return reply.substring(start, end);
    }

    /** Spec 9.2: a friendly message listing which companies and periods were searched. */
    static String abstentionMessage(AbstainReason reason, RetrievalResult retrieval) {
        if (reason == AbstainReason.PERIOD_NOT_IN_CORPUS && !retrieval.missing().isEmpty()) {
            return "I can't answer this because the corpus does not include " + join(retrieval.missing())
                    + ". EarningsLens only covers the quarters listed on the Corpus page.";
        }
        StringBuilder message = new StringBuilder(
                "I couldn't find enough evidence in the filings and earnings calls to answer this. ");
        if (retrieval.searchedTargets().isEmpty()) {
            message.append("Searched: all matching documents in the corpus.");
        } else {
            message.append("Searched: ").append(join(retrieval.searchedTargets())).append('.');
        }
        if (!retrieval.missing().isEmpty()) {
            message.append(" Not in the corpus: ").append(join(retrieval.missing())).append('.');
        }
        if (!retrieval.skippedTargets().isEmpty()) {
            message.append(" Not searched (too many quarters in one question): ")
                    .append(join(retrieval.skippedTargets())).append('.');
        }
        return message.toString();
    }

    private static String join(List<PeriodTarget> targets) {
        return targets.stream().distinct().map(PeriodTarget::toString).collect(Collectors.joining(", "));
    }
}
