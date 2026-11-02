package com.earningslens.retrieval;

/**
 * A vector search result.
 *
 * @param distance cosine distance from pgvector's {@code <=>}: 0 = same direction, 1 = unrelated, 2 = opposite
 * @param rank     1 for the nearest chunk, 2 for the next, and so on (RRF in E3.3 uses this)
 */
public record VectorHit(RetrievedChunk chunk, double distance, int rank) {

    /** Cosine similarity (1 - distance). The evidence threshold in E3.7 compares this to min-similarity. */
    public double similarity() {
        return 1.0 - distance;
    }
}
