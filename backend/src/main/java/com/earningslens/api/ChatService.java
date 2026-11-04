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
import com.earningslens.generation.CitationCheck;
import com.earningslens.generation.CitationValidator;
import com.earningslens.generation.GroundingCheck;
import com.earningslens.generation.NumberGroundingCheck;
import com.earningslens.retrieval.FusedHit;
import com.earningslens.retrieval.PeriodTarget;
import com.earningslens.retrieval.QueryParser;
import com.earningslens.retrieval.RetrievalResult;
import com.earningslens.retrieval.RetrievedChunk;
import com.earningslens.retrieval.Retriever;
import com.earningslens.retrieval.SearchFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * One chat request from question to response (spec 9 and 10, roadmap E4.5):
 * retrieve -> answer -> check citations -> check numbers -> build the response -> log to query_log.
 * The citation and number checks only run when there is an answer (not when abstained).
 */
@Service
public class ChatService {

    static final int SNIPPET_CHARS = 300;

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final Retriever retriever;
    private final QueryParser parser;
    private final AnswerService answerService;
    private final CitationValidator citationValidator;
    private final NumberGroundingCheck groundingCheck;
    private final DocumentRepository documents;
    private final QueryLogRepository queryLog;
    private final RetrievalProperties retrievalSettings;

    public ChatService(Retriever retriever, QueryParser parser, AnswerService answerService,
                       CitationValidator citationValidator, NumberGroundingCheck groundingCheck,
                       DocumentRepository documents, QueryLogRepository queryLog, RetrievalProperties retrievalSettings) {
        this.retriever = retriever;
        this.parser = parser;
        this.answerService = answerService;
        this.citationValidator = citationValidator;
        this.groundingCheck = groundingCheck;
        this.documents = documents;
        this.queryLog = queryLog;
        this.retrievalSettings = retrievalSettings;
    }

    public ChatResponse chat(ChatRequest request) {
        long start = System.nanoTime();
        String question = request.question().strip();
        ChatFilters requested = request.filters() == null ? ChatFilters.none() : request.filters();

        RetrievalResult retrieval = retriever.retrieve(question, toSearchFilters(requested));
        Answer answer = answerService.answer(question, retrieval);

        String text;
        List<CitationDto> citations;
        List<String> warnings;
        if (answer.abstained()) {
            text = answer.text();
            citations = List.of();
            warnings = List.of();
        } else {
            CitationCheck checkedCitations = citationValidator.validate(answer.text(), answer.sources().size());
            GroundingCheck checkedNumbers = groundingCheck.check(checkedCitations, answer.sources());
            text = checkedCitations.text();
            citations = citations(checkedCitations.citedMarkers(), answer.sources());
            warnings = new ArrayList<>(checkedCitations.warnings());
            warnings.addAll(checkedNumbers.warnings());
        }

        ChatFilters applied = appliedFilters(question, requested, retrieval);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        ChatResponse response = new ChatResponse(text, answer.abstained(), citations, warnings, applied, latencyMs,
                answer.model());

        logRequest(question, applied, retrieval, response, answer);
        return response;
    }

    // ---------- request -> retrieval ----------

    static SearchFilters toSearchFilters(ChatFilters filters) {
        Set<String> tickers = filters.tickers().stream()
                .map(t -> t.strip().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<FiscalPeriod> periods = filters.fiscalPeriods().stream()
                .map(p -> new FiscalPeriod(p.year(), p.quarter()))
                .collect(Collectors.toSet());
        return new SearchFilters(tickers, periods, Set.copyOf(filters.docTypes()));
    }

    /**
     * What was actually searched: companies and quarters from the retriever's targets (including quarters asked
     * for but not in the corpus), else the requested or detected companies; document types as requested.
     */
    ChatFilters appliedFilters(String question, ChatFilters requested, RetrievalResult retrieval) {
        List<PeriodTarget> targets = new ArrayList<>(retrieval.searchedTargets());
        targets.addAll(retrieval.missing());

        Set<String> tickers = new TreeSet<>();
        Set<FiscalPeriod> periods = new TreeSet<>();
        for (PeriodTarget target : targets) {
            tickers.add(target.ticker());
            periods.add(target.period());
        }
        if (tickers.isEmpty()) {
            tickers.addAll(toSearchFilters(requested).tickers());
        }
        if (tickers.isEmpty() && retrievalSettings.mode().usesParser()) {
            tickers.addAll(parser.parse(question).tickers());
        }
        List<FiscalPeriodDto> periodDtos = periods.stream()
                .map(p -> new FiscalPeriodDto(p.year(), p.quarter()))
                .toList();
        return new ChatFilters(List.copyOf(tickers), periodDtos, requested.docTypes());
    }

    // ---------- answer -> citations ----------

    private List<CitationDto> citations(List<Integer> markers, List<RetrievedChunk> sources) {
        Set<UUID> documentIds = new LinkedHashSet<>();
        for (int marker : markers) {
            documentIds.add(sources.get(marker - 1).documentId());
        }
        Map<UUID, String> urls = StreamSupport.stream(documents.findAllById(documentIds).spliterator(), false)
                .collect(Collectors.toMap(DocumentEntity::getId, DocumentEntity::getSourceUrl, (a, b) -> a));

        List<CitationDto> result = new ArrayList<>();
        for (int marker : markers) {
            RetrievedChunk chunk = sources.get(marker - 1);
            result.add(new CitationDto(marker, chunk.id(), chunk.ticker(), chunk.fiscalYear(), chunk.fiscalQuarter(),
                    chunk.docType(), chunk.pageStart(), chunk.section(), chunk.speaker(),
                    urls.get(chunk.documentId()), snippet(chunk.content())));
        }
        return result;
    }

    /**
     * The first 300 characters of the chunk's own text. The header lines the chunker adds
     * ("[MSFT | FY2026 Q2 | TRANSCRIPT]", "Section: ...", "Speaker: ...") are skipped, because the
     * citation already has those fields. Whitespace is collapsed so the snippet reads as one paragraph.
     */
    static String snippet(String content) {
        List<String> lines = new ArrayList<>(List.of(content.strip().split("\\R")));
        while (!lines.isEmpty() && isHeaderLine(lines.get(0))) {
            lines.remove(0);
        }
        String text = String.join(" ", lines).replaceAll("\\s+", " ").strip();
        return text.length() <= SNIPPET_CHARS ? text : text.substring(0, SNIPPET_CHARS);
    }

    private static boolean isHeaderLine(String line) {
        String l = line.strip();
        return (l.startsWith("[") && l.endsWith("]")) || l.startsWith("Section: ") || l.startsWith("Speaker: ");
    }

    // ---------- logging ----------

    private void logRequest(String question, ChatFilters applied, RetrievalResult retrieval, ChatResponse response,
                            Answer answer) {
        List<UUID> chunkIds = retrieval.chunks().stream().map(FusedHit::chunk).map(RetrievedChunk::id).toList();
        try {
            queryLog.insert(new QueryLogRepository.Entry(question, applied, chunkIds, response.answer(),
                    response.abstained(), response.latencyMs(), answer.promptTokens(), answer.completionTokens()));
        } catch (RuntimeException e) {
            // The user still gets the answer; a failed log row must not turn a good answer into an error.
            log.warn("Could not write query_log: {}", e.toString());
        }
    }
}
