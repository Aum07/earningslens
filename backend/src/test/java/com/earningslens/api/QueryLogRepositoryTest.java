package com.earningslens.api;

import com.earningslens.PgvectorContainerTest;
import com.earningslens.api.dto.ChatFilters;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap E4.5: one row per chat request in query_log, against the shared pgvector test container. */
class QueryLogRepositoryTest extends PgvectorContainerTest {

    @Autowired QueryLogRepository repository;

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM query_log");
    }

    @Test
    void storesOneRowWithFiltersChunkIdsAndTokenCounts() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ChatFilters filters = new ChatFilters(List.of("MSFT"), List.of(new FiscalPeriodDto(2026, 2)),
                List.of(DocType.TRANSCRIPT));

        UUID id = repository.insert(new QueryLogRepository.Entry("What was revenue?", filters, List.of(a, b),
                "Revenue was $81.3 billion [1].", false, 2140, 1400, 30));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT question, applied_filters::text AS filters, array_length(retrieved_chunk_ids, 1) AS chunks,
                       retrieved_chunk_ids[1]::text AS first_chunk, answer, abstained, latency_ms,
                       prompt_tokens, completion_tokens, created_at
                FROM query_log WHERE id = ?""", id);
        assertThat(row.get("question")).isEqualTo("What was revenue?");
        assertThat((String) row.get("filters"))
                .contains("\"tickers\": [\"MSFT\"]")
                .contains("\"year\": 2026")
                .contains("\"docTypes\": [\"TRANSCRIPT\"]");
        assertThat(row.get("chunks")).isEqualTo(2);
        assertThat(row.get("first_chunk")).isEqualTo(a.toString());
        assertThat(row.get("answer")).isEqualTo("Revenue was $81.3 billion [1].");
        assertThat(row.get("abstained")).isEqualTo(false);
        assertThat(row.get("latency_ms")).isEqualTo(2140);
        assertThat(row.get("prompt_tokens")).isEqualTo(1400);
        assertThat(row.get("completion_tokens")).isEqualTo(30);
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void abstentionWithoutModelCallStoresNullTokensAndEmptyChunkList() {
        UUID id = repository.insert(new QueryLogRepository.Entry("Revenue in FY2024 Q1?", ChatFilters.none(),
                List.of(), "Not in the corpus.", true, 12, null, null));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT cardinality(retrieved_chunk_ids) AS chunks, abstained, prompt_tokens
                FROM query_log WHERE id = ?""", id);
        assertThat(row.get("chunks")).isEqualTo(0);
        assertThat(row.get("abstained")).isEqualTo(true);
        assertThat(row.get("prompt_tokens")).isNull();
    }

    @Test
    void uuidArrayLiteral() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThat(QueryLogRepository.uuidArrayLiteral(List.of(a, a))).isEqualTo("{" + a + "," + a + "}");
        assertThat(QueryLogRepository.uuidArrayLiteral(List.of())).isEqualTo("{}");
    }
}
