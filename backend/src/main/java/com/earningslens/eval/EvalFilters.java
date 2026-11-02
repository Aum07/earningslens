package com.earningslens.eval;

import com.earningslens.ingest.DocType;

import java.util.List;

/**
 * Explicit filters for an eval item. Same shape as "filters" in the chat API request
 * (spec 10), so an item can be sent to the API unchanged. Empty list = no filter.
 */
public record EvalFilters(List<String> tickers, List<FiscalPeriod> fiscalPeriods, List<DocType> docTypes) {

    public EvalFilters {
        tickers = tickers == null ? List.of() : List.copyOf(tickers);
        fiscalPeriods = fiscalPeriods == null ? List.of() : List.copyOf(fiscalPeriods);
        docTypes = docTypes == null ? List.of() : List.copyOf(docTypes);
    }
}
