package com.earningslens.ingest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parses one real 10-Q from corpus/raw and writes a readable preview to
 * target/parse-preview/, so you can check by eye that the output reads cleanly
 * (the "done when" check for E1.3).
 *
 * Skipped automatically when the file is not downloaded (for example in CI).
 */
class RealFilingParsePreviewTest {

    private static final Path FILING = Path.of("../corpus/raw/MSFT_FY2027_Q1_FORM_10Q.html");
    private static final Path PREVIEW = Path.of("target/parse-preview/MSFT_FY2027_Q1_FORM_10Q.txt");

    @Test
    void realTenQParsesIntoSectionsAndTables() throws IOException {
        assumeTrue(Files.exists(FILING), "Run the Ingest job first to download " + FILING);

        List<ParsedBlock> blocks = new HtmlParser().parse(FILING);
        writePreview(blocks);

        assertThat(blocks).hasSizeGreaterThan(200);
        assertThat(blocks).extracting(ParsedBlock::section).contains("OVERVIEW");
        assertThat(blocks).anyMatch(b -> b.kind() == BlockKind.TABLE && b.text().contains(" | "));
        assertThat(blocks).noneMatch(b -> b.text().contains("<") && b.text().contains(">"));
    }

    private static void writePreview(List<ParsedBlock> blocks) throws IOException {
        List<String> lines = new ArrayList<>();
        String lastSection = null;
        for (ParsedBlock block : blocks) {
            if (!Objects.equals(block.section(), lastSection)) {
                lines.add("");
                lines.add("=== " + block.section() + " ===");
                lastSection = block.section();
            }
            lines.add((block.kind() == BlockKind.TABLE ? "[TABLE]\n" : "") + block.text());
            lines.add("");
        }
        Files.createDirectories(PREVIEW.getParent());
        Files.write(PREVIEW, lines);
        System.out.println("Preview written to " + PREVIEW.toAbsolutePath().normalize());
    }
}
