package com.earningslens.ingest;

import com.earningslens.document.DocumentEntity;
import com.earningslens.document.DocumentRepository;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

/**
 * Saves one document and all its chunks in a single database transaction (spec 7.2 step 6):
 * either everything is stored, or nothing is.
 *
 * SPRING CONCEPT, @Transactional: Spring opens a transaction before the method and commits
 * after it. If the method throws, everything is rolled back. The JPA save and the
 * JdbcTemplate inserts share that same transaction.
 */
@Component
public class DocumentStore {

    private static final String INSERT_CHUNK = """
            INSERT INTO chunks (id, document_id, chunk_index, ticker, fiscal_year, fiscal_quarter, doc_type,
                                content, token_count, page_start, page_end, section, speaker, embedding,
                                forward_looking)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?)
            """;

    private final DocumentRepository documents;
    private final JdbcTemplate jdbc;

    public DocumentStore(DocumentRepository documents, JdbcTemplate jdbc) {
        this.documents = documents;
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID store(ManifestEntry entry, String contentSha256, List<Chunk> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("%d chunks but %d vectors".formatted(chunks.size(), vectors.size()));
        }
        UUID documentId = UUID.randomUUID();
        // saveAndFlush writes the row now, so the chunks' foreign key can point to it.
        documents.saveAndFlush(new DocumentEntity(documentId, entry.ticker(), entry.companyName(),
                entry.fiscalYear(), entry.fiscalQuarter(), entry.docType(), entry.url(), contentSha256));

        // One batched INSERT for all chunks: far fewer round trips than one call per chunk.
        jdbc.batchUpdate(INSERT_CHUNK, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Chunk chunk = chunks.get(i);
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, documentId);
                ps.setInt(3, chunk.chunkIndex());
                ps.setString(4, entry.ticker());
                ps.setInt(5, entry.fiscalYear());
                ps.setShort(6, (short) entry.fiscalQuarter());
                ps.setString(7, entry.docType().name());
                ps.setString(8, chunk.content());
                ps.setInt(9, chunk.tokenCount());
                ps.setObject(10, chunk.pageStart(), Types.INTEGER);
                ps.setObject(11, chunk.pageEnd(), Types.INTEGER);
                ps.setString(12, chunk.section());
                ps.setString(13, chunk.speaker());
                ps.setString(14, toVectorLiteral(vectors.get(i)));
                ps.setBoolean(15, chunk.forwardLooking());
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
        return documentId;
    }

    /** pgvector reads vectors written as text: "[0.1,0.2,...]". */
    static String toVectorLiteral(float[] vector) {
        StringBuilder text = new StringBuilder(vector.length * 12).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(vector[i]);
        }
        return text.append(']').toString();
    }
}
