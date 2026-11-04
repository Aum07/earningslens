package com.earningslens.eval;

import com.earningslens.generation.CitationCheck.CitedSentence;
import com.earningslens.retrieval.RetrievedChunk;

import java.util.List;
import java.util.Objects;

/**
 * One saved answer for hand-labelling and judge agreement (roadmap E5.2).
 * Saved to eval/human-labels-answers.json by the label-prep task, so the judge later scores exactly the
 * answers the person labelled (no second generation, no database needed).
 *
 * @param labelId          item id plus config, for example "q001-C"
 * @param itemId           the dataset item
 * @param config           the retrieval config that produced it (C or A)
 * @param category         dataset category (json name, for example "single_fact")
 * @param question         the question
 * @param goldAnswer       the dataset's reference answer
 * @param answer           the answer after CitationValidator
 * @param sentences        its sentences with their markers
 * @param sources          every chunk sent to the answer model, in citation order ([1] is sources.get(0))
 * @param model            the answer model as reported by the API
 * @param promptTokens     answer-model input tokens (may be null)
 * @param completionTokens answer-model output tokens (may be null)
 */
public record LabelSample(String labelId, String itemId, String config, String category, String question,
                          String goldAnswer, String answer, List<CitedSentence> sentences,
                          List<RetrievedChunk> sources, String model, Integer promptTokens,
                          Integer completionTokens) {

    public LabelSample {
        Objects.requireNonNull(labelId, "labelId");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(config, "config");
        sentences = List.copyOf(sentences);
        sources = List.copyOf(sources);
    }

    static String labelId(String itemId, String config) {
        return itemId + "-" + config;
    }

    /** What the judge needs; the same check JudgeInput applies to live answers. */
    JudgeInput judgeInput() {
        return new JudgeInput(question, goldAnswer, answer, sources, sentences);
    }
}
