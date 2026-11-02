package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Reads the periods from the documents table (36 rows today, so no caching). */
@Repository
public class JdbcPeriodCatalog implements PeriodCatalog {

    private final JdbcTemplate jdbc;

    public JdbcPeriodCatalog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, NavigableSet<FiscalPeriod>> periodsByTicker() {
        Map<String, NavigableSet<FiscalPeriod>> result = new TreeMap<>();
        jdbc.query("SELECT DISTINCT ticker, fiscal_year, fiscal_quarter FROM documents", rs -> {
            result.computeIfAbsent(rs.getString("ticker"), t -> new TreeSet<>())
                    .add(new FiscalPeriod(rs.getInt("fiscal_year"), rs.getInt("fiscal_quarter")));
        });
        return result;
    }
}
