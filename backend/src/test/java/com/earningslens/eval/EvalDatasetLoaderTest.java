package com.earningslens.eval;

import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: no Spring context and no database needed. */
class EvalDatasetLoaderTest {

    private static final Path SAMPLE = Path.of("src/test/resources/eval/sample-dataset.jsonl");

    private final EvalDatasetLoader loader = new EvalDatasetLoader();

    @TempDir
    Path tempDir;

    @Test
    void sampleFileLoadsAllThreeItems() throws IOException {
        List<EvalItem> items = loader.load(SAMPLE);

        assertThat(items).extracting(EvalItem::id).containsExactly("q001", "q002", "q003");

        EvalItem single = items.get(0);
        assertThat(single.split()).isEqualTo(EvalSplit.DEV);
        assertThat(single.category()).isEqualTo(EvalCategory.SINGLE_FACT);
        assertThat(single.filters()).isNull();
        assertThat(single.goldEvidence()).singleElement().satisfies(e -> {
            assertThat(e.ticker()).isEqualTo("MSFT");
            assertThat(e.period()).isEqualTo(new FiscalPeriod(2026, 1));
            assertThat(e.docType()).isEqualTo(DocType.PRESS_RELEASE);
            assertThat(e.normalizedQuote()).isEqualTo("Example quote for the loader test");
        });

        EvalItem multi = items.get(1);
        assertThat(multi.filters().tickers()).containsExactly("GOOGL");
        assertThat(multi.filters().fiscalPeriods()).containsExactly(new FiscalPeriod(2026, 1), new FiscalPeriod(2026, 2));
        assertThat(multi.filters().docTypes()).containsExactly(DocType.TRANSCRIPT);
        assertThat(multi.requiredPeriods()).containsExactly(new FiscalPeriod(2026, 1), new FiscalPeriod(2026, 2));

        EvalItem unanswerable = items.get(2);
        assertThat(unanswerable.answerable()).isFalse();
        assertThat(unanswerable.goldEvidence()).isEmpty();
    }

    @Test
    void canLoadOneSplit() throws IOException {
        assertThat(loader.load(SAMPLE, EvalSplit.DEV)).extracting(EvalItem::id).containsExactly("q001", "q003");
        assertThat(loader.load(SAMPLE, EvalSplit.TEST)).extracting(EvalItem::id).containsExactly("q002");
    }

    @Test
    void missingFieldNamesLineIdAndField() throws IOException {
        Path file = write(validItem("q001"), validItem("q002").replace("\"question\": \"What was MSFT revenue?\", ", ""));

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 2 (q002)")
                .hasMessageContaining("missing field 'question'");
    }

    @Test
    void unknownCategoryIsRejected() throws IOException {
        Path file = write(validItem("q001").replace("single_fact", "trivia"));

        assertThatThrownBy(() -> loader.load(file))
                .hasMessageContaining("line 1 (q001)")
                .hasMessageContaining("unknown category 'trivia'")
                .hasMessageContaining("single_fact, multi_quarter, qualitative, unanswerable");
    }

    @Test
    void misspelledFieldIsRejected() throws IOException {
        Path file = write(validItem("q001").replace("gold_answer", "gold_anwser"));

        assertThatThrownBy(() -> loader.load(file)).hasMessageContaining("unknown item field 'gold_anwser'");
    }

    @Test
    void brokenJsonIsRejected() throws IOException {
        Path file = write(validItem("q001"), "{\"id\": \"q002\", ");

        assertThatThrownBy(() -> loader.load(file))
                .hasMessageContaining("line 2")
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void duplicateIdIsRejected() throws IOException {
        Path file = write(validItem("q001"), validItem("q001"));

        assertThatThrownBy(() -> loader.load(file)).hasMessageContaining("line 2 (q001): duplicate id q001");
    }

    @Test
    void answerableItemNeedsEvidence() throws IOException {
        Path file = write("""
                {"id": "q001", "split": "dev", "category": "single_fact", "question": "What was MSFT revenue?", \
                "filters": null, "answerable": true, "gold_answer": "x", "gold_evidence": []}""");

        assertThatThrownBy(() -> loader.load(file)).hasMessageContaining("needs at least one gold_evidence quote");
    }

    @Test
    void answerableFlagMustMatchCategory() throws IOException {
        Path file = write(validItem("q001").replace("\"answerable\": true", "\"answerable\": false"));

        assertThatThrownBy(() -> loader.load(file)).hasMessageContaining("answerable must be true for category single_fact");
    }

    @Test
    void multiQuarterItemNeedsTwoPeriods() throws IOException {
        Path file = write(validItem("q001").replace("single_fact", "multi_quarter"));

        assertThatThrownBy(() -> loader.load(file)).hasMessageContaining("at least 2 fiscal periods");
    }

    @Test
    void badEvidenceSaysWhichEntry() throws IOException {
        Path file = write(validItem("q001").replace("\"fiscal_quarter\": 1", "\"fiscal_quarter\": 5"));

        assertThatThrownBy(() -> loader.load(file))
                .hasMessageContaining("gold_evidence[0]")
                .hasMessageContaining("fiscal quarter must be between 1 and 4, got 5");
    }

    @Test
    void tooShortQuoteIsRejected() throws IOException {
        Path file = write(validItem("q001").replace("Microsoft Cloud revenue was", "revenue"));

        assertThatThrownBy(() -> loader.load(file)).hasMessageContaining("quote must be at least 15 characters");
    }

    private static String validItem(String id) {
        return """
                {"id": "%s", "split": "dev", "category": "single_fact", "question": "What was MSFT revenue?", \
                "filters": null, "answerable": true, "gold_answer": "x", "gold_evidence": [{"ticker": "MSFT", \
                "fiscal_year": 2026, "fiscal_quarter": 1, "doc_type": "PRESS_RELEASE", \
                "quote": "Microsoft Cloud revenue was"}]}""".formatted(id);
    }

    private Path write(String... lines) throws IOException {
        Path file = tempDir.resolve("dataset.jsonl");
        Files.write(file, List.of(lines));
        return file;
    }
}
