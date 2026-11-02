package com.earningslens.retrieval;

import java.util.List;
import java.util.Set;

/** What QueryParser found in a question: company tickers and period references, in question order. */
public record ParsedQuestion(Set<String> tickers, List<PeriodRef> periodRefs) {

    public ParsedQuestion {
        tickers = Set.copyOf(tickers);
        periodRefs = List.copyOf(periodRefs);
    }
}
