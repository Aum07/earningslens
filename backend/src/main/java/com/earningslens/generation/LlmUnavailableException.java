package com.earningslens.generation;

/**
 * The chat model could not produce an answer (network error, HTTP error, timeout or an empty reply).
 * The API maps this to 503 "LLM unavailable" (spec 10, roadmap E4.7).
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmUnavailableException(String message) {
        super(message);
    }
}
