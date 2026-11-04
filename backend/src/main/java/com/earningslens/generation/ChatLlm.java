package com.earningslens.generation;

/**
 * Sends one system + user message pair to the chat model and returns its reply.
 * An interface so AnswerService tests can use a fake (no OpenAI calls, no cost).
 * The real implementation is {@link OpenAiChatLlm}.
 */
@FunctionalInterface
public interface ChatLlm {

    /**
     * @throws RuntimeException on any failure (network, HTTP error, timeout); AnswerService turns it
     *                          into {@link LlmUnavailableException}
     */
    LlmReply complete(String systemMessage, String userMessage);
}
