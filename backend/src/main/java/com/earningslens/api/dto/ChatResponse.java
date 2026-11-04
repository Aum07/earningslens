package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response of POST /api/v1/chat (spec 10).
 *
 * @param answer            the answer with [n] markers, or a friendly message when abstained
 * @param citations         one per marker used in the answer, in order of first use; empty when abstained
 * @param groundingWarnings citation warnings (removed markers, uncited sentences) and number warnings
 * @param appliedFilters    the companies, quarters and document types actually searched
 * @param model             the model that answered, or null if the model was not called
 */
public record ChatResponse(
        @Schema(description = "The answer with [n] citation markers, or a message saying why there is no answer")
        String answer,
        @Schema(description = "True when the documents do not contain the answer")
        boolean abstained,
        @Schema(description = "One per marker used in the answer, in order of first use")
        List<CitationDto> citations,
        @Schema(description = "Removed markers, sentences without a citation, numbers not found in a cited source")
        List<String> groundingWarnings,
        @Schema(description = "Companies, quarters and document types actually searched")
        ChatFilters appliedFilters,
        @Schema(description = "Server time for the whole request, in milliseconds")
        long latencyMs,
        @Schema(description = "Model that wrote the answer; null when no model was called", example = "gpt-4o-mini-2024-07-18")
        String model
) {

    public ChatResponse {
        citations = List.copyOf(citations);
        groundingWarnings = List.copyOf(groundingWarnings);
    }
}
