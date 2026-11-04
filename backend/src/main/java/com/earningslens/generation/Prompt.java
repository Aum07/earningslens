package com.earningslens.generation;

import com.earningslens.retrieval.RetrievedChunk;

import java.util.List;

/**
 * The two messages sent to the chat model for one question.
 *
 * @param system  the system message (prompts/answer-system.txt)
 * @param user    the user message: numbered sources, then the question
 * @param sources the chunks in citation order: sources.get(0) is [1], sources.get(1) is [2], ...
 */
public record Prompt(String system, String user, List<RetrievedChunk> sources) {

    public Prompt {
        sources = List.copyOf(sources);
    }
}
