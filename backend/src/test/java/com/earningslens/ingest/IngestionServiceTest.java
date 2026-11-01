package com.earningslens.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the whole pipeline on the small HTML fixture against the real local database,
 * with a fake EmbeddingService (no OpenAI calls, no cost).
 *
 * @Transactional on a test class: every test runs in a transaction that is rolled back
 * at the end, so the test leaves nothing behind in your database.
 * @MockitoBean replaces the real EmbeddingService bean with a Mockito mock.
 */
@SpringBootTest
@Transactional
class IngestionServiceTest {

    private static final ManifestEntry ENTRY = new ManifestEntry("TEST", "Test Company Inc.", 2026, 1,
            DocType.FORM_10Q, "https://example.com/test-10q.htm", DocumentFormat.HTML);

    @Autowired IngestionService ingestion;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean EmbeddingService embeddingService;

    @Test
    void firstRunStoresTheDocumentAndChunksSecondRunInsertsNothing() throws Exception {
        when(embeddingService.embed(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(text -> new float[1536]).toList();
        });
        Path fixture = fixture();

        IngestResult first = ingestion.ingest(ENTRY, fixture);
        IngestResult second = ingestion.ingest(ENTRY, fixture);

        assertThat(first.inserted()).isTrue();
        assertThat(first.chunks()).isPositive();
        assertThat(second.inserted()).isFalse();
        verify(embeddingService, times(1)).embed(anyList());   // no OpenAI call for the duplicate

        assertThat(count("SELECT count(*) FROM documents WHERE ticker = 'TEST'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM chunks WHERE ticker = 'TEST'")).isEqualTo(first.chunks());
        assertThat(count("SELECT count(*) FROM chunks WHERE ticker = 'TEST' AND vector_dims(embedding) = 1536"))
                .isEqualTo(first.chunks());
        assertThat(count("""
                SELECT count(*) FROM chunks
                WHERE ticker = 'TEST' AND content LIKE '[TEST | FY2026 Q1 | FORM_10Q]%'
                  AND tsv IS NOT NULL AND forward_looking = false""")).isEqualTo(first.chunks());
    }

    @Test
    void sha256IsLowercaseHexOf64Characters() {
        assertThat(IngestionService.sha256("abc".getBytes()))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void vectorLiteralIsWhatPgvectorExpects() {
        assertThat(DocumentStore.toVectorLiteral(new float[] {0.5f, -1.0f, 2.25f})).isEqualTo("[0.5,-1.0,2.25]");
    }

    private int count(String sql) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Integer.class));
    }

    private static Path fixture() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(
                IngestionServiceTest.class.getResource("/fixtures/sample-filing.html")).toURI());
    }
}
