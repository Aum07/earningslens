package com.earningslens.retrieval;

import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Roadmap E3.3: RRF with hand-calculated scores. No database, so this runs in CI.
 * Constant 60: rank 1 gives 1/61, rank 2 gives 1/62, rank 3 gives 1/63.
 */
class RrfFusionTest {

    private static final double EPS = 1e-12;

    // Fixed ids, so the tie-break by id is predictable: A < B < C < D.
    private static final RetrievedChunk A = chunk("00000000-0000-0000-0000-00000000000a");
    private static final RetrievedChunk B = chunk("00000000-0000-0000-0000-00000000000b");
    private static final RetrievedChunk C = chunk("00000000-0000-0000-0000-00000000000c");
    private static final RetrievedChunk D = chunk("00000000-0000-0000-0000-00000000000d");

    @Test
    void handCalculatedExample() {
        // vector:  A(1) B(2) C(3)       keyword: D(1) C(2) A(3)
        // A = 1/61 + 1/63 = 0.0322664...
        // C = 1/63 + 1/62 = 0.0320020...
        // D = 1/61        = 0.0163934...
        // B = 1/62        = 0.0161290...
        List<FusedHit> fused = RrfFusion.fuse(vectors(A, B, C), keywords(D, C, A), 60, 10);

        assertThat(fused).extracting(FusedHit::chunk).containsExactly(A, C, D, B);
        assertThat(fused).extracting(FusedHit::rank).containsExactly(1, 2, 3, 4);
        assertThat(fused.get(0).score()).isCloseTo(1.0 / 61 + 1.0 / 63, within(EPS));
        assertThat(fused.get(1).score()).isCloseTo(1.0 / 63 + 1.0 / 62, within(EPS));
        assertThat(fused.get(2).score()).isCloseTo(1.0 / 61, within(EPS));
        assertThat(fused.get(3).score()).isCloseTo(1.0 / 62, within(EPS));
    }

    @Test
    void chunkInBothListsGetsBothTermsAndKeepsBothRanks() {
        List<FusedHit> fused = RrfFusion.fuse(vectors(A), keywords(B, A), 60, 10);

        FusedHit a = fused.get(0);
        assertThat(a.chunk()).isEqualTo(A);
        assertThat(a.score()).isCloseTo(1.0 / 61 + 1.0 / 62, within(EPS));
        assertThat(a.vectorRank()).isEqualTo(1);
        assertThat(a.keywordRank()).isEqualTo(2);
        assertThat(a.vectorSimilarity()).isCloseTo(0.9, within(1e-9));   // distance 0.1 in vectors()
        assertThat(fused).hasSize(2);   // A is not counted twice
    }

    @Test
    void chunkOnlyInKeywordListHasNoVectorRankOrSimilarity() {
        FusedHit b = RrfFusion.fuse(vectors(A), keywords(B), 60, 10).stream()
                .filter(h -> h.chunk().equals(B)).findFirst().orElseThrow();

        assertThat(b.vectorRank()).isNull();
        assertThat(b.vectorSimilarity()).isNull();
        assertThat(b.keywordRank()).isEqualTo(1);
    }

    @Test
    void bothListsEmptyGivesEmptyResult() {
        assertThat(RrfFusion.fuse(List.of(), List.of(), 60, 10)).isEmpty();
    }

    @Test
    void oneListEmptyKeepsTheOtherListsOrder() {
        List<FusedHit> vectorOnly = RrfFusion.fuse(vectors(C, A, B), List.of(), 60, 10);
        assertThat(vectorOnly).extracting(FusedHit::chunk).containsExactly(C, A, B);
        assertThat(vectorOnly.get(2).score()).isCloseTo(1.0 / 63, within(EPS));

        List<FusedHit> keywordOnly = RrfFusion.fuse(List.of(), keywords(B, D), 60, 10);
        assertThat(keywordOnly).extracting(FusedHit::chunk).containsExactly(B, D);
    }

    @Test
    void equalScoresAreOrderedByBestRankThenId() {
        // B is vector rank 1, A is keyword rank 1: both 1/61, both best rank 1, so id decides: A first.
        assertThat(RrfFusion.fuse(vectors(B), keywords(A), 60, 10))
                .extracting(FusedHit::chunk).containsExactly(A, B);
        // Same input, same output, every time.
        assertThat(RrfFusion.fuse(vectors(B), keywords(A), 60, 10))
                .extracting(FusedHit::chunk).containsExactly(A, B);
    }

    @Test
    void limitKeepsOnlyTheTop() {
        List<FusedHit> fused = RrfFusion.fuse(vectors(A, B, C), keywords(D, C, A), 60, 2);

        assertThat(fused).extracting(FusedHit::chunk).containsExactly(A, C);
    }

    @Test
    void repeatedChunkInOneListCountsOnlyItsBestRank() {
        List<VectorHit> withRepeat = List.of(
                new VectorHit(A, 0.1, 1), new VectorHit(B, 0.2, 2), new VectorHit(A, 0.3, 3));

        List<FusedHit> fused = RrfFusion.fuse(withRepeat, List.of(), 60, 10);

        assertThat(fused).extracting(FusedHit::chunk).containsExactly(A, B);
        assertThat(fused.get(0).score()).isCloseTo(1.0 / 61, within(EPS));
        assertThat(fused.get(0).vectorRank()).isEqualTo(1);
    }

    @Test
    void usesTheGivenConstant() {
        List<FusedHit> fused = RrfFusion.fuse(vectors(A), keywords(A), 10, 10);

        assertThat(fused.get(0).score()).isCloseTo(2.0 / 11, within(EPS));
    }

    @Test
    void rejectsBadArguments() {
        assertThatThrownBy(() -> RrfFusion.fuse(List.of(), List.of(), 60, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RrfFusion.fuse(List.of(), List.of(), -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RrfFusion.fuse(null, List.of(), 60, 10))
                .isInstanceOf(NullPointerException.class);
    }

    // ---- helpers ----

    private static RetrievedChunk chunk(String id) {
        return new RetrievedChunk(UUID.fromString(id), UUID.randomUUID(), 0, "MSFT", 2026, 4,
                DocType.TRANSCRIPT, "content " + id, 10, null, null, null, null, false);
    }

    /** Vector hits in the given order, ranks 1..n, all with distance 0.1 (similarity 0.9). */
    private static List<VectorHit> vectors(RetrievedChunk... chunks) {
        List<VectorHit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.length; i++) {
            hits.add(new VectorHit(chunks[i], 0.1, i + 1));
        }
        return hits;
    }

    /** Keyword hits in the given order, ranks 1..n, made-up scores. */
    private static List<KeywordHit> keywords(RetrievedChunk... chunks) {
        List<KeywordHit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.length; i++) {
            hits.add(new KeywordHit(chunks[i], 1.0 / (i + 1), i + 1));
        }
        return hits;
    }
}
