package com.earningslens.generation;

import com.earningslens.generation.GroundingCheck.NumberMention;
import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap E4.4: each number format, plus a made-up number that must be flagged. Pure Java, runs in CI. */
class NumberGroundingCheckTest {

    private static final RetrievedChunk PRESS_RELEASE = chunk("""
            [MSFT | FY2026 Q2 | PRESS_RELEASE]
            Revenue in Intelligent Cloud was $32.9 billion and increased 29% (up 28% in constant currency).
            Operating margin expanded 150 basis points. The company has 1,234 data centers.""");

    private static final RetrievedChunk TABLE = chunk("""
            [MSFT | FY2026 Q2 | FORM_10Q]
            ($ in millions) | Revenue | Operating Income
            Three Months Ended December 31, 2025 | $ | 81,273 | 38,276""");

    private final NumberGroundingCheck check = new NumberGroundingCheck();

    @Test
    void thousandsSeparatorsAreRead() {
        GroundingCheck result = check.check("They run 1,234 data centers [1].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).containsExactly(new NumberMention("1,234", true));
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void percentagesMatchPercentagesInAnyWording() {
        GroundingCheck result = check.check(
                "Cloud revenue grew 29% [1], or 28 percent in constant currency [1].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).extracting(NumberMention::grounded).containsExactly(true, true);
    }

    @Test
    void dollarAmountsWithScaleWordsAreNormalised() {
        GroundingCheck result = check.check(
                "Intelligent Cloud revenue was $32.9 billion [1], i.e. 32,900 million dollars.", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).containsExactly(
                new NumberMention("$32.9 billion", true),
                new NumberMention("32,900 million", true));
    }

    @Test
    void shortScaleSuffixesAreRead() {
        GroundingCheck result = check.check("Revenue was 32.9bn [1], also written $32.9B.", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).containsExactly(
                new NumberMention("32.9bn", true),
                new NumberMention("$32.9B", true));
    }

    @Test
    void roundedFiguresMatchTableValuesInMillions() {
        GroundingCheck result = check.check(
                "Revenue was $81.3 billion and operating income $38.3 billion [1].", List.of(TABLE));

        assertThat(result.numbers()).extracting(NumberMention::grounded).containsExactly(true, true);
        assertThat(result.groundingRate()).isEqualTo(1.0);
    }

    @Test
    void madeUpNumberIsFlagged() {
        GroundingCheck result = check.check(
                "Revenue was $32.9 billion [1] and Azure grew 47% [1].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).containsExactly(
                new NumberMention("$32.9 billion", true),
                new NumberMention("47%", false));
        assertThat(result.warnings()).containsExactly("Not found in the cited sources: 47%");
        assertThat(result.groundingRate()).isEqualTo(0.5);
    }

    @Test
    void moreDigitsThanTheSourceIsNotGrounded() {
        // The source says $32.9 billion; $32.95 billion is more precise than the evidence.
        GroundingCheck result = check.check("Revenue was $32.95 billion [1].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).containsExactly(new NumberMention("$32.95 billion", false));
    }

    @Test
    void percentIsNotGroundedByAPlainNumberOrBasisPoints() {
        GroundingCheck result = check.check("Margins rose 150% and 1234% [1].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).extracting(NumberMention::grounded).containsExactly(false, false);
    }

    @Test
    void basisPointsAreRead() {
        GroundingCheck result = check.check("Operating margin expanded 150 bps [1].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).containsExactly(new NumberMention("150 bps", true));
    }

    @Test
    void markersYearsLabelsAndSmallCountsAreNotChecked() {
        GroundingCheck result = check.check(
                "In FY2026 Q2 (calendar 2025), across 3 segments, see [1][2] and [1, 2].", List.of(PRESS_RELEASE));

        assertThat(result.numbers()).isEmpty();
        assertThat(result.groundingRate()).isNull();
    }

    @Test
    void onlyCitedSourcesCount() {
        String answer = "Revenue was $81.3 billion [1].";
        CitationCheck citations = new CitationValidator().validate(answer, 2);

        // [1] is the press release, which does not contain 81,273; the table is source [2] but is not cited.
        GroundingCheck result = check.check(citations, List.of(PRESS_RELEASE, TABLE));

        assertThat(result.numbers()).containsExactly(new NumberMention("$81.3 billion", false));
    }

    @Test
    void parsesValuesAndKinds() {
        List<NumberGroundingCheck.ParsedNumber> numbers =
                NumberGroundingCheck.parse("$3.2 billion, 3.2bn, 12.5%, 1,234, 50 basis points");

        assertThat(numbers).extracting(NumberGroundingCheck.ParsedNumber::value)
                .containsExactly(3.2e9, 3.2e9, 12.5, 1234.0, 50.0);
        assertThat(numbers).extracting(NumberGroundingCheck.ParsedNumber::kind).containsExactly(
                NumberGroundingCheck.Kind.AMOUNT, NumberGroundingCheck.Kind.AMOUNT, NumberGroundingCheck.Kind.PERCENT,
                NumberGroundingCheck.Kind.AMOUNT, NumberGroundingCheck.Kind.BASIS_POINTS);
    }

    private static RetrievedChunk chunk(String content) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), 0, "MSFT", 2026, 2, DocType.PRESS_RELEASE,
                content, 100, null, null, null, null, false);
    }
}
