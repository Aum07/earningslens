package com.earningslens;

import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;
import com.earningslens.ingest.DocumentStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for tests that need their own pgvector database (Testcontainers).
 *
 * One container for the whole test run: it is started once, here, and every subclass shares
 * it and the same Spring context. Testcontainers removes it when the JVM exits.
 * @ServiceConnection points the app's datasource (and Flyway) at the container, so V1 and V2
 * run there. Your local earningslens database and its corpus are never touched.
 * Needs Docker Desktop running.
 */
@SpringBootTest
public abstract class PgvectorContainerTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg16");

    static {
        POSTGRES.start();
    }

    protected static final int DIMS = 1536;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Deletes all documents and chunks, after checking we are connected to the container. */
    protected void clearDatabase() throws SQLException {
        try (Connection c = jdbc.getDataSource().getConnection()) {
            assertThat(c.getMetaData().getURL())
                    .as("test must run against the Testcontainers database")
                    .contains(":" + POSTGRES.getMappedPort(5432) + "/");
        }
        jdbc.update("DELETE FROM documents");   // chunks go too (ON DELETE CASCADE)
    }

    /** A 1536-number vector with the given first values and zeros after them. */
    protected static float[] vector(float... head) {
        float[] v = new float[DIMS];
        System.arraycopy(head, 0, v, 0, head.length);
        return v;
    }

    /** One document with one chunk: no pages, not forward-looking. */
    protected UUID insertChunk(String ticker, FiscalPeriod period, DocType docType, String content, float[] embedding) {
        return insertChunk(ticker, period, docType, content, embedding, null, null, false);
    }

    /** One document with one chunk. Returns the chunk id. */
    protected UUID insertChunk(String ticker, FiscalPeriod period, DocType docType, String content, float[] embedding,
                               Integer pageStart, Integer pageEnd, boolean forwardLooking) {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String sha = (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");   // 64 hex chars
        jdbc.update("""
                INSERT INTO documents (id, ticker, company_name, fiscal_year, fiscal_quarter, doc_type,
                                       source_url, content_sha256)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, documentId, ticker, ticker + " Inc.", period.year(), period.quarter(), docType.name(),
                "https://example.com/" + documentId, sha);
        jdbc.update("""
                INSERT INTO chunks (id, document_id, chunk_index, ticker, fiscal_year, fiscal_quarter, doc_type,
                                    content, token_count, page_start, page_end, section, speaker, embedding,
                                    forward_looking)
                VALUES (?, ?, 0, ?, ?, ?, ?, ?, 42, CAST(? AS int), CAST(? AS int), ?, NULL, CAST(? AS vector), ?)
                """, chunkId, documentId, ticker, period.year(), period.quarter(), docType.name(),
                content, pageStart, pageEnd, "Section for " + content,
                DocumentStore.toVectorLiteral(embedding), forwardLooking);
        return chunkId;
    }
}
