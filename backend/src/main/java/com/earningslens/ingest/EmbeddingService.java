package com.earningslens.ingest;

import com.earningslens.config.EmbeddingProperties;
import com.earningslens.config.LlmProperties;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns texts into embedding vectors with OpenAI (roadmap E1.7, spec 7.2 step 5).
 *
 * - Sends texts in batches of up to 64 per API call (fewer calls, same price).
 * - On a rate limit (HTTP 429), a server error (5xx) or a network error, waits and
 *   retries with exponential backoff: 1s, 2s, 4s, 8s, 16s, then gives up.
 *   Other errors (for example 400 bad request or 401 wrong key) fail immediately,
 *   because retrying cannot fix them.
 * - Logs the tokens used per batch and in total, so costs are visible.
 * - Checks every vector has the configured number of dimensions (1536).
 */
@Component
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    /** Waiting is behind an interface so tests can retry instantly instead of sleeping. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final EmbeddingModel embeddingModel;
    private final LlmProperties llm;
    private final EmbeddingProperties settings;
    private final Sleeper sleeper;
    private long totalTokens;

    // SPRING CONCEPT: with two constructors, @Autowired tells Spring which one to use.
    @Autowired
    public EmbeddingService(EmbeddingModel embeddingModel, LlmProperties llm, EmbeddingProperties settings) {
        this(embeddingModel, llm, settings, Thread::sleep);
    }

    EmbeddingService(EmbeddingModel embeddingModel, LlmProperties llm, EmbeddingProperties settings, Sleeper sleeper) {
        this.embeddingModel = embeddingModel;
        this.llm = llm;
        this.settings = settings;
        this.sleeper = sleeper;
    }

    /** Returns one vector per text, in the same order as the texts. */
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += settings.batchSize()) {
            List<String> batch = texts.subList(start, Math.min(start + settings.batchSize(), texts.size()));
            vectors.addAll(embedWithRetry(batch));
        }
        return vectors;
    }

    /** Tokens used by all calls so far (for logging at the end of an ingest run). */
    public long totalTokens() {
        return totalTokens;
    }

    private List<float[]> embedWithRetry(List<String> batch) {
        EmbeddingRequest request = new EmbeddingRequest(batch, OpenAiEmbeddingOptions.builder()
                .model(llm.embeddingModel())
                .dimensions(llm.embeddingDimensions())
                .build());

        for (int attempt = 0; ; attempt++) {
            try {
                return toVectors(embeddingModel.call(request), batch.size());
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt >= settings.maxRetries()) {
                    throw e;
                }
                long waitMillis = backoffMillis(attempt);
                log.warn("Embedding call failed ({}). Retry {}/{} in {} ms",
                        e.getMessage(), attempt + 1, settings.maxRetries(), waitMillis);
                pause(waitMillis);
            }
        }
    }

    private List<float[]> toVectors(EmbeddingResponse response, int expected) {
        List<Embedding> results = new ArrayList<>(response.getResults());
        if (results.size() != expected) {
            throw new IllegalStateException("Asked for %d embeddings, got %d".formatted(expected, results.size()));
        }
        results.sort(Comparator.comparing(Embedding::getIndex));

        List<float[]> vectors = new ArrayList<>(expected);
        for (Embedding embedding : results) {
            float[] vector = embedding.getOutput();
            if (vector.length != llm.embeddingDimensions()) {
                throw new IllegalStateException("Expected %d dimensions, got %d"
                        .formatted(llm.embeddingDimensions(), vector.length));
            }
            vectors.add(vector);
        }

        int batchTokens = tokensUsed(response);
        totalTokens += batchTokens;
        log.info("Embedded {} texts using {} tokens ({} tokens so far)", expected, batchTokens, totalTokens);
        return vectors;
    }

    private static int tokensUsed(EmbeddingResponse response) {
        if (response.getMetadata() == null) {
            return 0;
        }
        Usage usage = response.getMetadata().getUsage();
        Integer tokens = usage == null ? null : usage.getTotalTokens();
        return tokens == null ? 0 : tokens;
    }

    /** 429, 5xx and network errors are worth retrying; anything else is not. */
    static boolean isRetryable(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof OpenAIServiceException serviceError) {
                return isRetryableStatus(serviceError.statusCode());
            }
            if (t instanceof RestClientResponseException httpError) {
                return isRetryableStatus(httpError.getStatusCode().value());
            }
            if (t instanceof OpenAIIoException || t instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    long backoffMillis(int attempt) {
        long wait = settings.initialBackoffMillis() << Math.min(attempt, 20);
        return Math.min(wait, settings.maxBackoffMillis());
    }

    private void pause(long millis) {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry an embedding call", e);
        }
    }
}
