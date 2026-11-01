package com.earningslens.ingest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** One test per cleaning rule, each with a clear "before" and "after". */
class TextCleanerTest {

    private final TextCleaner cleaner = new TextCleaner();

    @Test
    void removesPageNumbersButKeepsOtherNumbers() {
        List<CleanedBlock> after = clean(DocType.FORM_10Q,
                "7", "Page 3", "- 12 -", "3 of 20", "2026", "Revenue grew 25% in Q3.");

        assertThat(texts(after)).containsExactly("2026", "Revenue grew 25% in Q3.");
    }

    @Test
    void removesShortLinesRepeatedOnEveryPageOfAFiling() {
        List<CleanedBlock> after = clean(DocType.FORM_10Q,
                "Item 2", "Overview text.", "Item 2", "More text.", "Item 2", "Item 3", "Item 3");

        // "Item 2" appears 3 times (running header): removed. "Item 3" only twice: kept.
        assertThat(texts(after)).containsExactly("Overview text.", "More text.", "Item 3", "Item 3");
    }

    @Test
    void keepsRepeatedSpeakerNamesInTranscripts() {
        List<CleanedBlock> after = clean(DocType.TRANSCRIPT,
                "AMY HOOD:", "Thanks.", "AMY HOOD:", "Sure.", "AMY HOOD:", "Yes.");

        assertThat(texts(after)).containsExactly("AMY HOOD:", "Thanks.", "AMY HOOD:", "Sure.", "AMY HOOD:", "Yes.");
    }

    @Test
    void removesSmallTablesRepeatedOnEveryPageOfAFiling() {
        List<ParsedBlock> blocks = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            blocks.add(new ParsedBlock("S", null, BlockKind.TABLE, "Table of Contents | Alphabet Inc."));
            blocks.add(new ParsedBlock("S", null, BlockKind.TEXT, "Body text " + page + "."));
        }
        blocks.add(new ParsedBlock("S", null, BlockKind.TEXT, "(Mark One)"));

        List<CleanedBlock> after = new TextCleaner().clean(blocks, DocType.FORM_10Q);

        assertThat(texts(after)).containsExactly("Body text 0.", "Body text 1.", "Body text 2.");
    }

    @Test
    void longRepeatedTextIsNotTreatedAsAHeader() {
        String sentence = "Refer to accompanying notes to the consolidated financial statements for details.";
        List<CleanedBlock> after = clean(DocType.FORM_10Q, sentence, sentence, sentence);

        assertThat(after).hasSize(3);
    }

    @Test
    void removesLoneFootnoteMarkersButKeepsFootnoteText() {
        List<CleanedBlock> after = clean(DocType.FORM_10Q, "(a)", "*", "(1)", "(a) Refer to Note 5.");

        assertThat(texts(after)).containsExactly("(a) Refer to Note 5.");
    }

    @Test
    void removesWebPageAndSecWrapperClutter() {
        List<CleanedBlock> after = clean(DocType.PRESS_RELEASE,
                "EX-99.1 3 meta-09302026xexhibit991.htm EX-99.1", "Follow us", "Share this page",
                "Table of Contents", "Meta reported revenue of $64.6 billion.");

        assertThat(texts(after)).containsExactly("Meta reported revenue of $64.6 billion.");
    }

    @Test
    void collapsesWhitespaceInTextAndTables() {
        List<ParsedBlock> before = List.of(
                new ParsedBlock("S", null, BlockKind.TEXT, "  Revenue   grew\t 25%  "),
                new ParsedBlock("S", null, BlockKind.TABLE, "  Revenue |  $100  \n\n   Cost | $40 \n"));

        List<CleanedBlock> after = cleaner.clean(before, DocType.FORM_10Q);

        assertThat(texts(after)).containsExactly("Revenue grew 25%", "Revenue | $100\nCost | $40");
    }

    @Test
    void tagsForwardLookingStatementsByTextOrHeadingAndKeepsThem() {
        List<ParsedBlock> before = List.of(
                new ParsedBlock(null, 1, BlockKind.TEXT, "This call includes forward-looking statements."),
                new ParsedBlock("Note About Forward-Looking Statements", null, BlockKind.TEXT,
                        "Actual results could differ materially."),
                new ParsedBlock(null, 1, BlockKind.TEXT, "These remarks fall under the safe harbor provisions."),
                new ParsedBlock("Overview", null, BlockKind.TEXT, "Revenue grew 25%."));

        List<CleanedBlock> after = cleaner.clean(before, DocType.TRANSCRIPT);

        assertThat(after).extracting(CleanedBlock::forwardLooking).containsExactly(true, true, true, false);
    }

    @Test
    void keepsSectionPageAndKind() {
        List<CleanedBlock> after = cleaner.clean(
                List.of(new ParsedBlock("Liquidity", 4, BlockKind.TABLE, "Cash | $95")), DocType.FORM_10Q);

        assertThat(after).containsExactly(new CleanedBlock("Liquidity", 4, BlockKind.TABLE, "Cash | $95", false));
    }

    private List<CleanedBlock> clean(DocType docType, String... texts) {
        List<ParsedBlock> blocks = new ArrayList<>();
        for (String text : texts) {
            blocks.add(new ParsedBlock(null, null, BlockKind.TEXT, text));
        }
        return cleaner.clean(blocks, docType);
    }

    private static List<String> texts(List<CleanedBlock> blocks) {
        return blocks.stream().map(CleanedBlock::text).toList();
    }
}
