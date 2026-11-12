package com.earningslens.api;

import com.earningslens.PgvectorContainerTest;
import com.earningslens.ingest.DocType;
import com.earningslens.ingest.DocumentFormat;
import com.earningslens.ingest.IngestionService;
import com.earningslens.ingest.ManifestEntry;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Roadmap E7.1: the whole system end to end, against the shared pgvector test container.
 *
 * 1. Ingest two synthetic fixture documents with the real pipeline (HtmlParser, TextCleaner, Chunker,
 *    EmbeddingService, DocumentStore): an MSFT FY2026 Q2 press release (3 sections, so 3 chunks) and an
 *    MSFT FY2026 Q3 call transcript (4 speaker turns, so 4 chunks).
 * 2. Call POST /api/v1/chat through MockMvc: the real controller, validation, rate limiter, Retriever,
 *    PromptBuilder, AnswerService, OpenAiChatLlm, CitationValidator, NumberGroundingCheck and ChatService.
 * 3. Check every citation in the JSON response points to the chunk in the database that holds the
 *    sentence the fake model cited.
 *
 * No OpenAI calls, no cost. Spring AI's two model beans are replaced with Mockito mocks:
 * - EmbeddingModel: a fake, deterministic "bag of words" embedding (each word adds 1 to one of the
 *   1536 positions, then the vector is scaled to length 1), so similar texts get similar vectors.
 * - ChatModel: a fake LLM that reads the numbered sources in the real prompt, finds the source that
 *   contains a given sentence and answers with that source's marker, the way gpt-4o-mini should.
 * As a second safety net the OpenAI key is forced to a dummy value: a missed mock would fail with
 * 401 instead of spending money.
 *
 * SPRING TEST CONCEPTS:
 * - Testcontainers lifecycle: PgvectorContainerTest starts one pgvector Docker container per test run
 *   (static block); all container tests share it, Flyway migrates it, and it is removed when the JVM
 *   exits. Needs Docker Desktop running.
 * - @SpringBootTest (inherited) starts the full application context, unlike a slice such as
 *   @WebMvcTest, which loads only the web layer. @AutoConfigureMockMvc adds MockMvc, which sends
 *   requests through the real Spring MVC stack without opening a network port.
 * - @MockitoBean replaces one bean in that context with a Mockito mock. The mocks are reset after each
 *   test, so they are set up again in @BeforeEach. A different set of mocks means Spring builds a
 *   separate context for this class (it still uses the same container).
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.ai.openai.api-key=test-key-no-real-calls")
class ChatEndToEndTest extends PgvectorContainerTest {

    private static final ManifestEntry Q2_PRESS_RELEASE = new ManifestEntry("MSFT", "Microsoft Corporation",
            2026, 2, DocType.PRESS_RELEASE, "https://example.com/fixtures/msft-fy2026-q2-press-release",
            DocumentFormat.HTML);
    private static final ManifestEntry Q3_TRANSCRIPT = new ManifestEntry("MSFT", "Microsoft Corporation",
            2026, 3, DocType.TRANSCRIPT, "https://example.com/fixtures/msft-fy2026-q3-transcript",
            DocumentFormat.HTML);

    private static final String Q2_REVENUE = "Revenue was $11.1 billion";
    private static final String Q3_REVENUE = "Revenue was $13.3 billion";
    private static final String FAKE_MODEL = "fake-llm-for-tests";

    /** A source header in the user message, for example "[2] (MSFT | FY2026 Q2 | PRESS_RELEASE | section: ...)". */
    private static final Pattern SOURCE_HEADER = Pattern.compile("(?m)^\\[(\\d+)] \\(.*\\)$");

    @Autowired MockMvc mvc;
    @Autowired IngestionService ingestion;
    @Autowired RateLimitInterceptor rateLimit;

    @MockitoBean EmbeddingModel embeddingModel;
    @MockitoBean ChatModel chatModel;

    /** What the fake LLM answers, given the user message (the numbered sources and the question). */
    private Function<String, String> fakeAnswer;

    /** Marker the fake LLM used -> the sentence it was citing. Filled while the fake LLM answers. */
    private final Map<Integer, String> citedSentences = new LinkedHashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        clearDatabase();
        rateLimit.reset();
        citedSentences.clear();
        fakeAnswer = userMessage -> "INSUFFICIENT_EVIDENCE";

