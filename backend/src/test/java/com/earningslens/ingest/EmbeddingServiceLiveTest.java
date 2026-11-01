package com.earningslens.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The roadmap's "real call" check for E1.7: embeds 3 sample chunks with the real
 * OpenAI API and checks each vector has 1536 numbers.
 *
 * Costs a tiny fraction of a cent. Skipped automatically when no OpenAI key is set
 * (for example in CI). Needs the local Docker database, like the other @SpringBootTest tests.
 */
@SpringBootTest
class EmbeddingServiceLiveTest {

    @Autowired
    EmbeddingService embeddingService;

    @Autowired
    Environment environment;

    @Test
    void realCallEmbedsThreeChunksInto1536Dimensions() {
        String key = environment.getProperty("spring.ai.openai.api-key");
        assumeTrue(key != null && !key.isBlank() && !key.equals("not-set"), "No OpenAI key configured");

        List<float[]> vectors = embeddingService.embed(List.of(
                "[MSFT | FY2027 Q1 | PRESS_RELEASE]\nRevenue was $92.3 billion and increased 19%.",
                "[GOOGL | FY2026 Q3 | TRANSCRIPT]\nSpeaker: Sundar Pichai, CEO\nGoogle Cloud revenues grew 103%.",
                "[META | FY2026 Q3 | FORM_10Q]\nTotal revenue for the third quarter of 2026 was $64.60 billion."));

        assertThat(vectors).hasSize(3).allSatisfy(vector -> assertThat(vector).hasSize(1536));
        assertThat(embeddingService.totalTokens()).isPositive();
    }
}
