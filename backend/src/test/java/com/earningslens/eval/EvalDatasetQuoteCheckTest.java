package com.earningslens.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Roadmap E2.2 / E2.3 check: every gold quote in eval/dataset.jsonl must appear in at
 * least one stored chunk OF THE DOCUMENT THE ITEM NAMES (ticker, period, doc type).
 * Matching ignores differences in whitespace, like the retrieval metrics will (spec 11.1).
 *
 * Writes target/eval-quote-check.txt with a line per quote, and fails listing every bad
 * quote at once so they can all be fixed in one pass.
 *
 * Needs the ingested corpus in the local Docker database. Skipped automatically when the
 * chunks table is empty (for example in CI).
 */
@SpringBootTest
class EvalDatasetQuoteCheckTest {

    private static final Path DATASET = Path.of("../eval/dataset.jsonl");
    private static final Path REPORT = Path.of("target/eval-quote-check.txt");

    /** Where a quote occurs, per document. Whitespace collapsed on both sides. */
    private static final String FIND_QUOTE = """
            SELECT ticker, fiscal_year, fiscal_quarter, doc_type, count(*) AS hits
            FROM chunks
            WHERE strpos(regexp_replace(content, '\\s+', ' ', 'g'), ?) > 0
            GROUP BY ticker, fiscal_year, fiscal_quarter, doc_type
            """;

    /** Same, but curly quotes and apostrophes made straight: only used to give a hint. */
    private static final String FIND_QUOTE_STRAIGHT = """
            SELECT count(*)
            FROM chunks
            WHERE strpos(translate(regexp_replace(content, '\\s+', ' ', 'g'), '‘’“”', '''''""'),
                         translate(?, '‘’“”', '''''""')) > 0
            """;

    @Autowired
    EvalDatasetLoader loader;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyGoldQuoteIsInAStoredChunkOfItsDocument() throws IOException {
        Integer chunkCount = jdbc.queryForObject("SELECT count(*) FROM chunks", Integer.class);
        assumeTrue(chunkCount != null && chunkCount > 0, "No chunks in the database: run ingestion first");

        List<EvalItem> items = loader.load(DATASET);
        assumeTrue(!items.isEmpty(), "eval/dataset.jsonl is empty");

        List<String> report = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        report.add(summary(items));

        for (EvalItem item : items) {
            for (int i = 0; i < item.goldEvidence().size(); i++) {
                GoldEvidence evidence = item.goldEvidence().get(i);
                String quote = evidence.normalizedQuote();
                String expected = "%s %s %s".formatted(evidence.ticker(), evidence.period(), evidence.docType());
                List<String> foundIn = new ArrayList<>();
                int hitsInExpected = 0;
                for (Map<String, Object> row : jdbc.queryForList(FIND_QUOTE, quote)) {
                    String doc = "%s FY%s Q%s %s".formatted(row.get("ticker"), row.get("fiscal_year"),
                            row.get("fiscal_quarter"), row.get("doc_type"));
                    int hits = ((Number) row.get("hits")).intValue();
                    foundIn.add(doc + " (" + hits + ")");
                    if (doc.equals(expected)) {
                        hitsInExpected = hits;
                    }
                }

                String label = "%s gold_evidence[%d] [%s] \"%s\"".formatted(item.id(), i, expected, shorten(quote));
                if (hitsInExpected > 0) {
                    report.add("OK    " + label + " -> " + hitsInExpected + " chunk(s)");
                } else {
                    String why;
                    if (!foundIn.isEmpty()) {
                        why = "not in that document, but found in: " + String.join(", ", foundIn);
                    } else if (countStraight(quote) > 0) {
                        why = "not found; it matches if curly quotes/apostrophes are made straight, so copy them exactly";
                    } else {
                        why = "not found in any chunk (check the exact wording, or the quote may cross a chunk boundary)";
                    }
                    report.add("FAIL  " + label + " -> " + why);
                    problems.add(label + "\n      " + why);
                }
            }
        }

        Files.createDirectories(REPORT.getParent());
        Files.write(REPORT, report);
        assertThat(problems)
                .as("Bad gold quotes (full report in backend/" + REPORT + "):\n" + String.join("\n", problems))
                .isEmpty();
    }

    private int countStraight(String quote) {
        Integer count = jdbc.queryForObject(FIND_QUOTE_STRAIGHT, Integer.class, quote);
        return count == null ? 0 : count;
    }

    private static String summary(List<EvalItem> items) {
        Map<String, Integer> counts = new TreeMap<>();
        for (EvalItem item : items) {
            counts.merge(item.split().jsonName() + " " + item.category().jsonName(), 1, Integer::sum);
        }
        StringBuilder text = new StringBuilder("Items: " + items.size() + "\n");
        counts.forEach((key, count) -> text.append("  ").append(key).append(": ").append(count).append('\n'));
        return text.toString();
    }

    private static String shorten(String quote) {
        return quote.length() <= 80 ? quote : quote.substring(0, 77) + "...";
    }
}
