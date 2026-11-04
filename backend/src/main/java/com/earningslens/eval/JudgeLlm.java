package com.earningslens.eval;

import com.earningslens.generation.LlmReply;

/**
 * Sends one system + user message pair to the judge model and forces the reply into a JSON schema.
 * Separate from the answer model's ChatLlm: a different model, a bigger output limit and structured output.
 * An interface so JudgeClient tests can use a fake (no OpenAI calls, no cost).
 */
@FunctionalInterface
public interface JudgeLlm {

    /**
     * @param jsonSchema the JSON schema the reply must follow (OpenAI structured output, strict mode)
     * @throws RuntimeException on any failure (network, HTTP error, timeout)
     */
    LlmReply complete(String systemMessage, String userMessage, String jsonSchema);
}
