package com.earningslens.ingest;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Removes page clutter from parsed blocks and tags forward-looking boilerplate (roadmap E1.5).
 *
 * Rules, applied to every block:
 * 1. Whitespace: collapse runs of spaces; in tables trim each row and drop empty rows.
 * 2. Page numbers: a block that is only "7", "Page 7", "- 7 -" or "7 of 20" is removed.
 * 3. Running headers/footers (filings only): a short block (60 chars or less, text or
 *    table) that appears 3 or more times in the same document is removed, for example
 *    "Item 2" or Alphabet's page-top table "Table of Contents | Alphabet Inc.".
 *    Not applied to transcripts, where speaker names like "AMY HOOD:" repeat on purpose.
 * 4. Lone footnote markers such as "(a)" or "*" are removed.
 * 5. Web page and SEC clutter: "Follow us", "Share this page", "Table of Contents",
 *    the cover-page checkbox label "(Mark One)", and the EDGAR wrapper line "EX-99.1 3 file.htm EX-99.1" are removed.
 * 6. Forward-looking statements: blocks that mention them (or sit under a heading that
 *    does) are kept but tagged with forwardLooking = true.
 */
@Component
public class TextCleaner {

    static final int RUNNING_LINE_MAX_CHARS = 60;
    static final int RUNNING_LINE_MIN_COUNT = 3;

    private static final Pattern PAGE_NUMBER = Pattern.compile(
            "(?i)(page\\s+)?-?\\s*\\d{1,3}\\s*-?(\\s+of\\s+\\d{1,3})?");
    private static final Pattern FOOTNOTE_MARKER = Pattern.compile("\\(?[a-z0-9]{1,2}\\)|\\*{1,3}");
    private static final Pattern SEC_WRAPPER = Pattern.compile(
            "EX-\\d+(\\.\\d+)?\\s+\\d+\\s+\\S+\\.html?\\s+EX-\\d+(\\.\\d+)?.*");
    private static final Pattern FORWARD_LOOKING = Pattern.compile(
            "(?i)forward[- ]looking\\s+statement|safe\\s+harbou?r|private\\s+securities\\s+litigation\\s+reform\\s+act");
    private static final Set<String> WEB_CLUTTER = Set.of(
            "follow us", "share this page", "share", "back to top", "skip to main content",
            "print", "email alerts", "table of contents", "(mark one)");

    public List<CleanedBlock> clean(List<ParsedBlock> blocks, DocType docType) {
        Set<String> runningLines = docType == DocType.TRANSCRIPT ? Set.of() : runningLines(blocks);

        List<CleanedBlock> cleaned = new ArrayList<>();
        for (ParsedBlock block : blocks) {
            boolean table = block.kind() == BlockKind.TABLE;
            String text = table ? tidyTable(block.text()) : TextNormalizer.normalize(block.text());
            if (text.isEmpty() || isClutter(text) || runningLines.contains(text)) {
                continue;
            }
            boolean forwardLooking = FORWARD_LOOKING.matcher(text).find()
                    || (block.section() != null && FORWARD_LOOKING.matcher(block.section()).find());
            cleaned.add(new CleanedBlock(block.section(), block.page(), block.kind(), text, forwardLooking));
        }
        return cleaned;
    }

    /** Short blocks (text or table) that repeat 3+ times in one document: running page headers and footers. */
    private static Set<String> runningLines(List<ParsedBlock> blocks) {
        Map<String, Integer> counts = new HashMap<>();
        for (ParsedBlock block : blocks) {
            String text = block.kind() == BlockKind.TABLE
                    ? tidyTable(block.text()) : TextNormalizer.normalize(block.text());
            if (!text.isEmpty() && text.length() <= RUNNING_LINE_MAX_CHARS) {
                counts.merge(text, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= RUNNING_LINE_MIN_COUNT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static boolean isClutter(String text) {
        return PAGE_NUMBER.matcher(text).matches()
                || FOOTNOTE_MARKER.matcher(text).matches()
                || SEC_WRAPPER.matcher(text).matches()
                || WEB_CLUTTER.contains(text.toLowerCase(Locale.ROOT));
    }

    /** Trims every table row, collapses spaces inside it, and drops empty rows. */
    private static String tidyTable(String table) {
        return table.lines()
                .map(TextNormalizer::normalize)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }
}
