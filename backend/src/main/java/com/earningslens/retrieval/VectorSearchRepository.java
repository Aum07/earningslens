package com.earningslens.retrieval;

import com.earningslens.ingest.DocumentStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Vector search over chunks (roadmap E3.1, spec section 8 step 3).
 *
 * Orders by cosine distance ({@code embedding <=> query}) with optional WHERE filters on
 * ticker, fiscal period and doc type. Only fixed SQL pieces are joined together; every
 * value is a bound "?" parameter.
 *
 * Why "SET LOCAL hnsw.iterative_scan": by default the HNSW index hands back only about 40
 * nearest rows and Postgres applies the WHERE filter afterwards, so a filter such as
 * "MSFT, FY2026 Q4" could leave far fewer than the requested rows. With iterative scan
 * (pgvector 0.8+) the index keeps searching until enough rows pass the filter.
 * "strict_order" keeps the results exactly sorted by distance. SET LOCAL only lasts for
 * this transaction, which is why the method is @Transactional.
 */
@Repository
public class VectorSearchRepository {

    private static final String SELECT = "SELECT " + ChunkSql.COLUMNS + """
            ,
                   embedding <=> CAST(? AS vector) AS distance
            FROM chunks
            """;

    private final JdbcTemplate jdbc;

    public VectorSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param queryEmbedding the question's embedding (same model and size as the stored chunks)
     * @param filters        optional filters; {@link SearchFilters#none()} searches everything
     * @param limit          how many chunks to return at most (spec: 30 candidates before fusion)
     * @return nearest chunks first, ranks 1..n
     */
    @Transactional(readOnly = true)
    public List<VectorHit> search(float[] queryEmbedding, SearchFilters filters, int limit) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("query embedding is empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got " + limit);
        }
        if (filters == null) {
            filters = SearchFilters.none();
        }

        jdbc.execute("SET LOCAL hnsw.iterative_scan = strict_order");

        List<Object> args = new ArrayList<>();
        args.add(DocumentStore.toVectorLiteral(queryEmbedding));
        String where = ChunkSql.where(ChunkSql.filterConditions(filters, args));
        args.add(limit);
        // ORDER BY the alias, so the distance expression (and its parameter) appears only once
        // and pgvector can use the HNSW index for the ordering.
        String sql = SELECT + where + "ORDER BY distance\nLIMIT ?";

        List<Row> rows = jdbc.query(sql, ROW_MAPPER, args.toArray());

        List<VectorHit> hits = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            hits.add(new VectorHit(rows.get(i).chunk(), rows.get(i).distance(), i + 1));
        }
        return hits;
    }

    private record Row(RetrievedChunk chunk, double distance) {
    }

    private static final RowMapper<Row> ROW_MAPPER =
            (rs, rowNum) -> new Row(ChunkSql.mapChunk(rs), rs.getDouble("distance"));
}
