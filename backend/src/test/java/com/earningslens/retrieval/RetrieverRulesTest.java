package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap E3.5: retriever rules that need no database (cap, interleave, explicit filters, dedupe). */
class RetrieverRulesTest {

    private static final UUID DOC = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final String SENTENCE = "Revenue was 77.7 billion dollars, up 18 percent, driven by Azure and cloud services.";

    @Test
    void capKeepsEveryCompanyNewestFirst() {
        List<PeriodTarget> six = List.of(
                t("GOOGL", 2026, 1), t("GOOGL", 2026, 2), t("GOOGL", 2026, 3),
                t("MSFT", 2026, 3), t("MSFT", 2026, 4), t("MSFT", 2027, 1));

        assertThat(Retriever.cap(six, 4)).containsExactly(
                t("GOOGL", 2026, 2), t("GOOGL", 2026, 3), t("MSFT", 2026, 4), t("MSFT", 2027, 1));
        assertThat(Retriever.cap(six, 3)).containsExactly(
                t("GOOGL", 2026, 2), t("GOOGL", 2026, 3), t("MSFT", 2027, 1));
        assertThat(Retriever.cap(six, 10)).isEqualTo(six);
    }

    @Test
    void capWithOneCompanyKeepsTheNewestQuarters() {
        List<PeriodTarget> five = List.of(t("META", 2025, 3), t("META", 2025, 4), t("META", 2026, 1),
                t("META", 2026, 2), t("META", 2026, 3));

        assertThat(Retriever.cap(five, 4)).containsExactly(
                t("META", 2025, 4), t("META", 2026, 1), t("META", 2026, 2), t("META", 2026, 3));
    }

    @Test
    void interleaveTakesTurnsAndRenumbers() {
        RetrievedChunk a1 = chunk(0, "a1"), a2 = chunk(1, "a2"), b1 = chunk(5, "b1");
        List<FusedHit> out = Retriever.interleave(List.of(List.of(hit(a1, 1), hit(a2, 2)), List.of(hit(b1, 1))));

        assertThat(out).extracting(h -> h.chunk().content()).containsExactly("a1", "b1", "a2");
        assertThat(out).extracting(FusedHit::rank).containsExactly(1, 2, 3);
    }

    @Test
    void explicitTickersAndPeriodsReplaceParsedOnes() {
        ParsedQuestion parsed = new ParsedQuestion(Set.of("MSFT"),
                List.of(new PeriodRef.Fiscal(new FiscalPeriod(2026, 3)), new PeriodRef.Latest(1)));

        ParsedQuestion both = Retriever.withExplicitFilters(parsed,
                SearchFilters.tickers("GOOGL").withPeriods(new FiscalPeriod(2026, 1)));
        assertThat(both.tickers()).containsExactly("GOOGL");
        assertThat(both.periodRefs()).containsExactly(new PeriodRef.Fiscal(new FiscalPeriod(2026, 1)));

        ParsedQuestion onlyDocTypes = Retriever.withExplicitFilters(parsed, SearchFilters.none().withDocTypes(DocType.TRANSCRIPT));
        assertThat(onlyDocTypes).isEqualTo(parsed);
    }

    @Test
    void dedupeDropsTheLowerRankedOfTwoNearDuplicateNeighbours() {
        // They share SENTENCE (85 characters), well over half of the shorter chunk.
        RetrievedChunk first = chunk(4, "Intro text. " + SENTENCE);
        RetrievedChunk second = chunk(5, SENTENCE + "\n\nA little new text.");

        // The later chunk ranks higher: it is kept, the earlier one dropped.
        assertThat(ChunkDeduper.dedupe(List.of(hit(second, 1), hit(first, 2))))
                .extracting(FusedHit::chunk).containsExactly(second);
    }

    @Test
    void dedupeKeepsOrdinaryNeighboursThatShareOnlyTheChunkerOverlap() {
        // Like real chunks: about 1/6 shared, the rest unique to each chunk (dev q021, q025).
        String uniqueA = "Unique text of the earlier chunk. ".repeat(12);
        String uniqueB = "Unique text of the later chunk, with the gold quote. ".repeat(8);
        RetrievedChunk first = chunk(4, "[META | FY2026 Q3 | TRANSCRIPT]\n" + uniqueA + SENTENCE);
        RetrievedChunk second = chunk(5, "[META | FY2026 Q3 | TRANSCRIPT]\n" + SENTENCE + " " + uniqueB);

        assertThat(ChunkDeduper.sharedChars(
                (uniqueA + SENTENCE).strip(), (SENTENCE + " " + uniqueB).strip())).isEqualTo(SENTENCE.length());
        assertThat(ChunkDeduper.dedupe(List.of(hit(first, 1), hit(second, 2)))).hasSize(2);
    }

    @Test
    void sharedCharsIsTheLongestEndOfTheEarlierChunkFoundInTheLaterOne() {
        String end = "x".repeat(70);
        // The 70 x's appear in the later chunk; " " + 70 x's does not.
        assertThat(ChunkDeduper.sharedChars("abc " + end, end + " def")).isEqualTo(70);
        // Shorter than the 60-character probe: no overlap at all.
        assertThat(ChunkDeduper.sharedChars("short", "short")).isZero();
    }

    @Test
    void dedupeIgnoresWhitespaceDifferencesInTheOverlap() {
        RetrievedChunk first = chunk(4, "Intro. " + SENTENCE.replace(" driven", "\n\ndriven"));
        RetrievedChunk second = chunk(5, "Header\n" + SENTENCE + " Then more.");

        assertThat(ChunkDeduper.overlappingNeighbours(first, second)).isTrue();
    }

    @Test
    void dedupeKeepsChunksThatAreNotOverlappingNeighbours() {
        RetrievedChunk c4 = chunk(4, "Intro. " + SENTENCE);
        RetrievedChunk c6 = chunk(6, SENTENCE + " Not a neighbour: index 6.");
        RetrievedChunk c5noOverlap = chunk(5, "A table or a new section with no copied sentences at all, long enough to compare.");
        RetrievedChunk otherDoc = new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), 5, "MSFT", 2026, 2,
                DocType.PRESS_RELEASE, SENTENCE + " Other document.", 10, null, null, null, null, false);

        assertThat(ChunkDeduper.dedupe(List.of(hit(c4, 1), hit(c6, 2), hit(c5noOverlap, 3), hit(otherDoc, 4))))
                .hasSize(4);
    }

    // ---- helpers ----

    private static PeriodTarget t(String ticker, int year, int quarter) {
        return new PeriodTarget(ticker, new FiscalPeriod(year, quarter));
    }

    private static RetrievedChunk chunk(int index, String content) {
        return new RetrievedChunk(UUID.randomUUID(), DOC, index, "MSFT", 2026, 2, DocType.PRESS_RELEASE,
                content, 10, null, null, null, null, false);
    }

    private static FusedHit hit(RetrievedChunk chunk, int rank) {
        return new FusedHit(chunk, 1.0 / (60 + rank), rank, rank, null, 0.8);
    }
}
