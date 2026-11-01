package com.earningslens.ingest;

import com.earningslens.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests the five rules the roadmap asks for, plus page ranges, groups and tables. */
class ChunkerTest {

    private static final ChunkingProperties SIZES = new ChunkingProperties(600, 100, 900);
    private static final String FILING_HEADER = "[MSFT | FY2026 Q2 | FORM_10Q]";
    private static final String TRANSCRIPT_HEADER = "[MSFT | FY2026 Q2 | TRANSCRIPT]";

    private final TokenCounter tokens = new TokenCounter();
    private final Chunker chunker = new Chunker(tokens, SIZES);

    @Test
    void noChunkIsOverTheMaximumEvenWithAHugeSentenceAndTable() {
        List<CleanedBlock> blocks = new ArrayList<>(paragraphs("Overview", 20));
        blocks.add(text("Overview", "word ".repeat(2000).strip()));                 // one 2000-word "sentence"
        blocks.add(new CleanedBlock("Overview", null, BlockKind.TABLE, tableRows(400), false));

        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), blocks);

        assertThat(chunks).isNotEmpty().allSatisfy(chunk -> {
            assertThat(chunk.tokenCount()).isLessThanOrEqualTo(900);
            assertThat(chunk.tokenCount()).isEqualTo(tokens.count(chunk.content()));
        });
    }

    @Test
    void everyChunkStartsWithTheHeaderLine() {
        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), paragraphs("Overview", 20));

        assertThat(chunks).hasSizeGreaterThan(2)
                .allSatisfy(chunk -> assertThat(chunk.content()).startsWith(FILING_HEADER + "\n"));
    }

    @Test
    void chunksAreFilledCloseToTheTargetSize() {
        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), paragraphs("Overview", 20));

        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).tokenCount()).as("chunk " + i).isBetween(400, 600);
        }
    }

    @Test
    void noChunkEndsMidSentenceInNormalText() {
        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), paragraphs("Overview", 20));

        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content().strip()).endsWith("."));
    }

    @Test
    void consecutiveChunksOverlap() {
        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), paragraphs("Overview", 20));

        for (int i = 1; i < chunks.size(); i++) {
            String fixedLines = FILING_HEADER + "\nSection: Overview\n";
            String body = chunks.get(i).content().substring(fixedLines.length());
            String firstSentence = body.substring(0, body.indexOf('.') + 1);
            assertThat(chunks.get(i - 1).content()).as("chunk " + (i - 1) + " contains start of chunk " + i)
                    .contains(firstSentence);
        }
    }

    @Test
    void transcriptChunksFollowSpeakerTurnsAndCarryTheSpeakerPrefix() {
        List<CleanedBlock> blocks = new ArrayList<>();
        blocks.add(text(null, "Operator: Welcome to the call."));
        blocks.add(text(null, "Susan Li, CFO"));
        blocks.addAll(paragraphs(null, 8));
        blocks.add(text(null, "Brian Nowak, Morgan Stanley: My question is about capital expenditure."));
        blocks.add(text(null, "Susan Li: Capital expenditure will grow next year."));

        List<Chunk> chunks = chunker.chunk(document(DocType.TRANSCRIPT), blocks);

        assertThat(chunks).extracting(Chunk::speaker).containsSubsequence(
                "Operator", "Susan Li, CFO", "Brian Nowak, Morgan Stanley", "Susan Li, CFO");
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content())
                .startsWith(TRANSCRIPT_HEADER + "\nSpeaker: " + chunk.speaker() + "\n"));
        Chunk question = chunks.stream().filter(c -> c.speaker().startsWith("Brian")).findFirst().orElseThrow();
        assertThat(question.content()).contains("My question is about capital expenditure.")
                .doesNotContain("will grow next year");
    }

    @Test
    void paragraphSplitAcrossAPageBreakIsJoinedWithAPageRange() {
        List<CleanedBlock> blocks = List.of(
                new CleanedBlock(null, 1, BlockKind.TEXT, "Mark Zuckerberg, CEO", false),
                new CleanedBlock(null, 1, BlockKind.TEXT, "For our earliest adopters, the longer that they use Muse,", false),
                new CleanedBlock(null, 2, BlockKind.TEXT, "the better that retention seems to get.", false));

        List<Chunk> chunks = chunker.chunk(document(DocType.TRANSCRIPT), blocks);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.pageStart()).isEqualTo(1);
            assertThat(chunk.pageEnd()).isEqualTo(2);
        });
    }

    @Test
    void sectionAndForwardLookingChangesStartANewChunk() {
        List<CleanedBlock> blocks = List.of(
                text("Overview", "Revenue grew 25%."),
                text("Overview", "Margins improved."),
                new CleanedBlock("Forward-Looking Statements", null, BlockKind.TEXT,
                        "This report contains forward-looking statements.", true));

        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), blocks);

        assertThat(chunks).extracting(Chunk::section).containsExactly("Overview", "Forward-Looking Statements");
        assertThat(chunks).extracting(Chunk::forwardLooking).containsExactly(false, true);
        assertThat(chunks.get(0).content()).contains("Margins improved.").doesNotContain("forward-looking");
    }

    @Test
    void chunksUnderAHeadingCarryASectionLine() {
        List<CleanedBlock> blocks = List.of(
                text("Item 3. Defaults Upon Senior Securities", "None."),
                text(null, "Text before any heading."));

        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), blocks);

        assertThat(chunks).extracting(Chunk::content).containsExactly(
                FILING_HEADER + "\nSection: Item 3. Defaults Upon Senior Securities\nNone.",
                FILING_HEADER + "\nText before any heading.");
    }

    @Test
    void bigTablesAreSplitBetweenRowsNeverInsideARow() {
        List<CleanedBlock> blocks = List.of(new CleanedBlock("Results", null, BlockKind.TABLE, tableRows(400), false));

        List<Chunk> chunks = chunker.chunk(document(DocType.FORM_10Q), blocks);

        assertThat(chunks).hasSizeGreaterThan(1).allSatisfy(chunk -> {
            List<String> lines = chunk.content().lines().skip(2).filter(line -> !line.isBlank()).toList();
            assertThat(lines).allMatch(line -> line.matches("Row \\d+ revenue \\| \\$\\d+,000 \\| \\$\\d+,500"));
        });
    }

    // ---------- helpers ----------

    private static ManifestEntry document(DocType type) {
        return new ManifestEntry("MSFT", "Microsoft Corporation", 2026, 2, type,
                "https://example.com/doc", DocumentFormat.HTML);
    }

    private static CleanedBlock text(String section, String text) {
        return new CleanedBlock(section, null, BlockKind.TEXT, text, false);
    }

    /** Paragraphs of 8 short sentences each (about 130 tokens per paragraph). */
    private static List<CleanedBlock> paragraphs(String section, int count) {
        List<CleanedBlock> blocks = new ArrayList<>();
        for (int p = 1; p <= count; p++) {
            StringBuilder paragraph = new StringBuilder();
            for (int s = 1; s <= 8; s++) {
                paragraph.append("Sentence ").append(s).append(" of paragraph ").append(p)
                        .append(" says that cloud revenue grew strongly this quarter. ");
            }
            blocks.add(text(section, paragraph.toString().strip()));
        }
        return blocks;
    }

    private static String tableRows(int count) {
        StringBuilder table = new StringBuilder();
        for (int r = 1; r <= count; r++) {
            table.append("Row ").append(r).append(" revenue | $").append(r).append(",000 | $").append(r).append(",500\n");
        }
        return table.toString().strip();
    }
}
