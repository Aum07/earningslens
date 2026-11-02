package com.earningslens.retrieval;

/**
 * A keyword (full-text) search result.
 *
 * @param score {@code ts_rank_cd} score: higher is better. Only comparable within one query;
 *              RRF (E3.3) uses the rank, not the score.
 * @param rank  1 for the best match, 2 for the next, and so on
 */
public record KeywordHit(RetrievedChunk chunk, double score, int rank) {
}
