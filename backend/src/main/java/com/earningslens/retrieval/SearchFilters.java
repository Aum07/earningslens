package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;

import java.util.Set;

/**
 * Optional filters for vector and keyword search (spec section 8).
 * An empty set means "no filter" for that field. Tickers are matched exactly (use canonical
 * tickers such as "MSFT"; the query parser in E3.4 maps names and aliases to them).
 */
public record SearchFilters(Set<String> tickers, Set<FiscalPeriod> periods, Set<DocType> docTypes) {

    private static final SearchFilters NONE = new SearchFilters(Set.of(), Set.of(), Set.of());

    public SearchFilters {
        tickers = tickers == null ? Set.of() : Set.copyOf(tickers);
        periods = periods == null ? Set.of() : Set.copyOf(periods);
        docTypes = docTypes == null ? Set.of() : Set.copyOf(docTypes);
    }

    public static SearchFilters none() {
        return NONE;
    }

    public static SearchFilters tickers(String... tickers) {
        return new SearchFilters(Set.of(tickers), Set.of(), Set.of());
    }

    public SearchFilters withPeriods(FiscalPeriod... periods) {
        return new SearchFilters(tickers, Set.of(periods), docTypes);
    }

    public SearchFilters withDocTypes(DocType... docTypes) {
        return new SearchFilters(tickers, periods, Set.of(docTypes));
    }
}
