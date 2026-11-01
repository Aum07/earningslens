package com.earningslens.ingest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses SEC filings and IR transcript pages into {@link ParsedBlock}s (roadmap E1.3).
 *
 * How it works:
 * 1. Walk the HTML tree in document order. Text is collected until a block element
 *    (p, div, li, h1...) starts or ends; then the collected text becomes one block.
 * 2. Tables become one block each: one line per row, cells joined by " | ".
 * 3. A short block whose text is all bold or underlined (or an h1-h6) is a heading.
 *    SEC filings have no h tags; their headings are bold spans, so style matters.
 * 4. A heading seen 3 or more times is a running page header (for example "PART I")
 *    and is dropped. Every other block gets the nearest heading above it as section.
 *
 * Dropped: scripts, styles, nav/header/footer, hidden elements (including the hidden
 * inline XBRL data in SEC filings), and blocks that are mostly links (menus).
 */
@Component
public class HtmlParser implements DocumentParser {

    private static final int MAX_HEADING_CHARS = 150;
    private static final int MAX_HEADING_WORDS = 20;
    private static final int RUNNING_HEADER_MIN_COUNT = 3;
    private static final double HEADING_EMPHASIS_SHARE = 0.95;
    private static final double MENU_LINK_SHARE = 0.5;

