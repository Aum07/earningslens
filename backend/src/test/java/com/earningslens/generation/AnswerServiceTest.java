package com.earningslens.generation;

import com.earningslens.eval.FiscalPeriod;
import com.earningslens.generation.Answer.AbstainReason;
import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.PeriodTarget;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.RetrievalResult.Strategy;
import com.earningslens.retrieval.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roadmap E4.2: normal answer, abstention and LLM error. The chat model is a fake
 * (a lambda that records what it was sent), so this makes no OpenAI calls and runs in CI.
 */
class AnswerServiceTest {

    private static final PeriodTarget MSFT_Q2 = new PeriodTarget("MSFT", new FiscalPeriod(2026, 2));
    private static final PeriodTarget MSFT_Q3 = new PeriodTarget("MSFT", new FiscalPeriod(2026, 3));

    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final List<String> sentUserMessages = new ArrayList<>();

    private AnswerService serviceReplying(LlmReply reply) {
        return new AnswerService(promptBuilder, (system, user) -> {
            sentUserMessages.add(user);
            return reply;
        });
    }

    @Test
    void normalAnswerKeepsTextSourcesAndTokenCounts() {
        AnswerService service = serviceReplying(
                new LlmReply("  Intelligent Cloud revenue was $32.9 billion [1].\n", "gpt-4o-mini-2024-07-18",
                        1500, 40, false));
        RetrievalResult retrieval = found(List.of(MSFT_Q2), chunk("a"), chunk("b"));

        Answer answer = service.answer("What was Intelligent Cloud revenue?", retrieval);

        assertThat(answer.abstained()).isFalse();
        assertThat(answer.abstainReason()).isNull();
        assertThat(answer.text()).isEqualTo("Intelligent Cloud revenue was $32.9 billion [1].");
        assertThat(answer.sources()).extracting(RetrievedChunk::content).containsExactly("a", "b");
        assertThat(answer.model()).isEqualTo("gpt-4o-mini-2024-07-18");
        assertThat(answer.promptTokens()).isEqualTo(1500);
        assertThat(answer.completionTokens()).isEqualTo(40);
        assertThat(answer.modelCalled()).isTrue();
        assertThat(answer.truncated()).isFalse();
        assertThat(sentUserMessages).hasSize(1);
        assertThat(sentUserMessages.get(0)).startsWith("Sources:\n[1] (").endsWith("Question: What was Intelligent Cloud revenue?");
    }

    @Test
    void modelAbstentionBecomesFriendlyMessageListingWhatWasSearched() {
        AnswerService service = serviceReplying(new LlmReply("INSUFFICIENT_EVIDENCE", "gpt-4o-mini", 900, 4, false));
        RetrievalResult retrieval = found(List.of(MSFT_Q2, MSFT_Q3), chunk("a"));

        Answer answer = service.answer("What was monthly revenue?", retrieval);

        assertThat(answer.abstained()).isTrue();
        assertThat(answer.abstainReason()).isEqualTo(AbstainReason.INSUFFICIENT_EVIDENCE);
        assertThat(answer.text()).isEqualTo("I couldn't find enough evidence in the filings and earnings calls "
                + "to answer this. Searched: MSFT FY2026 Q2, MSFT FY2026 Q3.");
        assertThat(answer.modelCalled()).isTrue();              // the call was made and billed
        assertThat(answer.promptTokens()).isEqualTo(900);
        assertThat(answer.sources()).hasSize(1);
    }

    @Test
    void recognisesTheAbstentionTokenWithSmallFormattingDifferences() {
        assertThat(AnswerService.isInsufficientEvidence("INSUFFICIENT_EVIDENCE")).isTrue();
        assertThat(AnswerService.isInsufficientEvidence("  `INSUFFICIENT_EVIDENCE`.\n")).isTrue();
        assertThat(AnswerService.isInsufficientEvidence("**INSUFFICIENT_EVIDENCE**")).isTrue();
        assertThat(AnswerService.isInsufficientEvidence("INSUFFICIENT_EVIDENCE\n\nThe sources only cover Q2.")).isTrue();

        assertThat(AnswerService.isInsufficientEvidence("Revenue was $10 billion [1].")).isFalse();
        assertThat(AnswerService.isInsufficientEvidence("INSUFFICIENT_EVIDENCES")).isFalse();
        assertThat(AnswerService.isInsufficientEvidence("Revenue grew [1]. INSUFFICIENT_EVIDENCE for margins.")).isFalse();
    }

