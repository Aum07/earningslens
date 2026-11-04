package com.earningslens.api;

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

/** GET /api/v1/eval/latest (spec 10, roadmap E4.6): the latest eval report, exactly as the eval runner wrote it. */
@RestController
@RequestMapping("/api/v1/eval")
@Tag(name = "Evaluation")
public class EvalController {

    private final LatestEvalReport latest;

    public EvalController(LatestEvalReport latest) {
        this.latest = latest;
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
}
