package com.earningslens.ingest;

import com.earningslens.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs parse, clean and chunk on every downloaded document (no database, no OpenAI)
 * and checks the chunk rules on real data. Writes:
 * - target/parse-preview/chunk-summary.txt (chunks and average size per document)
 * - target/parse-preview/chunks-META_FY2026_Q3_TRANSCRIPT.txt (every chunk of one transcript)
 *
 * Skipped automatically when corpus/raw has not been downloaded (for example in CI).
 */
class RealCorpusChunkingTest {

    private static final Path RAW = Path.of("../corpus/raw");
    private static final Path PREVIEW_DIR = Path.of("target/parse-preview");
    private static final String SAMPLE = "META_FY2026_Q3_TRANSCRIPT.pdf";

    private final HtmlParser htmlParser = new HtmlParser();
    private final PdfParser pdfParser = new PdfParser();
    private final TextCleaner cleaner = new TextCleaner();
    private final Chunker chunker = new Chunker(new TokenCounter(), new ChunkingProperties(600, 100, 900));

    @Test
    void everyRealDocumentChunksWithinTheRules() throws IOException {
        List<ManifestEntry> entries = new ManifestLoader().load(Path.of("../corpus/manifest.yaml"));
        assumeTrue(Files.exists(RAW.resolve(entries.get(0).rawFileName())), "Run the Ingest job first");

        List<String> summary = new ArrayList<>();
        summary.add("document | blocks | chunks | avg tokens | max tokens | with speaker");
        for (ManifestEntry entry : entries) {
            Path file = RAW.resolve(entry.rawFileName());
            List<ParsedBlock> parsed = entry.format() == DocumentFormat.PDF ? pdfParser.parse(file) : htmlParser.parse(file);
            List<CleanedBlock> cleaned = cleaner.clean(parsed, entry.docType());
            List<Chunk> chunks = chunker.chunk(entry, cleaned);

            String header = "[%s | %s | %s]".formatted(entry.ticker(), entry.periodLabel(), entry.docType());
            assertThat(chunks).as(entry.rawFileName()).isNotEmpty().allSatisfy(chunk -> {
                assertThat(chunk.tokenCount()).isLessThanOrEqualTo(900);
                assertThat(chunk.content()).startsWith(header);
            });
            long withSpeaker = chunks.stream().filter(c -> c.speaker() != null).count();
            if (entry.docType() == DocType.TRANSCRIPT) {
                assertThat(withSpeaker).as(entry.rawFileName() + " chunks with a speaker")
                        .isGreaterThanOrEqualTo(Math.round(chunks.size() * 0.8));
            }

            summary.add("%s | %d | %d | %d | %d | %d".formatted(entry.rawFileName(), cleaned.size(), chunks.size(),
                    chunks.stream().mapToInt(Chunk::tokenCount).sum() / chunks.size(),
                    chunks.stream().mapToInt(Chunk::tokenCount).max().orElse(0), withSpeaker));
            if (entry.rawFileName().equals(SAMPLE)) {
                writeChunks(chunks);
            }
        }
        Files.createDirectories(PREVIEW_DIR);
        Files.write(PREVIEW_DIR.resolve("chunk-summary.txt"), summary);
        System.out.println("Summary written to " + PREVIEW_DIR.resolve("chunk-summary.txt").toAbsolutePath().normalize());
    }

    private static void writeChunks(List<Chunk> chunks) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Chunk chunk : chunks) {
            lines.add("----- chunk %d | %d tokens | pages %s-%s | speaker: %s | forward-looking: %s -----".formatted(
                    chunk.chunkIndex(), chunk.tokenCount(), chunk.pageStart(), chunk.pageEnd(),
                    chunk.speaker(), chunk.forwardLooking()));
            lines.add(chunk.content());
            lines.add("");
        }
        Files.createDirectories(PREVIEW_DIR);
        Files.write(PREVIEW_DIR.resolve("chunks-" + SAMPLE.replace(".pdf", ".txt")), lines);
    }
}