    @Test
    void retrievalAbstentionDoesNotCallTheModel() {
        AnswerService service = new AnswerService(promptBuilder, (system, user) -> {
            throw new AssertionError("the model must not be called");
        });
        PeriodTarget msftFy2024 = new PeriodTarget("MSFT", new FiscalPeriod(2024, 1));
        RetrievalResult retrieval = new RetrievalResult(RetrievalMode.D, Strategy.NONE, List.of(), List.of(), List.of(),
                List.of(msftFy2024), null, RetrievalResult.AbstainReason.PERIOD_NOT_IN_CORPUS);

        Answer answer = service.answer("What was Microsoft revenue in FY2024 Q1?", retrieval);

        assertThat(answer.abstained()).isTrue();
        assertThat(answer.abstainReason()).isEqualTo(AbstainReason.PERIOD_NOT_IN_CORPUS);
        assertThat(answer.text()).isEqualTo("I can't answer this because the corpus does not include MSFT FY2024 Q1. "
                + "EarningsLens only covers the quarters listed on the Corpus page.");
        assertThat(answer.modelCalled()).isFalse();
        assertThat(answer.promptTokens()).isNull();
        assertThat(answer.sources()).isEmpty();
    }

    @Test
    void lowSimilarityAndNoChunksAlsoSkipTheModel() {
        AnswerService service = new AnswerService(promptBuilder, (system, user) -> {
            throw new AssertionError("the model must not be called");
        });
        RetrievalResult low = new RetrievalResult(RetrievalMode.D, Strategy.SINGLE, List.of(hit(chunk("a"), 1)),
                List.of(), List.of(), List.of(), 0.42, RetrievalResult.AbstainReason.LOW_SIMILARITY);
        RetrievalResult empty = found(List.of(MSFT_Q2));

        Answer lowAnswer = service.answer("What did Nvidia say about stablecoins?", low);
        Answer emptyAnswer = service.answer("Anything?", empty);

        assertThat(lowAnswer.abstainReason()).isEqualTo(AbstainReason.LOW_SIMILARITY);
        assertThat(lowAnswer.text()).endsWith("Searched: all matching documents in the corpus.");
        assertThat(emptyAnswer.abstainReason()).isEqualTo(AbstainReason.NO_SOURCES);
        assertThat(emptyAnswer.text()).endsWith("Searched: MSFT FY2026 Q2.");
    }

    @Test
    void abstentionMessageMentionsMissingAndSkippedQuarters() {
        PeriodTarget googlQ1 = new PeriodTarget("GOOGL", new FiscalPeriod(2026, 1));
        PeriodTarget msft2025 = new PeriodTarget("MSFT", new FiscalPeriod(2025, 4));
        RetrievalResult retrieval = new RetrievalResult(RetrievalMode.D, Strategy.PER_PERIOD, List.of(hit(chunk("a"), 1)),
                List.of(MSFT_Q2, MSFT_Q3), List.of(googlQ1), List.of(msft2025), 0.7, null);

        String message = AnswerService.abstentionMessage(AbstainReason.INSUFFICIENT_EVIDENCE, retrieval);

        assertThat(message).isEqualTo("I couldn't find enough evidence in the filings and earnings calls to answer "
                + "this. Searched: MSFT FY2026 Q2, MSFT FY2026 Q3. Not in the corpus: MSFT FY2025 Q4. "
                + "Not searched (too many quarters in one question): GOOGL FY2026 Q1.");
    }

    @Test
    void modelErrorBecomesLlmUnavailable() {
        RuntimeException boom = new RuntimeException("HTTP 500 from OpenAI");
        AnswerService service = new AnswerService(promptBuilder, (system, user) -> {
            throw boom;
        });

        assertThatThrownBy(() -> service.answer("q?", found(List.of(MSFT_Q2), chunk("a"))))
                .isInstanceOf(LlmUnavailableException.class)
                .hasCause(boom);
    }

    @Test
    void emptyReplyBecomesLlmUnavailable() {
        AnswerService service = serviceReplying(new LlmReply("   ", "gpt-4o-mini", 900, 0, false));

        assertThatThrownBy(() -> service.answer("q?", found(List.of(MSFT_Q2), chunk("a"))))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("empty reply");
    }

    @Test
    void truncatedAnswerIsFlagged() {
        AnswerService service = serviceReplying(new LlmReply("FY2026 Q1: revenue was", "gpt-4o-mini", 9000, 700, true));

        Answer answer = service.answer("Summarise all quarters", found(List.of(MSFT_Q2), chunk("a")));

        assertThat(answer.abstained()).isFalse();
        assertThat(answer.truncated()).isTrue();
        assertThat(answer.completionTokens()).isEqualTo(700);
    }

    // ---------- helpers ----------

    private static RetrievalResult found(List<PeriodTarget> searched, RetrievedChunk... chunks) {
        List<FusedHit> hits = new ArrayList<>();
        for (int i = 0; i < chunks.length; i++) {
            hits.add(hit(chunks[i], i + 1));
        }
        return new RetrievalResult(RetrievalMode.D, Strategy.PER_PERIOD, hits, searched, List.of(), List.of(), 0.7, null);
    }

    private static FusedHit hit(RetrievedChunk chunk, int rank) {
        return new FusedHit(chunk, 1.0 / (60 + rank), rank, rank, null, 0.7);
    }

    private static RetrievedChunk chunk(String content) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), 0, "MSFT", 2026, 2, DocType.PRESS_RELEASE,
                content, 10, null, null, null, null, false);
    }
}
