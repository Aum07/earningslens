package com.earningslens.generation;

import com.earningslens.generation.CitationCheck.CitedSentence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roadmap E4.3: valid markers, out-of-range markers, repeated markers and no markers. Pure Java, runs in CI. */
class CitationValidatorTest {

    private final CitationValidator validator = new CitationValidator();

    @Test
    void validMarkersAreKeptAndListedInOrderOfFirstUse() {
        CitationCheck check = validator.validate(
                "Revenue was $32.9 billion [2]. Operating income grew 20% [1][3].", 3);

        assertThat(check.text()).isEqualTo("Revenue was $32.9 billion [2]. Operating income grew 20% [1][3].");
        assertThat(check.citedMarkers()).containsExactly(2, 1, 3);
        assertThat(check.sentences()).containsExactly(
                new CitedSentence("Revenue was $32.9 billion [2].", List.of(2)),
                new CitedSentence("Operating income grew 20% [1][3].", List.of(1, 3)));
        assertThat(check.warnings()).isEmpty();
    }

    @Test
    void outOfRangeMarkersAreRemovedWithOneWarningEach() {
        CitationCheck check = validator.validate(
                "Revenue was $32.9 billion [7]. Operating margins rose [0][2]. Capex was $10 billion [2, 9].", 3);

        assertThat(check.text()).isEqualTo("Revenue was $32.9 billion. Operating margins rose [2]. Capex was $10 billion [2].");
        assertThat(check.citedMarkers()).containsExactly(2);
        assertThat(check.warnings()).containsExactly(
                "Removed citation [7]: there are only 3 sources.",
                "Removed citation [0]: there are only 3 sources.",
                "Removed citation [9]: there are only 3 sources.",
                "1 of 3 sentences have no citation.");   // the first sentence lost its only marker
        assertThat(check.uncitedSentences()).extracting(CitedSentence::text)
                .containsExactly("Revenue was $32.9 billion.");
    }

    @Test
    void repeatedMarkersAreCollapsedInsideAGroupAndListedOnce() {
        CitationCheck check = validator.validate(
                "Azure grew 40% [1][1]. Cloud margins improved [1, 1, 2]. Azure demand stayed strong [1].", 2);

        assertThat(check.text()).isEqualTo("Azure grew 40% [1]. Cloud margins improved [1][2]. Azure demand stayed strong [1].");
        assertThat(check.citedMarkers()).containsExactly(1, 2);
        assertThat(check.sentences()).extracting(CitedSentence::markers)
                .containsExactly(List.of(1), List.of(1, 2), List.of(1));
        assertThat(check.warnings()).isEmpty();
    }

    @Test
    void answerWithNoMarkersGetsOneUncitedWarning() {
        CitationCheck check = validator.validate("Revenue grew strongly this quarter. Margins also improved a lot.", 4);

        assertThat(check.citedMarkers()).isEmpty();
        assertThat(check.uncitedSentences()).hasSize(2);
        assertThat(check.warnings()).containsExactly("2 of 2 sentences have no citation.");
    }

    @Test
    void markerAfterTheFullStopBelongsToThePreviousSentence() {
        CitationCheck check = validator.validate("Revenue was $32.9 billion. [2] Capex rose to $10 billion. [1]", 2);

        assertThat(check.sentences()).extracting(CitedSentence::markers).containsExactly(List.of(2), List.of(1));
        assertThat(check.warnings()).isEmpty();
    }

    @Test
    void decimalsAbbreviationsAndYearsDoNotConfuseTheSplitter() {
        CitationCheck check = validator.validate(
                "U.S. revenue was $3.2 billion, e.g. from ads [1]. Mr. Nadella said demand in [2026] was high [2].", 2);

        assertThat(check.sentences()).extracting(CitedSentence::text).containsExactly(
                "U.S. revenue was $3.2 billion, e.g. from ads [1].",
                "Mr. Nadella said demand in [2026] was high [2].");
        assertThat(check.citedMarkers()).containsExactly(1, 2);   // [2026] is not a marker
        assertThat(check.warnings()).isEmpty();
    }

    @Test
    void headingsAndShortFragmentsDoNotNeedCitations() {
        String answer = """
                **FY2026 Q1**
                - Revenue was $70.1 billion [1].
                ### FY2026 Q2
                Here is the breakdown:
                - Revenue was $76.4 billion [2].
                Overall, strong.""";

        CitationCheck check = validator.validate(answer, 2);

        assertThat(check.sentences()).extracting(CitedSentence::text).containsExactly(
                "- Revenue was $70.1 billion [1].",
                "- Revenue was $76.4 billion [2].");
        assertThat(check.warnings()).isEmpty();
    }

    @Test
    void removingAMarkerAtTheStartLeavesNoStraySpace() {
        CitationCheck check = validator.validate("[5] Revenue grew a lot [1].", 1);

        assertThat(check.text()).isEqualTo("Revenue grew a lot [1].");
    }

    @Test
    void emptyAnswerAndBadSourceCount() {
        assertThat(validator.validate("", 3).sentences()).isEmpty();
        assertThat(validator.validate(null, 3).warnings()).isEmpty();
        assertThatThrownBy(() -> validator.validate("x", -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
