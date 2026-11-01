package com.earningslens.ingest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parses one real Meta transcript PDF and checks the page numbers (the "done when"
 * check for E1.4). Meta prints the page number at the bottom of every page, so
 * the last block of page N should be exactly "N".
 *
 * Also writes target/parse-preview/META_FY2026_Q3_TRANSCRIPT.txt to read by eye.
 * Skipped automatically when the file is not downloaded (for example in CI).
 */
class RealTranscriptParsePreviewTest {

    private static final Path TRANSCRIPT = Path.of("../corpus/raw/META_FY2026_Q3_TRANSCRIPT.pdf");
    private static final Path PREVIEW = Path.of("target/parse-preview/META_FY2026_Q3_TRANSCRIPT.txt");

    @Test
    void pageNumbersMatchTheNumbersPrintedOnThePages() throws IOException {
        assumeTrue(Files.exists(TRANSCRIPT), "Run the Ingest job first to download " + TRANSCRIPT);

        List<ParsedBlock> blocks = new PdfParser().parse(TRANSCRIPT);
        writePreview(blocks);

        Map<Integer, List<ParsedBlock>> byPage = blocks.stream()
                .collect(Collectors.groupingBy(ParsedBlock::page));
        assertThat(byPage).hasSize(20);
        byPage.forEach((page, pageBlocks) ->
                assertThat(pageBlocks.get(pageBlocks.size() - 1).text()).as("last block on page " + page)
                        .isEqualTo(String.valueOf(page)));
        assertThat(blocks).anyMatch(b -> b.page() == 1 && b.text().contains("Third Quarter 2026 Results Conference Call"));

        // Paragraphs, not single lines: about 10 to 15 blocks per page, not 40.
        assertThat(blocks.size()).isLessThan(20 * 20);
    }

    private static void writePreview(List<ParsedBlock> blocks) throws IOException {
        List<String> lines = new ArrayList<>();
        int lastPage = 0;
        for (ParsedBlock block : blocks) {
            if (block.page() != lastPage) {
                lines.add("");
                lines.add("=== page " + block.page() + " ===");
                lastPage = block.page();
            }
            lines.add(block.text());
            lines.add("");
        }
        Files.createDirectories(PREVIEW.getParent());
        Files.write(PREVIEW, lines);
        System.out.println("Preview written to " + PREVIEW.toAbsolutePath().normalize());
    }
}
