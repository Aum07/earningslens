package com.earningslens.generation;

/**
 * What the chat model sent back for one call.
 *
 * @param text             the reply text (may be empty if the model returned nothing)
 * @param model            the model that answered, as reported by the API (for example "gpt-4o-mini-2024-07-18")
 * @param promptTokens     input tokens billed, or null if the API did not report them
 * @param completionTokens output tokens billed, or null if the API did not report them
 * @param truncated        true if the reply stopped at the output token limit (finish reason LENGTH)
 */
public record LlmReply(String text, String model, Integer promptTokens, Integer completionTokens,
                       boolean truncated) {
}
