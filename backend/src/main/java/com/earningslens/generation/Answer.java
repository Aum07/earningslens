package com.earningslens.generation;

import com.earningslens.retrieval.RetrievedChunk;

import java.util.List;

/**
 * The result of answering one question (roadmap E4.2). Citation checks (E4.3, E4.4) run on this next.
 *
 * @param text             the model's answer, or a friendly message when abstained
 * @param abstained        true if no answer is given
 * @param abstainReason    why, or null when answered
 * @param sources          chunks sent to the model, in citation order ([1] is sources.get(0)); empty if the model was not called
 * @param model            the model that answered, or null if the model was not called
 * @param promptTokens     input tokens, or null if the model was not called or did not report them
 * @param completionTokens output tokens, or null if the model was not called or did not report them
 * @param truncated        true if the answer was cut off at the output token limit
 */
public record Answer(String text, boolean abstained, AbstainReason abstainReason, List<RetrievedChunk> sources,
                     String model, Integer promptTokens, Integer completionTokens, boolean truncated) {

    public enum AbstainReason {
        /** Retrieval: every period the question asked for is outside the corpus (model not called). */
        PERIOD_NOT_IN_CORPUS,
        /** Retrieval: best similarity below the evidence threshold (model not called). */
        LOW_SIMILARITY,
        /** Retrieval found no chunks at all (model not called). */
        NO_SOURCES,
        /** The model replied INSUFFICIENT_EVIDENCE. */
        INSUFFICIENT_EVIDENCE
    }

    public Answer {
        sources = List.copyOf(sources);
    }

    /** True if the chat model was called (and billed) for this answer. */
    public boolean modelCalled() {
        return model != null;
    }
}
