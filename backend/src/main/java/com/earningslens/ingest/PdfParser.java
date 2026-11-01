package com.earningslens.ingest;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses PDF transcripts into {@link ParsedBlock}s, one per paragraph, each with
 * its page number (roadmap E1.4).
 *
 * How it works:
 * 1. Extract the text of each page separately, so every block knows its page.
 * 2. Rebuild paragraphs from lines. A PDF only stores lines, not paragraphs, so:
 *    - a line clearly shorter than the full width of the page (under 70% of the
 *      longest line) is the last line of a paragraph or a short label like
 *      "Susan Li, CFO", so it ends the paragraph;
 *    - a blank line also ends the paragraph;
 *    - a line that is only a number is the printed page number and becomes its own block.
 * 3. Lines inside a paragraph are joined with spaces.
 *
 * We do not use PDFBox's own paragraph detection: on the Meta transcripts it starts
 * a new paragraph on every line. The page-number blocks stay in the output; the
 * cleaner (E1.5) removes them.
 */
@Component
public class PdfParser implements DocumentParser {

    private static final double SHORT_LINE_SHARE = 0.7;
    private static final Pattern PAGE_NUMBER = Pattern.compile("\\d{1,3}");

    @Override
    public List<ParsedBlock> parse(Path file) throws IOException {
        // Loader.loadPDF is the PDFBox 3 way to open a file (PDDocument.load was removed).
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);   // read in visual order, top to bottom
            stripper.setLineSeparator("\n");

            List<ParsedBlock> blocks = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                blocks.addAll(paragraphs(stripper.getText(document), page));
            }
            return blocks;
        }
    }

    /** Splits one page of text into paragraph blocks. */
    static List<ParsedBlock> paragraphs(String pageText, int page) {
        List<String> lines = new ArrayList<>();
        int longest = 0;
        for (String line : pageText.split("\n", -1)) {
            String cleaned = TextNormalizer.normalize(line);
            lines.add(cleaned);
            longest = Math.max(longest, cleaned.length());
        }
        double shortLine = longest * SHORT_LINE_SHARE;

        List<ParsedBlock> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) {
                addParagraph(blocks, paragraph, page);
            } else if (PAGE_NUMBER.matcher(line).matches()) {
                addParagraph(blocks, paragraph, page);
                blocks.add(new ParsedBlock(null, page, BlockKind.TEXT, line));
            } else {
                if (!paragraph.isEmpty()) {
                    paragraph.append(' ');
                }
                paragraph.append(line);
                if (line.length() < shortLine) {
                    addParagraph(blocks, paragraph, page);
                }
            }
        }
        addParagraph(blocks, paragraph, page);
        return blocks;
    }

    private static void addParagraph(List<ParsedBlock> blocks, StringBuilder paragraph, int page) {
        if (!paragraph.isEmpty()) {
            blocks.add(new ParsedBlock(null, page, BlockKind.TEXT, paragraph.toString()));
            paragraph.setLength(0);
        }
    }
}
