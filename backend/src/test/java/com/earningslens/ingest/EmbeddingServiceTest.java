package com.earningslens.ingest;

import com.earningslens.config.EmbeddingProperties;
import com.earningslens.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses a Mockito mock instead of the real OpenAI API: no network, no cost, and we can
 * make it fail on purpose. Sleeping is replaced by recording the waits.
 */
class EmbeddingServiceTest {

    private static final int DIMENSIONS = 1536;
    private static final LlmProperties LLM =
            new LlmProperties("gpt-4o-mini", "text-embedding-3-small", DIMENSIONS, 0.0, 700);
    private static final EmbeddingProperties SETTINGS = new EmbeddingProperties(64, 5, 1000, 30000);

    private final EmbeddingModel model = mock(EmbeddingModel.class);
    private final List<Long> waits = new ArrayList<>();
    private final EmbeddingService service = new EmbeddingService(model, LLM, SETTINGS, waits::add);

    @Test
    void retriesAfterTwoFailuresThenSucceeds() {
        when(model.call(any(EmbeddingRequest.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenAnswer(invocation -> responseFor(invocation.getArgument(0)));

        List<float[]> vectors = service.embed(List.of("a", "b", "c"));

        assertThat(vectors).hasSize(3).allSatisfy(v -> assertThat(v).hasSize(DIMENSIONS));
        verify(model, times(3)).call(any(EmbeddingRequest.class));
        assertThat(waits).containsExactly(1000L, 2000L);   // exponential backoff
    }

    @Test
    void doesNotRetryAClientErrorLikeABadRequest() {
        when(model.call(any(EmbeddingRequest.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.embed(List.of("a"))).isInstanceOf(HttpClientErrorException.class);
        verify(model, times(1)).call(any(EmbeddingRequest.class));
        assertThat(waits).isEmpty();
    }

    @Test
    void givesUpAfterFiveRetries() {
        when(model.call(any(EmbeddingRequest.class))).thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.embed(List.of("a"))).isInstanceOf(HttpServerErrorException.class);
        verify(model, times(6)).call(any(EmbeddingRequest.class));   // 1 try + 5 retries
        assertThat(waits).containsExactly(1000L, 2000L, 4000L, 8000L, 16000L);
    }

    @Test
    void sendsBatchesOf64AndKeepsTheOrder() {
        when(model.call(any(EmbeddingRequest.class))).thenAnswer(invocation -> responseFor(invocation.getArgument(0)));
        List<String> texts = IntStream.range(0, 130).mapToObj(i -> "text " + i).toList();

        List<float[]> vectors = service.embed(texts);

        verify(model, times(3)).call(any(EmbeddingRequest.class));   // 64 + 64 + 2
        assertThat(vectors).hasSize(130);
        for (int i = 0; i < 130; i++) {
            assertThat(vectors.get(i)[0]).as("vector " + i).isEqualTo(i);
        }
    }

    @Test
    void rejectsVectorsWithTheWrongNumberOfDimensions() {
        when(model.call(any(EmbeddingRequest.class)))
                .thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[10], 0))));

        assertThatThrownBy(() -> service.embed(List.of("a")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("1536");
    }

    @Test
    void backoffIsCappedAtTheMaximum() {
        assertThat(service.backoffMillis(10)).isEqualTo(30000L);
    }

    /**
     * A fake response with one vector per input text. Results come back in reverse
     * order on purpose, to prove the service sorts them by index. The first number of
     * each vector is the text's number ("text 7" gives 7) so the test can check order.
     */
    private static EmbeddingResponse responseFor(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        List<Embedding> results = new ArrayList<>();
        for (int i = texts.size() - 1; i >= 0; i--) {
            float[] vector = new float[DIMENSIONS];
            String text = texts.get(i);
            vector[0] = text.startsWith("text ") ? Float.parseFloat(text.substring(5)) : i;
            results.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(results);
    }
}
