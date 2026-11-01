package com.earningslens.ingest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests on a small hand-written fixture (src/test/resources/fixtures/sample-filing.html). */
class HtmlParserTest {

    private static List<ParsedBlock> blocks;

    @BeforeAll
    static void parseFixture() throws IOException, URISyntaxException {
        Path fixture = Path.of(Objects.requireNonNull(
                HtmlParserTest.class.getResource("/fixtures/sample-filing.html")).toURI());
        blocks = new HtmlParser().parse(fixture);
    }

    @Test
    void dropsScriptsStylesHiddenElementsNavigationAndFooters() {
        String all = allText();
        assertThat(all).doesNotContain("HIDDEN XBRL DATA", "SCRIPT TEXT", "color: black",
                "Home", "Products", "Copyright");
    }

    @Test
    void boldParagraphBecomesTheSectionOfFollowingBlocks() {
        assertThat(block("Revenue increased").section()).isEqualTo("Executive Overview");
    }

    @Test
    void htmlHeadingTagsAndUnderlinedTextAreHeadingsToo() {
        assertThat(block("We had cash").section()).isEqualTo("Liquidity and Capital Resources");
        assertThat(block("Non-GAAP measures").section()).isEqualTo("OVERVIEW");
    }

    @Test
    void headingRepeatedOnEveryPageIsDropped() {
        assertThat(blocks).extracting(ParsedBlock::section).doesNotContain("PART I");
        assertThat(allText()).doesNotContain("PART I");
    }

    @Test
    void tableBecomesPipeSeparatedRowsWithSymbolsJoined() {
        ParsedBlock table = blocks.stream().filter(b -> b.kind() == BlockKind.TABLE).findFirst().orElseThrow();

        assertThat(table.text()).isEqualTo("""
                2025 | 2026
                Revenues | $102,346 | $127,577
                Change | 16% | (4)%""");
        assertThat(table.section()).isEqualTo("Executive Overview");
    }

    @Test
    void partlyBoldTextAndSpeakerLabelsStayAsText() {
        assertThat(block("We had cash").text()).isEqualTo("We had cash of $95.6 billion at quarter end.");
        assertThat(block("SATYA NADELLA:").kind()).isEqualTo(BlockKind.TEXT);
    }

    @Test
    void loneBulletIsJoinedToTheNextBlock() {
        assertThat(block("• Operating income").text()).isEqualTo("• Operating income increased 30%.");
    }

    @Test
    void specialSpacesAndHyphensAreNormalized() {
        assertThat(allText()).contains("Revenue increased 25%", "Non-GAAP measures");
    }

    private static ParsedBlock block(String prefix) {
        return blocks.stream().filter(b -> b.text().startsWith(prefix)).findFirst()
                .orElseThrow(() -> new AssertionError("No block starts with: " + prefix));
    }

    private static String allText() {
        return String.join("\n", blocks.stream().map(ParsedBlock::text).toList());
    }
}
