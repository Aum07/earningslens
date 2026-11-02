package com.earningslens.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Spec 8 step 4: if two chosen chunks are neighbours in the same document and overlap,
 * keep the higher-scoring one.
 *
 * "Overlap" means near-duplicate: the text the two neighbours share must be at least
 * MIN_OVERLAP_FRACTION of the shorter chunk. The chunker copies up to 100 tokens of whole
 * sentences from the end of a chunk to the start of the next (about 1/6 of a 600-token chunk),
 * so ordinary neighbours are both kept: dropping one would throw away ~500 tokens of text that
 * only it contains. The first dev run showed exactly that (q021, q025 lost their gold chunk).
 *
 * The shared text is the longest end of the earlier chunk that appears in the later one
 * (whitespace collapsed first, because the copied sentences may be joined differently).
 */
public final class ChunkDeduper {

    /** Shortest shared end that counts as overlap at all. */
    static final int OVERLAP_PROBE_CHARS = 60;
    /** Shared text / shorter chunk at or above which the lower-ranked neighbour is dropped. */
    static final double MIN_OVERLAP_FRACTION = 0.5;

    private ChunkDeduper() {
    }

    /** Keeps the input order (best first) and drops a chunk if a better-ranked kept chunk nearly duplicates it. */
    public static List<FusedHit> dedupe(List<FusedHit> hits) {
        List<FusedHit> kept = new ArrayList<>();
        for (FusedHit hit : hits) {
            boolean duplicate = kept.stream().anyMatch(k -> overlappingNeighbours(k.chunk(), hit.chunk()));
            if (!duplicate) {
                kept.add(hit);
            }
        }
        return kept;
    }

    static boolean overlappingNeighbours(RetrievedChunk a, RetrievedChunk b) {
        if (!Objects.equals(a.documentId(), b.documentId()) || Math.abs(a.chunkIndex() - b.chunkIndex()) != 1) {
            return false;
        }
        RetrievedChunk earlier = a.chunkIndex() < b.chunkIndex() ? a : b;
        RetrievedChunk later = earlier == a ? b : a;
        String earlierText = collapse(earlier.content());
        String laterText = collapse(later.content());
        int shared = sharedChars(earlierText, laterText);
        int shorter = Math.min(earlierText.length(), laterText.length());
        return shared > 0 && shorter > 0 && shared >= MIN_OVERLAP_FRACTION * shorter;
    }

    /**
     * Length of the longest end of {@code earlier} that appears in {@code later}, or 0 if not even
     * the last OVERLAP_PROBE_CHARS characters do. Binary search: if an end of length n appears,
     * every shorter end appears too.
     */
    static int sharedChars(String earlier, String later) {
        if (earlier.length() < OVERLAP_PROBE_CHARS || !later.contains(tail(earlier, OVERLAP_PROBE_CHARS))) {
            return 0;
        }
        int low = OVERLAP_PROBE_CHARS;
        int high = earlier.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (later.contains(tail(earlier, mid))) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private static String tail(String text, int length) {
        return text.substring(text.length() - length);
    }

    private static String collapse(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }
}