        when(embeddingModel.call(any(EmbeddingRequest.class)))
                .thenAnswer(invocation -> fakeEmbeddings(invocation.getArgument(0)));
        when(chatModel.call(any(Prompt.class)))
                .thenAnswer(invocation -> fakeChatResponse(invocation.getArgument(0)));

        assertThat(ingestion.ingest(Q2_PRESS_RELEASE, fixture("msft-fy2026-q2-press-release-synthetic.html")).chunks())
                .isEqualTo(3);
        assertThat(ingestion.ingest(Q3_TRANSCRIPT, fixture("msft-fy2026-q3-transcript-synthetic.html")).chunks())
                .isEqualTo(4);
        verify(embeddingModel, atLeastOnce()).call(any(EmbeddingRequest.class));   // ingest used the fake
        clearInvocations(embeddingModel, chatModel);
    }

    @Test
    void citationMapsToTheChunkThatHoldsTheCitedSentence() throws Exception {
        fakeAnswer = userMessage -> "In FY2026 Q2, revenue was $11.1 billion " + cite(userMessage, Q2_REVENUE) + ".";

        String json = chat("What was Microsoft's revenue in Q2 FY2026?");

        assertThat((Boolean) JsonPath.read(json, "$.abstained")).isFalse();
        assertThat((String) JsonPath.read(json, "$.model")).isEqualTo(FAKE_MODEL);
        assertThat((List<?>) JsonPath.read(json, "$.groundingWarnings")).isEmpty();   // $11.1 billion is in the cited chunk
        assertCitationsMatchCitedSentences(json);

        Map<String, Object> citation = citations(json).get(0);
        assertThat(citation.get("ticker")).isEqualTo("MSFT");
        assertThat(citation.get("fiscalYear")).isEqualTo(2026);
        assertThat(citation.get("fiscalQuarter")).isEqualTo(2);
        assertThat(citation.get("docType")).isEqualTo("PRESS_RELEASE");
        assertThat(citation.get("section")).isEqualTo("Quarterly Results");
        assertThat(citation.get("sourceUrl")).isEqualTo(Q2_PRESS_RELEASE.url());
        assertThat((String) citation.get("snippet")).contains(Q2_REVENUE);

        verify(chatModel).call(any(Prompt.class));   // exactly one LLM call
    }

    @Test
    void twoQuarterQuestionCitesTheRightChunkFromEachDocument() throws Exception {
        fakeAnswer = userMessage -> "In FY2026 Q2, revenue was $11.1 billion " + cite(userMessage, Q2_REVENUE)
                + ". In FY2026 Q3, revenue was $13.3 billion " + cite(userMessage, Q3_REVENUE) + ".";

        String json = chat("How did Microsoft's revenue change from Q2 to Q3 FY2026?");

        assertThat((Boolean) JsonPath.read(json, "$.abstained")).isFalse();
        assertThat((List<?>) JsonPath.read(json, "$.groundingWarnings")).isEmpty();
        assertThat((List<?>) JsonPath.read(json, "$.appliedFilters.fiscalPeriods[*].quarter")).containsExactly(2, 3);
        assertThat(citations(json)).hasSize(2);
        assertCitationsMatchCitedSentences(json);

        Map<String, Object> transcript = citations(json).stream()
                .filter(c -> "TRANSCRIPT".equals(c.get("docType"))).findFirst().orElseThrow();
        assertThat(transcript.get("fiscalQuarter")).isEqualTo(3);
        assertThat(transcript.get("speaker")).isEqualTo("Jane Example, Chief Financial Officer");
        assertThat(transcript.get("page")).isNull();
        assertThat(transcript.get("sourceUrl")).isEqualTo(Q3_TRANSCRIPT.url());
    }

    @Test
    void markerForASourceThatDoesNotExistIsRemovedWithAWarning() throws Exception {
        fakeAnswer = userMessage -> "In FY2026 Q2, revenue was $11.1 billion " + cite(userMessage, Q2_REVENUE) + "[99].";

        String json = chat("What was Microsoft's revenue in Q2 FY2026?");

        assertThat((String) JsonPath.read(json, "$.answer")).doesNotContain("[99]");
        assertThat((List<String>) JsonPath.read(json, "$.groundingWarnings"))
                .anySatisfy(w -> assertThat(w).startsWith("Removed citation [99]"));
        assertThat(citations(json)).hasSize(1);
        assertCitationsMatchCitedSentences(json);
    }

    @Test
    void quarterNotInTheCorpusAbstainsWithoutCallingTheLlm() throws Exception {
        String json = chat("What was Microsoft's revenue in Q1 FY2025?");

        assertThat((Boolean) JsonPath.read(json, "$.abstained")).isTrue();
        assertThat(citations(json)).isEmpty();
        assertThat((Object) JsonPath.read(json, "$.model")).isNull();
        assertThat((List<?>) JsonPath.read(json, "$.appliedFilters.fiscalPeriods[*].year")).containsExactly(2025);
        verify(chatModel, never()).call(any(Prompt.class));
        verify(embeddingModel, never()).call(any(EmbeddingRequest.class));
    }

    // ---------- the request ----------

    private String chat(String question) throws Exception {
        String body = "{\"question\": \"" + question + "\"}";
        return mvc.perform(post("/api/v1/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<Map<String, Object>> citations(String json) {
        return JsonPath.read(json, "$.citations");
    }

    /**
     * The core check: for every citation, the marker the fake LLM wrote for a sentence leads to the
     * chunk in the database that contains that sentence (same chunk id), and the response cites
     * exactly the markers the fake LLM used for real sources.
     */
    private void assertCitationsMatchCitedSentences(String json) {
        List<Map<String, Object>> citations = citations(json);
        assertThat(citations).extracting(c -> (Integer) c.get("marker"))
                .containsExactlyInAnyOrderElementsOf(citedSentences.keySet());
        for (Map<String, Object> citation : citations) {
            String sentence = citedSentences.get((Integer) citation.get("marker"));
            assertThat(UUID.fromString((String) citation.get("chunkId")))
                    .as("chunk for marker [%s] (%s)", citation.get("marker"), sentence)
                    .isEqualTo(chunkIdContaining(sentence));
        }
    }

    /** The one chunk whose text contains the sentence (fails if none or more than one). */
    private UUID chunkIdContaining(String sentence) {
        List<UUID> ids = jdbc.queryForList("SELECT id FROM chunks WHERE position(? in content) > 0", UUID.class, sentence);
        assertThat(ids).as("chunks containing '%s'", sentence).hasSize(1);
        return ids.get(0);
    }

    // ---------- the fake LLM ----------

    private ChatResponse fakeChatResponse(Prompt prompt) {
        String userMessage = prompt.getInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .findFirst().orElseThrow();
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model(FAKE_MODEL).build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(fakeAnswer.apply(userMessage)))), metadata);
    }

    /** "[n]" for the one source in the prompt whose text contains the sentence; remembers n -> sentence. */
    private String cite(String userMessage, String sentence) {
        // Each source runs from the end of its "[n] (...)" header to the start of the next header,
        // and the last one to the "Question:" line.
        List<int[]> headers = new ArrayList<>();   // {source number, header start, header end}
        Matcher header = SOURCE_HEADER.matcher(userMessage);
        while (header.find()) {
            headers.add(new int[] {Integer.parseInt(header.group(1)), header.start(), header.end()});
        }
        int questionStart = userMessage.lastIndexOf("\n\nQuestion: ");
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            int bodyEnd = i + 1 < headers.size() ? headers.get(i + 1)[1] : questionStart;
            if (userMessage.substring(headers.get(i)[2], bodyEnd).contains(sentence)) {
                matches.add(headers.get(i)[0]);
            }
        }
        assertThat(matches).as("sources in the prompt containing '%s'", sentence).hasSize(1);
        citedSentences.put(matches.get(0), sentence);
        return "[" + matches.get(0) + "]";
    }

    // ---------- the fake embeddings ----------

    private static EmbeddingResponse fakeEmbeddings(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        List<Embedding> results = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            results.add(new Embedding(bagOfWords(texts.get(i)), i));
        }
        return new EmbeddingResponse(results);
    }

    /** 1536 numbers: each word adds 1 at position hash(word) mod 1536; scaled to length 1. */
    static float[] bagOfWords(String text) {
        float[] vector = new float[DIMS];
        for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!word.isEmpty()) {
                vector[Math.floorMod(word.hashCode(), DIMS)] += 1;
            }
        }
        double length = 0;
        for (float v : vector) {
            length += v * v;
        }
        if (length == 0) {
            vector[0] = 1;   // a zero vector has no cosine similarity
            return vector;
        }
        for (int i = 0; i < DIMS; i++) {
            vector[i] = (float) (vector[i] / Math.sqrt(length));
        }
        return vector;
    }

    private static Path fixture(String name) throws Exception {
        return Path.of(ChatEndToEndTest.class.getResource("/fixtures/" + name).toURI());
    }
}
