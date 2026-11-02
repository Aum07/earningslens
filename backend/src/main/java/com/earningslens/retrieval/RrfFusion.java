package com.earningslens.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Reciprocal rank fusion (roadmap E3.3, spec section 8 step 3).
 *
 * score(chunk) = sum over the lists it appears in of 1 / (rrfK + rank), with rrfK = 60 from
 * config. Only ranks are used, never the raw scores, because cosine distance and ts_rank_cd
 * are on different scales. A chunk found by both searches gets two terms, so it usually
 * beats a chunk found by only one.
 *
 * Ties (equal scores) are ordered by the chunk's best single rank, then by chunk id, so the
 * output is always the same for the same input. A pure function: no database, no Spring.
 */
public final class RrfFusion {

    private RrfFusion() {
    }

    /**
     * @param vectorHits  vector results, ranks 1..n (may be empty, for example in vector-off ablations)
     * @param keywordHits keyword results, ranks 1..n (may be empty)
     * @param rrfK        the RRF constant (60 in the spec)
     * @param limit       how many fused chunks to return at most
     * @return best fused chunks first, ranks 1..n
     */
    public static List<FusedHit> fuse(List<VectorHit> vectorHits, List<KeywordHit> keywordHits, int rrfK, int limit) {
        Objects.requireNonNull(vectorHits, "vectorHits");
        Objects.requireNonNull(keywordHits, "keywordHits");
        if (rrfK < 0) {
            throw new IllegalArgumentException("rrfK must not be negative, got " + rrfK);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got " + limit);
        }

        // LinkedHashMap: one entry per chunk id. If a list repeats a chunk, only its first (best) rank counts.
        Map<UUID, Entry> byChunk = new LinkedHashMap<>();
        for (VectorHit hit : vectorHits) {
            Entry e = byChunk.computeIfAbsent(hit.chunk().id(), id -> new Entry(hit.chunk()));
            if (e.vectorRank == null) {
                e.vectorRank = hit.rank();
                e.vectorSimilarity = hit.similarity();
                e.score += 1.0 / (rrfK + hit.rank());
            }
        }
        for (KeywordHit hit : keywordHits) {
            Entry e = byChunk.computeIfAbsent(hit.chunk().id(), id -> new Entry(hit.chunk()));
            if (e.keywordRank == null) {
                e.keywordRank = hit.rank();
                e.score += 1.0 / (rrfK + hit.rank());
            }
        }

        List<Entry> sorted = new ArrayList<>(byChunk.values());
        sorted.sort(Comparator.comparingDouble((Entry e) -> e.score).reversed()
                .thenComparingInt(Entry::bestRank)
                .thenComparing(e -> e.chunk.id()));

        List<FusedHit> fused = new ArrayList<>(Math.min(limit, sorted.size()));
        for (int i = 0; i < sorted.size() && i < limit; i++) {
            Entry e = sorted.get(i);
            fused.add(new FusedHit(e.chunk, e.score, i + 1, e.vectorRank, e.keywordRank, e.vectorSimilarity));
        }
        return fused;
    }

    /** Running totals for one chunk while fusing. */
    private static final class Entry {
        final RetrievedChunk chunk;
        double score;
        Integer vectorRank;
        Integer keywordRank;
        Double vectorSimilarity;

        Entry(RetrievedChunk chunk) {
            this.chunk = chunk;
        }

        int bestRank() {
            int v = vectorRank == null ? Integer.MAX_VALUE : vectorRank;
            int k = keywordRank == null ? Integer.MAX_VALUE : keywordRank;
            return Math.min(v, k);
        }
    }
}
