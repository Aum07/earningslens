package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SQL pieces shared by vector and keyword search: the chunk columns, the filter conditions
 * and the row-to-RetrievedChunk mapping. Only fixed SQL text is joined; values go into args.
 */
final class ChunkSql {

    /** Every chunks column except embedding and tsv, in the order mapChunk reads them. */
    static final String COLUMNS = """
            id, document_id, chunk_index, ticker, fiscal_year, fiscal_quarter, doc_type,
            content, token_count, page_start, page_end, section, speaker, forward_looking""";

    private ChunkSql() {
    }

    /** One SQL condition per non-empty filter; adds the matching values to args, in order. */
    static List<String> filterConditions(SearchFilters filters, List<Object> args) {
        List<String> conditions = new ArrayList<>();

        if (!filters.tickers().isEmpty()) {
            List<String> tickers = filters.tickers().stream().sorted().toList();
            conditions.add("ticker IN (" + placeholders(tickers.size(), "?") + ")");
            args.addAll(tickers);
        }
        if (!filters.periods().isEmpty()) {
            List<FiscalPeriod> periods = filters.periods().stream().sorted().toList();
            conditions.add("(fiscal_year, fiscal_quarter) IN (" + placeholders(periods.size(), "(?, ?)") + ")");
            for (FiscalPeriod p : periods) {
                args.add(p.year());
                args.add(p.quarter());
            }
        }
        if (!filters.docTypes().isEmpty()) {
            List<String> types = filters.docTypes().stream().map(DocType::name).sorted().toList();
            conditions.add("doc_type IN (" + placeholders(types.size(), "?") + ")");
            args.addAll(types);
        }
        return conditions;
    }

    /** "WHERE a\n  AND b\n", or "" when there are no conditions. */
    static String where(List<String> conditions) {
        return conditions.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", conditions) + "\n";
    }

    static RetrievedChunk mapChunk(ResultSet rs) throws SQLException {
        return new RetrievedChunk(
                rs.getObject("id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getInt("chunk_index"),
                rs.getString("ticker"),
                rs.getInt("fiscal_year"),
                rs.getInt("fiscal_quarter"),
                DocType.valueOf(rs.getString("doc_type")),
                rs.getString("content"),
                rs.getInt("token_count"),
                rs.getObject("page_start", Integer.class),
                rs.getObject("page_end", Integer.class),
                rs.getString("section"),
                rs.getString("speaker"),
                rs.getBoolean("forward_looking"));
    }

    private static String placeholders(int count, String one) {
        return String.join(", ", Collections.nCopies(count, one));
    }
}
