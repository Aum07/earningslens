package com.earningslens.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that application.yaml is bound into the properties records correctly.
 * Needs the local Docker database running (docker compose up -d).
 */
@SpringBootTest
class ConfigPropertiesTest {

    // @Autowired asks Spring to inject the bean it created from application.yaml.
    @Autowired LlmProperties llm;
    @Autowired RetrievalProperties retrieval;
    @Autowired RateLimitProperties rateLimit;
    @Autowired SecProperties sec;

    @Test
    void llmSettingsMatchSpec() {
        assertThat(llm.embeddingDimensions()).isEqualTo(1536);
        assertThat(llm.temperature()).isZero();
        assertThat(llm.maxOutputTokens()).isEqualTo(700);
    }

    @Test
    void retrievalSettingsMatchSpec() {
        assertThat(retrieval.k()).isEqualTo(8);
        assertThat(retrieval.kPerPeriod()).isEqualTo(3);
        assertThat(retrieval.rrfK()).isEqualTo(60);
    }

    @Test
    void rateLimitsMatchSpec() {
        assertThat(rateLimit.requestsPerMinutePerIp()).isEqualTo(10);
        assertThat(rateLimit.dailyChatCap()).isEqualTo(300);
    }

    @Test
    void secUserAgentIsSet() {
        assertThat(sec.userAgent()).isNotBlank();
        assertThat(sec.minDelayMillis()).isGreaterThanOrEqualTo(100);
    }
}
