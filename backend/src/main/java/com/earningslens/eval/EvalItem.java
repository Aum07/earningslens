package com.earningslens.eval;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * One line of eval/dataset.jsonl (spec 11.1).
 * The compact constructor holds the rules that tie fields together, so an
 * inconsistent item (for example answerable but with no evidence) cannot exist.
 */
public record EvalItem(
        String id,
        EvalSplit split,
        EvalCategory category,
        String question,
        EvalFilters filters,          // null when the item has no explicit filters
        boolean answerable,
        String goldAnswer,
        List<GoldEvidence> goldEvidence
) {

    public EvalItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(split, "split");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(goldAnswer, "gold_answer");
        goldEvidence = goldEvidence == null ? List.of() : List.copyOf(goldEvidence);

        if (!id.matches("q\\d{3}")) {
            throw new IllegalArgumentException("id must look like q001, got '" + id + "'");
        }
        String trimmed = question.strip();
        if (trimmed.length() < 3 || trimmed.length() > 500) {
            throw new IllegalArgumentException("question must be 3 to 500 characters (same rule as the chat API)");
        }
        if (goldAnswer.isBlank()) {
            throw new IllegalArgumentException("gold_answer must not be blank");
        }
        if (answerable == (category == EvalCategory.UNANSWERABLE)) {
            throw new IllegalArgumentException("answerable must be " + (category != EvalCategory.UNANSWERABLE)
                    + " for category " + category.jsonName());
        }
        if (answerable && goldEvidence.isEmpty()) {
            throw new IllegalArgumentException("an answerable item needs at least one gold_evidence quote");
        }
        if (!answerable && !goldEvidence.isEmpty()) {
            throw new IllegalArgumentException("an unanswerable item must have an empty gold_evidence list");
        }
        if (category == EvalCategory.MULTI_QUARTER && requiredPeriodsOf(goldEvidence).size() < 2) {
            throw new IllegalArgumentException("a multi_quarter item needs evidence from at least 2 fiscal periods");
        }
    }

    /** Periods the evidence comes from. Used for the period coverage metric (spec 11.2). */
    public Set<FiscalPeriod> requiredPeriods() {
        return requiredPeriodsOf(goldEvidence);
    }

    private static Set<FiscalPeriod> requiredPeriodsOf(List<GoldEvidence> evidence) {
        Set<FiscalPeriod> periods = new TreeSet<>();
        evidence.forEach(e -> periods.add(e.period()));
        return periods;
    }
}
