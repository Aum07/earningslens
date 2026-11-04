package com.earningslens.api;

import com.earningslens.api.dto.CompanyDto;
import com.earningslens.api.dto.DocumentDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.Company;
import com.earningslens.retrieval.CompanyDirectory;
import com.earningslens.retrieval.PeriodCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;

/**
 * What is in the corpus (roadmap E4.6): the documents with their chunk counts, and the companies with
 * their quarters. Read straight from the database on each call: 36 documents, so no caching needed.
 */
@Service
public class CorpusService {

    private static final String DOCUMENTS = """
            SELECT d.id, d.ticker, d.company_name, d.fiscal_year, d.fiscal_quarter, d.doc_type, d.source_url,
                   COUNT(c.id) AS chunk_count
            FROM documents d
            LEFT JOIN chunks c ON c.document_id = d.id
            GROUP BY d.id
            ORDER BY d.ticker, d.fiscal_year, d.fiscal_quarter, d.doc_type
            """;

    private final JdbcTemplate jdbc;
    private final CompanyDirectory companies;
    private final PeriodCatalog periods;

    public CorpusService(JdbcTemplate jdbc, CompanyDirectory companies, PeriodCatalog periods) {
        this.jdbc = jdbc;
        this.companies = companies;
        this.periods = periods;
    }

    /** Every document, sorted by ticker, then quarter (oldest first), then document type. */
    public List<DocumentDto> documents() {
        return jdbc.query(DOCUMENTS, (rs, row) -> new DocumentDto(
                rs.getObject("id", UUID.class),
                rs.getString("ticker"),
                rs.getString("company_name"),
                rs.getInt("fiscal_year"),
                rs.getInt("fiscal_quarter"),
                DocType.valueOf(rs.getString("doc_type")),
                rs.getString("source_url"),
                rs.getInt("chunk_count")));
    }

    /** Companies from companies.yaml that have at least one quarter in the corpus, in the file's order. */
    public List<CompanyDto> companies() {
        Map<String, NavigableSet<FiscalPeriod>> byTicker = periods.periodsByTicker();
        List<CompanyDto> result = new ArrayList<>();
        for (Company company : companies.all()) {
            NavigableSet<FiscalPeriod> held = byTicker.get(company.ticker());
            if (held == null || held.isEmpty()) {
                continue;
            }
            List<FiscalPeriodDto> periodDtos = held.stream()
                    .map(p -> new FiscalPeriodDto(p.year(), p.quarter()))
                    .toList();
            result.add(new CompanyDto(company.ticker(), company.name(), company.fiscalYearEndMonth(), periodDtos));
        }
        return result;
    }
}
