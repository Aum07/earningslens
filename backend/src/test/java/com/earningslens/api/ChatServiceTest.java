package com.earningslens.api;

import com.earningslens.api.dto.ChatFilters;
import com.earningslens.api.dto.ChatRequest;
import com.earningslens.api.dto.ChatResponse;
import com.earningslens.api.dto.CitationDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.config.RetrievalProperties;
import com.earningslens.document.DocumentEntity;
import com.earningslens.document.DocumentRepository;
import com.earningslens.eval.FiscalPeriod;
import com.earningslens.generation.Answer;
import com.earningslens.generation.AnswerService;
import com.earningslens.generation.CitationValidator;
import com.earningslens.generation.NumberGroundingCheck;
import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.CompanyDirectory;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.PeriodTarget;
import com.earningslens.retrieval.QueryParser;
import com.earningslens.retrieval.RetrievalMode;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.RetrievalResult.Strategy;
import com.earningslens.retrieval.RetrievedChunk;
import com.earningslens.retrieval.Retriever;
import com.earningslens.retrieval.SearchFilters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Roadmap E4.5: the chat flow with mocked retrieval, model and database, so no OpenAI calls and no Docker.
 * The citation and number checks are the real ones.
 */
class ChatServiceTest {

    private static final RetrievalProperties SETTINGS =
            new RetrievalProperties(16, 5, 4, 30, 30, 60, 0.531, RetrievalMode.D);
    private static final PeriodTarget MSFT_Q2 = new PeriodTarget("MSFT", new FiscalPeriod(2026, 2));
    private static final UUID PRESS_RELEASE_DOC = UUID.randomUUID();
    private static final UUID TRANSCRIPT_DOC = UUID.randomUUID();

