package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/chat (spec 10).
 *
 * @param question 3 to 500 characters
 * @param filters  optional; each of its fields is optional too
 */
public record ChatRequest(
        @Schema(description = "The question, 3 to 500 characters", example = "What was Microsoft's total revenue in Q2 FY2026?")
        @NotBlank @Size(min = 3, max = 500) String question,
        @Schema(description = "Optional filters; any field may be left out")
        @Valid ChatFilters filters
) {
}
