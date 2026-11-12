package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What the live chat API runs right now (GET /api/v1/eval/live-config), read from application.yaml.
 * The Evaluation page uses it to say which row of the ablation table the API matches. latest.json cannot say
 * this: it records what was measured, and the API mode was changed after the test run (docs/DECISIONS.md, E5.5).
 */
public record LiveConfigDto(
        @Schema(description = "Ablation config used by POST /api/v1/chat: A, B, C or D (spec 11.4)", example = "C")
        String retrievalMode,
        @Schema(example = "gpt-4o-mini") String chatModel,
        @Schema(example = "text-embedding-3-small") String embeddingModel
) {
}
