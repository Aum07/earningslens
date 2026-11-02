package com.earningslens.eval;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks eval/dataset.jsonl itself, so a wrong gold answer cannot make the eval unfair:
 * every number in a gold answer must also appear in that item's quotes. Catches typos
 * such as "up 18%" when the quote says 17%, and answers that claim more than the quotes show.
 *
 * Years (2026), quarter labels (Q3) and fiscal-year labels (FY2027) in the answer are
 * ignored: they name the period, they are not claims. "$17.7B" matches "$17.7 billion"
 * because only the digits are compared; "8,000" matches "8000".
 *
 * Plain unit test: no database, so it also runs in CI.
 */
class EvalGoldAnswerNumbersTest {

    private static final Path DATASET = Path.of("../eval/dataset.jsonl");

    private static final Pattern PERIOD_LABEL = Pattern.compile("\\bQ[1-4]\\b|\\bFY ?\\d{2,4}\\b|\\b(?:19|20)\\d{2}\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");

    private final EvalDatasetLoader loader = new EvalDatasetLoader();

    @Test
    void datasetLoads() throws IOException {
        assertThat(loader.load(DATASET)).isNotEmpty();
    }

    @Test
    void everyNumberInAGoldAnswerIsInItsQuotes() throws IOException {
        List<String> problems = new ArrayList<>();
        for (EvalItem item : loader.load(DATASET)) {
            if (!item.answerable()) {
                continue;
            }
            Set<String> inQuotes = new LinkedHashSet<>();
            item.goldEvidence().forEach(e -> inQuotes.addAll(numbers(e.normalizedQuote())));

            Set<String> missing = numbers(PERIOD_LABEL.matcher(item.goldAnswer()).replaceAll(" "));
            missing.removeAll(inQuotes);
            if (!missing.isEmpty()) {
                problems.add("%s: gold_answer has %s, not found in its quotes. Answer: \"%s\""
                        .formatted(item.id(), missing, item.goldAnswer()));
            }
        }
        assertThat(problems).as("Gold answers with unsupported numbers:\n" + String.join("\n", problems)).isEmpty();
    }

    @Test
    void numberRuleIgnoresPeriodLabelsAndFormatting() {
        String answer = "Q3 FY2026 $17.7B (+48%), 8,000 employees in 2026.";
        assertThat(numbers(PERIOD_LABEL.matcher(answer).replaceAll(" "))).containsExactlyInAnyOrder("17.7", "48", "8000");
    }

    /** Numbers in the text with thousands separators removed. */
    static Set<String> numbers(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            found.add(m.group().replace(",", ""));
        }
        return found;
    }
}
