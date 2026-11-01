package com.earningslens.ingest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfParserTest {

    private static final String FULL_LINE_1 = "Thank you. Good afternoon and welcome to our third quarter earnings call today.";
    private static final String FULL_LINE_2 = "Joining me are our chief executive officer and our chief financial officer, both.";

    @TempDir
    Path tempDir;

    // ---- Paragraph rules, tested on plain text (fast and exact) ----

    @Test
    void wrappedLinesAreJoinedAndAShortLineEndsTheParagraph() {
        List<ParsedBlock> blocks = PdfParser.paragraphs(FULL_LINE_1 + "\n" + FULL_LINE_2 + "\nThanks.\n", 1);

        assertThat(blocks).extracting(ParsedBlock::text)
                .containsExactly(FULL_LINE_1 + " " + FULL_LINE_2 + " Thanks.");
    }

    @Test
    void shortLabelLineIsItsOwnBlock() {
        List<ParsedBlock> blocks = PdfParser.paragraphs("Susan Li, CFO\n" + FULL_LINE_1 + "\n", 1);

        assertThat(blocks).extracting(ParsedBlock::text).containsExactly("Susan Li, CFO", FULL_LINE_1);
    }

    @Test
    void pageNumberLineIsSeparateEvenBetweenFullLines() {
        List<ParsedBlock> blocks = PdfParser.paragraphs(FULL_LINE_1 + "\n12\n" + FULL_LINE_2 + "\n", 12);

        assertThat(blocks).extracting(ParsedBlock::text).containsExactly(FULL_LINE_1, "12", FULL_LINE_2);
    }

    @Test
    void blankLineEndsAParagraph() {
        List<ParsedBlock> blocks = PdfParser.paragraphs(FULL_LINE_1 + "\n \n" + FULL_LINE_2 + "\n", 1);

        assertThat(blocks).extracting(ParsedBlock::text).containsExactly(FULL_LINE_1, FULL_LINE_2);
    }

    @Test
    void textIsNormalizedAndBlocksCarryThePage() {
        List<ParsedBlock> blocks = PdfParser.paragraphs("non‐GAAP measures\n", 7);

        assertThat(blocks).singleElement().satisfies(b -> {
            assertThat(b.text()).isEqualTo("non-GAAP measures");
            assertThat(b.page()).isEqualTo(7);
            assertThat(b.section()).isNull();
            assertThat(b.kind()).isEqualTo(BlockKind.TEXT);
        });
    }

    // ---- End to end on a real (small, generated) PDF ----

    @Test
    void readsEachPageOfAGeneratedPdfWithItsPageNumber() throws IOException {
        Path pdf = tempDir.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            addPage(document, FULL_LINE_1, "end of the first paragraph.", "Susan Li, CFO");
            addPage(document, "Thank you, everyone.");
            document.save(pdf.toFile());
        }

        List<ParsedBlock> blocks = new PdfParser().parse(pdf);

        assertThat(blocks).extracting(ParsedBlock::text).containsExactly(
                FULL_LINE_1 + " end of the first paragraph.",
                "Susan Li, CFO",
                "Thank you, everyone.");
        assertThat(blocks).extracting(ParsedBlock::page).containsExactly(1, 1, 2);
    }

    /** Writes the lines top to bottom on a new page. */
    private static void addPage(PDDocument document, String... lines) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            float y = 700;
            for (String line : lines) {
                content.beginText();
                content.newLineAtOffset(50, y);
                content.showText(line);
                content.endText();
                y -= 14;
            }
        }
    }
}