    private static final Set<String> SKIPPED_TAGS = Set.of(
            "head", "script", "style", "noscript", "template", "nav", "header", "footer",
            "svg", "iframe", "button", "select", "input", "ix:header");
    private static final Set<String> BLOCK_TAGS = Set.of(
            "html", "body", "main", "article", "section", "aside", "div", "p", "blockquote", "pre",
            "ul", "ol", "li", "dl", "dt", "dd", "figure", "figcaption", "address", "center", "hr",
            "h1", "h2", "h3", "h4", "h5", "h6");
    private static final Set<String> HEADING_TAGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    private static final Pattern EMPHASIS_STYLE = Pattern.compile(
            "font-weight\\s*:\\s*(bold|[6-9]00)|text-decoration[^;]*underline", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIDDEN_STYLE = Pattern.compile("display\\s*:\\s*none", Pattern.CASE_INSENSITIVE);
    private static final Pattern BULLET_ONLY = Pattern.compile("[•●▪◦·\\-–]");
    private static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");
    private static final Pattern HAS_LETTER_OR_DIGIT = Pattern.compile("[\\p{L}\\p{N}]");

    @Override
    public List<ParsedBlock> parse(Path file) throws IOException {
        // null charset: Jsoup reads it from the file's <meta> tag, defaulting to UTF-8.
        return parse(Jsoup.parse(file.toFile(), null));
    }

    /** Parses an HTML string. Used by the unit tests. */
    public List<ParsedBlock> parseHtml(String html) {
        return parse(Jsoup.parse(html));
    }

    private List<ParsedBlock> parse(Document document) {
        BlockCollector collector = new BlockCollector();
        collector.walk(document, false, false);
        collector.flush();
        return assignSections(collector.blocks);
    }

    /** Second pass: drop running headers and attach the nearest heading to each block. */
    private static List<ParsedBlock> assignSections(List<RawBlock> rawBlocks) {
        Map<String, Integer> headingCounts = new HashMap<>();
        for (RawBlock block : rawBlocks) {
            if (block.isHeading()) {
                headingCounts.merge(block.text(), 1, Integer::sum);
            }
        }

        List<ParsedBlock> result = new ArrayList<>();
        String section = null;
        for (RawBlock block : rawBlocks) {
            if (block.isHeading()) {
                if (headingCounts.get(block.text()) < RUNNING_HEADER_MIN_COUNT) {
                    section = block.text();
                }
            } else {
                result.add(new ParsedBlock(section, null, block.kind(), block.text()));
            }
        }
        return result;
    }

    /** Converts a table into "cell | cell | cell" lines, one per row. */
    static String tableText(Element table) {
        List<String> rows = new ArrayList<>();
        for (Element row : table.select("tr")) {
            List<String> cells = new ArrayList<>();
            for (Element cell : row.children()) {
                if (!cell.normalName().equals("td") && !cell.normalName().equals("th")) {
                    continue;
                }
                String text = TextNormalizer.normalize(cell.text());
                if (text.isEmpty()) {
                    continue;
                }
                int last = cells.size() - 1;
                // SEC tables put "$", ")" and "%" in their own cells. Glue them to the number.
                if (last >= 0 && cells.get(last).equals("$")) {
                    cells.set(last, "$" + text);
                } else if (last >= 0 && text.matches("[)%]+")) {
                    cells.set(last, cells.get(last) + text);
                } else {
                    cells.add(text);
                }
            }
            String line = String.join(" | ", cells);
            if (HAS_LETTER_OR_DIGIT.matcher(line).find()) {
                rows.add(line);
            }
        }
        return String.join("\n", rows);
    }

    private static boolean looksLikeHeading(String text) {
        return text.length() <= MAX_HEADING_CHARS
                && HAS_LETTER.matcher(text).find()
                && !text.endsWith(":")                       // "SATYA NADELLA:" is a speaker label
                && text.split(" ").length <= MAX_HEADING_WORDS;
    }

    private static int visibleChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
                count++;
            }
        }
        return count;
    }

    private enum RawKind { HEADING, TEXT, TABLE }

    private record RawBlock(RawKind rawKind, String text) {
        boolean isHeading() {
            return rawKind == RawKind.HEADING;
        }

        BlockKind kind() {
            return rawKind == RawKind.TABLE ? BlockKind.TABLE : BlockKind.TEXT;
        }
    }

    /** Collects text while walking the tree and cuts it into blocks. One instance per document. */
    private static final class BlockCollector {

        private final List<RawBlock> blocks = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private int totalChars;
        private int emphasizedChars;
        private int linkChars;
        private String pendingBullet;

        void walk(Node node, boolean emphasized, boolean inLink) {
            for (Node child : node.childNodes()) {
                if (child instanceof TextNode textNode) {
                    append(textNode.getWholeText(), emphasized, inLink);
                } else if (child instanceof Element element) {
                    visitElement(element, emphasized, inLink);
                }
                // Comments and other node types are ignored.
            }
        }

        private void visitElement(Element element, boolean emphasized, boolean inLink) {
            String name = element.normalName();
            String style = element.attr("style");
            if (SKIPPED_TAGS.contains(name) || element.hasAttr("hidden") || HIDDEN_STYLE.matcher(style).find()) {
                return;
            }
            if (name.equals("table")) {
                flush();
                String table = tableText(element);
                if (!table.isEmpty()) {
                    blocks.add(new RawBlock(RawKind.TABLE, table));
                }
                return;
            }
            if (name.equals("br")) {
                append(" ", emphasized, inLink);
                return;
            }

            boolean childEmphasized = emphasized
                    || name.equals("b") || name.equals("strong")
                    || HEADING_TAGS.contains(name)
                    || EMPHASIS_STYLE.matcher(style).find();
            boolean childInLink = inLink || name.equals("a");

            if (BLOCK_TAGS.contains(name)) {
                flush();
                walk(element, childEmphasized, childInLink);
                flush();
            } else {
                walk(element, childEmphasized, childInLink);
            }
        }

        private void append(String raw, boolean emphasized, boolean inLink) {
            int chars = visibleChars(raw);
            text.append(raw);
            totalChars += chars;
            if (emphasized) {
                emphasizedChars += chars;
            }
            if (inLink) {
                linkChars += chars;
            }
        }

        /** Turns the collected text into a block (or drops it) and resets the counters. */
        void flush() {
            String cleaned = TextNormalizer.normalize(text.toString());
            boolean menu = linkChars > totalChars * MENU_LINK_SHARE;
            boolean allEmphasized = emphasizedChars >= totalChars * HEADING_EMPHASIS_SHARE;
            text.setLength(0);
            int chars = totalChars;
            totalChars = 0;
            emphasizedChars = 0;
            linkChars = 0;

            if (cleaned.isEmpty() || chars == 0 || menu) {
                return;
            }
            if (BULLET_ONLY.matcher(cleaned).matches()) {
                pendingBullet = "• ";   // Some filings put the bullet in its own element.
                return;
            }
            if (allEmphasized && looksLikeHeading(cleaned)) {
                pendingBullet = null;
                blocks.add(new RawBlock(RawKind.HEADING, cleaned));
                return;
            }
            String body = pendingBullet == null ? cleaned : pendingBullet + cleaned;
            pendingBullet = null;
            blocks.add(new RawBlock(RawKind.TEXT, body));
        }
    }
}
