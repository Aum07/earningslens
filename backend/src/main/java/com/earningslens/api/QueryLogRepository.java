package com.earningslens.api;

import com.earningslens.api.dto.ChatFilters;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes one row per chat request to query_log (spec 9.2, V1__init.sql). No IP addresses or other
 * personal data: only the question, the filters, which chunks were retrieved, the answer and the cost.
 */
@Repository
public class QueryLogRepository {

    /** One chat request, ready to be stored. */
    public record Entry(String question, ChatFilters appliedFilters, List<UUID> retrievedChunkIds, String answer,
                        boolean abstained, long latencyMs, Integer promptTokens, Integer completionTokens) {

        public Entry {
            retrievedChunkIds = List.copyOf(retrievedChunkIds);
        }
    }

    private static final String INSERT = """
            INSERT INTO query_log (id, question, applied_filters, retrieved_chunk_ids, answer, abstained,
                                   latency_ms, prompt_tokens, completion_tokens)
            VALUES (?, ?, CAST(? AS jsonb), CAST(? AS uuid[]), ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    public QueryLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Stores the entry and returns the new row's id. */
    public UUID insert(Entry entry) {
        UUID id = UUID.randomUUID();
        jdbc.update(INSERT,
                id,
                entry.question(),
                json.writeValueAsString(entry.appliedFilters()),
                uuidArrayLiteral(entry.retrievedChunkIds()),
                entry.answer(),
                entry.abstained(),
                (int) Math.min(entry.latencyMs(), Integer.MAX_VALUE),
                entry.promptTokens(),
                entry.completionTokens());
        return id;
    }

    /** Postgres array text form, for example {a1b2...,c3d4...}; UUIDs need no quoting. */
    static String uuidArrayLiteral(List<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
    }
}
