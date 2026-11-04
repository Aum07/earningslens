package com.earningslens.generation;

import java.util.List;

/**
 * Result of {@link NumberGroundingCheck}.
 *
 * @param numbers  every number found in the answer, in order, with whether a cited source contains it
 * @param warnings one per number not found in any cited source (the API's groundingWarnings)
 */
public record GroundingCheck(List<NumberMention> numbers, List<String> warnings) {

    /** One number as written in the answer ("$3.2 billion") and whether it was found. */
    public record NumberMention(String text, boolean grounded) {
    }

    public GroundingCheck {
        numbers = List.copyOf(numbers);
        warnings = List.copyOf(warnings);
    }

    public long groundedCount() {
        return numbers.stream().filter(NumberMention::grounded).count();
    }

    /** Share of numbers found in the cited sources (spec 11.2), or null if the answer has no numbers. */
    public Double groundingRate() {
        return numbers.isEmpty() ? null : (double) groundedCount() / numbers.size();
    }
}
