package com.earningslens.retrieval;

/**
 * A chunk after reciprocal rank fusion (RRF) of the vector and keyword lists.
 *
 * @param score            sum over both lists of 1 / (rrfK + rank); higher is better
 * @param rank             1 for the best fused chunk, 2 for the next, and so on
 * @param vectorRank       rank in the vector list, or null if the chunk was not in it
 * @param keywordRank      rank in the keyword list, or null if the chunk was not in it
 * @param vectorSimilarity cosine similarity from vector search, or null if not in that list
 */
public record FusedHit(RetrievedChunk chunk, double score, int rank,
                       Integer vectorRank, Integer keywordRank, Double vectorSimilarity) {
}
