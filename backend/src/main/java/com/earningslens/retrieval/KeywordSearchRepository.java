package com.earningslens.retrieval;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Keyword (full-text) search over chunks (roadmap E3.2, spec section 8 step 3).
 *
 * Postgres turns the question into a search query with websearch_to_tsquery('english', ?):
 * stop words dropped, words stemmed ("expanding" becomes "expand"), "quoted text" kept as a
 * phrase. Chunks are ranked with ts_rank_cd against the stored tsv column (GIN index),
 * with the same optional filters as vector search. All values are bound parameters.
 *
 * OR instead of AND: websearch_to_tsquery joins words with AND, so a whole question
 * ("What was Azure revenue growth in Q1 FY2027?") would only match chunks containing every
 * word, which is usually none. We rewrite the top-level " & " to " | " so a chunk needs at
 * least one word, and ts_rank_cd ranks chunks with more of the words, closer together,
 * higher. Quoted phrases stay phrases (they use "<->", not "&").
 *
 * A leading minus ("-cloud") would mean NOT in websearch syntax; combined with OR that
 * would match almost every chunk, so the minus is removed before the query is built.
 */
@Repository
public class KeywordSearchRepository {

    private static final String SELECT = "SELECT " + ChunkSql.COLUMNS + """
            ,
                   ts_rank_cd(tsv, query.q) AS score
            FROM chunks,
                 (SELECT CAST(replace(CAST(websearch_to_tsquery('english', ?) AS text), ' & ', ' | ') AS tsquery) AS q) AS query
            """;

    /** A minus at the start of a word: websearch_to_tsquery would read it as NOT. */
    private static final Pattern LEADING_MINUS = Pattern.compile("(^|\\s)-+(?=\\S)");

    private final JdbcTemplate jdbc;

    public KeywordSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param question the user's question, as typed
     * @param filters  optional filters; {@link SearchFilters#none()} searches everything
     * @param limit    how many chunks to return at most (spec: 30 candidates before fusion)
     * @return best matches first, ranks 1..n; empty if no word of the question is in any chunk
     */
    public List<KeywordHit> search(String question, SearchFilters filters, int limit) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got " + limit);
        }
        if (filters == null) {
            filters = SearchFilters.none();
        }

        List<Object> args = new ArrayList<>();
        args.add(withoutNegation(question));
        List<String> conditions = new ArrayList<>();
        conditions.add("tsv @@ query.q");
        conditions.addAll(ChunkSql.filterConditions(filters, args));
        args.add(limit);
        // "id" breaks ties so equal scores always come back in the same order.
        String sql = SELECT + ChunkSql.where(conditions) + "ORDER BY score DESC, id\nLIMIT ?";

        List<Row> rows = jdbc.query(sql, ROW_MAPPER, args.toArray());

        List<KeywordHit> hits = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            hits.add(new KeywordHit(rows.get(i).chunk(), rows.get(i).score(), i + 1));
        }
        return hits;
    }

    static String withoutNegation(String question) {
        return LEADING_MINUS.matcher(question).replaceAll("$1");
    }

    private record Row(RetrievedChunk chunk, double score) {
    }

    private static final RowMapper<Row> ROW_MAPPER =
            (rs, rowNum) -> new Row(ChunkSql.mapChunk(rs), rs.getDouble("score"));
}
