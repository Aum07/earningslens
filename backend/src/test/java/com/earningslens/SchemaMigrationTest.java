package com.earningslens;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that Flyway applied V1__init.sql: tables, extension and indexes exist.
 * Needs the local Docker database running (docker compose up -d).
 */
@SpringBootTest
class SchemaMigrationTest {

    // JdbcTemplate runs plain SQL. Spring creates it from the datasource settings.
    @Autowired JdbcTemplate jdbc;

    @Test
    void vectorExtensionIsInstalled() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void allTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);
        assertThat(tables).contains("documents", "chunks", "query_log");
    }

    @Test
    void chunksHasHnswAndGinIndexes() {
        // pg_indexes.indexdef holds the CREATE INDEX statement, including the index method.
        List<String> defs = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'chunks'", String.class);
        assertThat(defs).anyMatch(d -> d.contains("chunks_embedding_hnsw") && d.contains("USING hnsw"));
        assertThat(defs).anyMatch(d -> d.contains("chunks_tsv_gin") && d.contains("USING gin"));
        assertThat(defs).anyMatch(d -> d.contains("chunks_filter_idx"));
    }

    @Test
    void v1IsRecordedByFlyway() {
        Boolean success = jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);
        assertThat(success).isTrue();
    }
}
