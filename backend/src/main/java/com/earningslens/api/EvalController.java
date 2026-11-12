package com.earningslens.api;

import com.earningslens.api.dto.LiveConfigDto;
import com.earningslens.config.LlmProperties;
import com.earningslens.config.RetrievalProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * GET /api/v1/eval/latest (spec 10, roadmap E4.6): the latest eval report, exactly as the eval runner wrote it.
 * GET /api/v1/eval/live-config (E6.5): which ablation config and models the live chat API uses.
 */
@RestController
@RequestMapping("/api/v1/eval")
@Tag(name = "Evaluation")
public class EvalController {

    private final LatestEvalReport latest;
    private final RetrievalProperties retrieval;
    private final LlmProperties llm;

    public EvalController(LatestEvalReport latest, RetrievalProperties retrieval, LlmProperties llm) {
        this.latest = latest;
        this.retrieval = retrieval;
        this.llm = llm;
    }

    /** 404 until the first full eval run (Phase 5) has written latest.json. */
    @GetMapping("/latest")
    @Operation(summary = "Get the latest evaluation report",
            description = "The JSON report written by the evaluation runner (eval/reports/latest.json), returned "
                    + "unchanged. The report itself says which questions (split) and settings it covers.")
    @ApiResponse(responseCode = "200", description = "The report")
    @ApiResponse(responseCode = "404", description = "No evaluation has been run yet",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "No report yet", value = """
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "No evaluation report yet. Run the evaluation to create latest.json.",
                              "instance": "/api/v1/eval/latest"
                            }""")))
    public JsonNode latest() {
        return latest.read().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No evaluation report yet. Run the evaluation to create latest.json."));
    }

    /** The settings the chat endpoint really uses, so the UI never has to hard-code "config C". */
    @GetMapping("/live-config")
    @Operation(summary = "Get the configuration the live chat API runs",
            description = "The ablation config (A to D, see the evaluation report) and the models used by "
                    + "POST /api/v1/chat, read from the server's settings.")
    public LiveConfigDto liveConfig() {
        return new LiveConfigDto(retrieval.mode().name(), llm.chatModel(), llm.embeddingModel());
    }
}
