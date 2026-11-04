package com.earningslens.eval;

/**
 * One line of eval/human-labels.jsonl (spec 11.3): the person's own scores for one saved answer.
 *
 * @param labelId     matches LabelSample.labelId, for example "q001-C"
 * @param itemId      the dataset item
 * @param config      C or A
 * @param faithful    true if every factual claim is supported by the sources the answer cites; null = not labelled yet
 * @param correctness 1 to 5 against the gold answer (eval/judge-rubric.md, task 3); null = not labelled yet
 * @param note        optional free text, for example why a claim is unsupported
 */
public record HumanLabel(String labelId, String itemId, String config, Boolean faithful, Integer correctness,
                         String note) {

    public boolean complete() {
        return faithful != null && correctness != null;
    }

    public boolean started() {
        return faithful != null || correctness != null || (note != null && !note.isBlank());
    }
}