    private final Retriever retriever = mock(Retriever.class);
    private final AnswerService answerService = mock(AnswerService.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final QueryLogRepository queryLog = mock(QueryLogRepository.class);
    private final ChatService service = new ChatService(retriever, parser(), answerService, new CitationValidator(),
            new NumberGroundingCheck(), documents, queryLog, SETTINGS);

    private final RetrievedChunk pressRelease = chunk(PRESS_RELEASE_DOC, DocType.PRESS_RELEASE, null, "Segment Results",
            null, "[MSFT | FY2026 Q2 | PRESS_RELEASE]\nSection: Segment Results\nRevenue was $81.3 billion and increased 17%.");
    private final RetrievedChunk transcript = chunk(TRANSCRIPT_DOC, DocType.TRANSCRIPT, null, "Prepared remarks",
            "Amy Hood", "[MSFT | FY2026 Q2 | TRANSCRIPT]\nSection: Prepared remarks\nSpeaker: Amy Hood\nWe saw strong demand.");
    private final RetrievedChunk unused = chunk(TRANSCRIPT_DOC, DocType.TRANSCRIPT, null, null, null, "unused text");

    @Test
    void answerIsCheckedAndTurnedIntoTheSpecResponse() {
        RetrievalResult retrieval = found(pressRelease, transcript, unused);
        when(retriever.retrieve(anyString(), any(SearchFilters.class))).thenReturn(retrieval);
        when(answerService.answer(anyString(), eq(retrieval))).thenReturn(new Answer(
                "Revenue was $81.3 billion, up 17% [1]. Demand was strong [2][9]. Margins were 52% [1].",
                false, null, List.of(pressRelease, transcript, unused), "gpt-4o-mini-2024-07-18", 1400, 30, false));
        when(documents.findAllById(any())).thenReturn(List.of(
                new DocumentEntity(PRESS_RELEASE_DOC, "MSFT", "Microsoft", 2026, 2, DocType.PRESS_RELEASE,
                        "https://example.com/pr", "a".repeat(64)),
                new DocumentEntity(TRANSCRIPT_DOC, "MSFT", "Microsoft", 2026, 2, DocType.TRANSCRIPT,
                        "https://example.com/call", "b".repeat(64))));

        ChatResponse response = service.chat(new ChatRequest("  What was Microsoft revenue in Q2 FY2026?  ", null));

        assertThat(response.abstained()).isFalse();
        assertThat(response.answer())
                .isEqualTo("Revenue was $81.3 billion, up 17% [1]. Demand was strong [2]. Margins were 52% [1].");
        assertThat(response.citations()).extracting(CitationDto::marker).containsExactly(1, 2);   // only cited ones
        CitationDto first = response.citations().get(0);
        assertThat(first.chunkId()).isEqualTo(pressRelease.id());
        assertThat(first.sourceUrl()).isEqualTo("https://example.com/pr");
        assertThat(first.section()).isEqualTo("Segment Results");
        assertThat(first.snippet()).isEqualTo("Revenue was $81.3 billion and increased 17%.");
        assertThat(response.citations().get(1).speaker()).isEqualTo("Amy Hood");
        assertThat(response.citations().get(1).snippet()).isEqualTo("We saw strong demand.");
        assertThat(response.groundingWarnings()).containsExactly(
                "Removed citation [9]: there are only 3 sources.",
                "Not found in the cited sources: 52%");
        assertThat(response.appliedFilters().tickers()).containsExactly("MSFT");
        assertThat(response.appliedFilters().fiscalPeriods()).containsExactly(new FiscalPeriodDto(2026, 2));
        assertThat(response.model()).isEqualTo("gpt-4o-mini-2024-07-18");
        assertThat(response.latencyMs()).isGreaterThanOrEqualTo(0);

        ArgumentCaptor<QueryLogRepository.Entry> logged = ArgumentCaptor.forClass(QueryLogRepository.Entry.class);
        verify(queryLog).insert(logged.capture());
        QueryLogRepository.Entry entry = logged.getValue();
        assertThat(entry.question()).isEqualTo("What was Microsoft revenue in Q2 FY2026?");
        assertThat(entry.retrievedChunkIds()).containsExactly(pressRelease.id(), transcript.id(), unused.id());
        assertThat(entry.answer()).isEqualTo(response.answer());
        assertThat(entry.abstained()).isFalse();
        assertThat(entry.promptTokens()).isEqualTo(1400);
        assertThat(entry.completionTokens()).isEqualTo(30);
    }

    @Test
    void abstentionHasNoCitationsOrWarningsAndIsStillLogged() {
        PeriodTarget fy2024 = new PeriodTarget("MSFT", new FiscalPeriod(2024, 1));
        RetrievalResult retrieval = new RetrievalResult(RetrievalMode.D, Strategy.NONE, List.of(), List.of(), List.of(),
                List.of(fy2024), null, RetrievalResult.AbstainReason.PERIOD_NOT_IN_CORPUS);
        when(retriever.retrieve(anyString(), any(SearchFilters.class))).thenReturn(retrieval);
        when(answerService.answer(anyString(), eq(retrieval))).thenReturn(new Answer("Not in the corpus.", true,
                Answer.AbstainReason.PERIOD_NOT_IN_CORPUS, List.of(), null, null, null, false));

        ChatResponse response = service.chat(new ChatRequest("Microsoft revenue in FY2024 Q1?", null));

        assertThat(response.abstained()).isTrue();
        assertThat(response.answer()).isEqualTo("Not in the corpus.");
        assertThat(response.citations()).isEmpty();
        assertThat(response.groundingWarnings()).isEmpty();
        assertThat(response.model()).isNull();
        assertThat(response.appliedFilters().fiscalPeriods()).containsExactly(new FiscalPeriodDto(2024, 1));

        ArgumentCaptor<QueryLogRepository.Entry> logged = ArgumentCaptor.forClass(QueryLogRepository.Entry.class);
        verify(queryLog).insert(logged.capture());
        assertThat(logged.getValue().abstained()).isTrue();
        assertThat(logged.getValue().retrievedChunkIds()).isEmpty();
        assertThat(logged.getValue().promptTokens()).isNull();
    }

    @Test
    void requestFiltersAreNormalisedAndPassedToTheRetriever() {
        RetrievalResult retrieval = found();
        when(retriever.retrieve(anyString(), any(SearchFilters.class))).thenReturn(retrieval);
        when(answerService.answer(anyString(), eq(retrieval))).thenReturn(new Answer("Nothing found.", true,
                Answer.AbstainReason.NO_SOURCES, List.of(), null, null, null, false));
        ChatFilters filters = new ChatFilters(List.of(" msft "), List.of(new FiscalPeriodDto(2026, 2)),
                List.of(DocType.TRANSCRIPT));

        ChatResponse response = service.chat(new ChatRequest("What did they say about AI?", filters));

        verify(retriever).retrieve("What did they say about AI?",
                new SearchFilters(Set.of("MSFT"), Set.of(new FiscalPeriod(2026, 2)), Set.of(DocType.TRANSCRIPT)));
        assertThat(response.appliedFilters().docTypes()).containsExactly(DocType.TRANSCRIPT);
    }

    @Test
    void companyNamedInTheQuestionShowsInAppliedFiltersWhenNoQuarterWasSearched() {
        RetrievalResult retrieval = new RetrievalResult(RetrievalMode.D, Strategy.SINGLE, List.of(), List.of(),
                List.of(), List.of(), 0.7, null);
        when(retriever.retrieve(anyString(), any(SearchFilters.class))).thenReturn(retrieval);
        when(answerService.answer(anyString(), eq(retrieval))).thenReturn(new Answer("Nothing found.", true,
                Answer.AbstainReason.NO_SOURCES, List.of(), null, null, null, false));

        ChatResponse response = service.chat(new ChatRequest("What did Microsoft say about AI capex?", null));

        assertThat(response.appliedFilters().tickers()).containsExactly("MSFT");
        assertThat(response.appliedFilters().fiscalPeriods()).isEmpty();
    }

    @Test
    void aFailedLogWriteDoesNotFailTheRequest() {
        RetrievalResult retrieval = found();
        when(retriever.retrieve(anyString(), any(SearchFilters.class))).thenReturn(retrieval);
        when(answerService.answer(anyString(), eq(retrieval))).thenReturn(new Answer("Nothing found.", true,
                Answer.AbstainReason.NO_SOURCES, List.of(), null, null, null, false));
        when(queryLog.insert(any())).thenThrow(new IllegalStateException("database down"));

        ChatResponse response = service.chat(new ChatRequest("What was revenue?", null));

        assertThat(response.answer()).isEqualTo("Nothing found.");
    }

    @Test
    void snippetSkipsHeaderLinesAndIsAtMost300Characters() {
        assertThat(ChatService.snippet("[META | FY2026 Q1 | TRANSCRIPT]\nSection: Q&A\nSpeaker: Susan Li\nLine one.\n\nLine two."))
                .isEqualTo("Line one. Line two.");
        assertThat(ChatService.snippet("x".repeat(500))).hasSize(ChatService.SNIPPET_CHARS);
    }

    // ---------- helpers ----------

    private static QueryParser parser() {
        try (InputStream in = ChatServiceTest.class.getResourceAsStream("/companies.yaml")) {
            return new QueryParser(CompanyDirectory.load(in));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RetrievalResult found(RetrievedChunk... chunks) {
        List<FusedHit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.length; i++) {
            hits.add(new FusedHit(chunks[i], 1.0 / (61 + i), i + 1, i + 1, null, 0.7));
        }
        return new RetrievalResult(RetrievalMode.D, Strategy.SINGLE, hits, List.of(MSFT_Q2), List.of(), List.of(),
                0.7, null);
    }

    private static RetrievedChunk chunk(UUID documentId, DocType type, Integer page, String section, String speaker,
                                        String content) {
        return new RetrievedChunk(UUID.randomUUID(), documentId, 0, "MSFT", 2026, 2, type, content, 50, page, page,
                section, speaker, false);
    }
}
