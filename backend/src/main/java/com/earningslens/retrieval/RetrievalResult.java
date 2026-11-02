package com.earningslens.retrieval;

import java.util.List;

/**
 * What the retriever found for one question.
 *
 * @param chunks          the chosen chunks, best first, ranks 1..n (generation cites them as [1], [2], ...)
 * @param searchedTargets company + quarter pairs searched one by one (PER_PERIOD), or the single pair used as a filter
 * @param skippedTargets  pairs left out because of the max-periods cap
 * @param missing         explicitly asked-for pairs that are not in the corpus
 * @param bestSimilarity  highest cosine similarity of any vector hit, or null if nothing was searched or found
 * @param abstainReason   why generation should not be called, or null to go ahead
 */
public record RetrievalResult(RetrievalMode mode, Strategy strategy, List<FusedHit> chunks,
                              List<PeriodTarget> searchedTargets, List<PeriodTarget> skippedTargets,
                              List<PeriodTarget> missing, Double bestSimilarity, AbstainReason abstainReason) {

    public enum Strategy {
        /** One hybrid (or vector) search with k results. */
        SINGLE,
        /** One search per company + quarter, k-per-period results each. */
        PER_PERIOD,
        /** No search was run (abstained before searching). */
        NONE
    }

    public enum AbstainReason {
        /** Every period the question asked for is outside the corpus. */
        PERIOD_NOT_IN_CORPUS,
        /** Best vector similarity below earningslens.retrieval.min-similarity (mode D only). */
        LOW_SIMILARITY
    }

    public RetrievalResult {
        chunks = List.copyOf(chunks);
        searchedTargets = List.copyOf(searchedTargets);
        skippedTargets = List.copyOf(skippedTargets);
        missing = List.copyOf(missing);
    }

    public boolean abstained() {
        return abstainReason != null;
    }
}
