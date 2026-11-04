package com.earningslens.api;

import com.earningslens.api.dto.ChatRequest;
import com.earningslens.api.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/v1/chat (spec 10, roadmap E4.5). @Valid checks the request (question 3 to 500 characters,
 * valid quarters) and answers 400 before anything else runs. Errors are ProblemDetail (GlobalExceptionHandler).
 * The annotations below only describe the endpoint for Swagger UI (E4.8); they do not change its behaviour.
 * The examples are real responses from a local run on 2026-11-04 (dev item q001), not invented ones.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Chat")
public class ChatController {

    static final String EXAMPLE_REQUEST = """
            {
              "question": "What was Microsoft's total revenue in Q2 FY2026?"
            }""";

    static final String EXAMPLE_REQUEST_WITH_FILTERS = """
            {
              "question": "How did Microsoft describe AI capex?",
              "filters": {
                "tickers": ["MSFT"],
                "fiscalPeriods": [{ "year": 2026, "quarter": 2 }, { "year": 2026, "quarter": 3 }],
                "docTypes": ["TRANSCRIPT"]
              }
            }""";

    static final String EXAMPLE_ANSWER = """
            {
              "answer": "Microsoft's total revenue in Q2 FY2026 was $81.3 billion, which represented an increase of 17% compared to the same quarter in the previous fiscal year [13].",
              "abstained": false,
              "citations": [
                {
                  "marker": 13,
                  "chunkId": "17c754c3-3cc6-4131-b94e-9091f92ef87e",
                  "ticker": "MSFT",
                  "fiscalYear": 2026,
                  "fiscalQuarter": 2,
                  "docType": "PRESS_RELEASE",
                  "page": null,
                  "section": "Microsoft Cloud and AI Strength Drives Second Quarter Results",
                  "speaker": null,
                  "sourceUrl": "https://www.sec.gov/Archives/edgar/data/789019/000119312526027198/msft-ex99_1.htm",
                  "snippet": "REDMOND, Wash. \u2014 January 28, 2026 \u2014 Microsoft Corp. today announced the following results for the quarter ended December 31, 2025, as compared to the corresponding period of last fiscal year: \u2022 Revenue was $81.3 billion and increased 17% (up 15% in constant currency) \u2022 Operating income was $38.3 bil"
                }
              ],
              "groundingWarnings": [],
              "appliedFilters": { "tickers": ["MSFT"], "fiscalPeriods": [{ "year": 2026, "quarter": 2 }], "docTypes": [] },
              "latencyMs": 6079,
              "model": "gpt-4o-mini-2024-07-18"
            }""";

    static final String EXAMPLE_ABSTENTION = """
            {
              "answer": "I can't answer this because the corpus does not include MSFT FY2024 Q1. EarningsLens only covers the quarters listed on the Corpus page.",
              "abstained": true,
              "citations": [],
              "groundingWarnings": [],
              "appliedFilters": { "tickers": ["MSFT"], "fiscalPeriods": [{ "year": 2024, "quarter": 1 }], "docTypes": [] },
              "latencyMs": 20,
              "model": null
            }""";

    static final String EXAMPLE_400 = """
            {
              "title": "Invalid request",
              "status": 400,
              "detail": "Invalid request: question: size must be between 3 and 500",
              "instance": "/api/v1/chat",
              "errors": [{ "field": "question", "message": "size must be between 3 and 500" }]
            }""";

    static final String EXAMPLE_429 = """
            {
              "title": "Too many requests",
              "status": 429,
              "detail": "Too many questions from your address. Please wait 5 seconds and try again.",
              "instance": "/api/v1/chat",
              "reason": "PER_IP",
              "retryAfterSeconds": 5
            }""";

    static final String EXAMPLE_503 = """
            {
              "title": "LLM unavailable",
              "status": 503,
              "detail": "The language model is not available right now. Please try again in a minute.",
              "instance": "/api/v1/chat"
            }""";

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Ask a question about earnings",
            description = """
                    Retrieves the most relevant chunks from the corpus, asks the model to answer using only those \
                    sources, then checks the answer on the server: citation markers that point to no source are \
                    removed, and numbers not found in a cited source are listed in groundingWarnings.

                    If the question asks for quarters that are not in the corpus, or the sources do not contain the \
                    answer, the response has abstained: true and a message saying what was searched.

                    Limits: 10 requests per minute per IP address and 300 per day in total (429 when exceeded).""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatRequest.class),
                            examples = {
                                    @ExampleObject(name = "Simple question", value = EXAMPLE_REQUEST),
                                    @ExampleObject(name = "With filters", value = EXAMPLE_REQUEST_WITH_FILTERS)
                            })))
    @ApiResponse(responseCode = "200", description = "A cited answer, or an abstention",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatResponse.class),
                    examples = {
                            @ExampleObject(name = "Answer", value = EXAMPLE_ANSWER),
                            @ExampleObject(name = "Abstention", value = EXAMPLE_ABSTENTION)
                    }))
    @ApiResponse(responseCode = "400", description = "The request is not valid (for example a question shorter than 3 characters)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Question too short", value = EXAMPLE_400)))
    @ApiResponse(responseCode = "429", description = "Rate limit reached; see the Retry-After header",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Per-IP limit", value = EXAMPLE_429)))
    @ApiResponse(responseCode = "503", description = "The language model could not be reached",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "LLM unavailable", value = EXAMPLE_503)))
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
