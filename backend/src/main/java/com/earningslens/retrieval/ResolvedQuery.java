package com.earningslens.retrieval;

import java.util.List;
import java.util.Set;

/**
 * The question's filters after period resolution.
 *
 * @param tickers         companies named in the question (empty = all companies)
 * @param targets         company + quarter pairs to search, all present in the corpus, sorted
 * @param missing         explicitly asked-for pairs that are not in the corpus, sorted
 * @param periodMentioned true if the question referred to any period at all
 */
public record ResolvedQuery(Set<String> tickers, List<PeriodTarget> targets, List<PeriodTarget> missing,
                            boolean periodMentioned) {

    public ResolvedQuery {
        tickers = Set.copyOf(tickers);
        targets = List.copyOf(targets);
        missing = List.copyOf(missing);
    }

    /** True when periods were asked for but none of them is in the corpus: answer "not in the corpus". */
    public boolean onlyMissingPeriods() {
        return periodMentioned && targets.isEmpty() && !missing.isEmpty();
    }
}
