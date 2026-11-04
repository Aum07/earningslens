package com.earningslens.api;

import com.earningslens.generation.LlmUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Every API error as an RFC 7807 ProblemDetail (spec 10, roadmap E4.7): 400 validation, 404, 429 rate limited,
 * 503 LLM unavailable, 500 anything else. Bodies never contain stack traces, exception class names or internal
 * messages; the details go to the server log instead.
 *
 * The parent class (ResponseEntityExceptionHandler) already turns Spring MVC's own errors into ProblemDetail:
 * unreadable JSON (400), unknown URL (404), wrong HTTP method (405), ResponseStatusException (its status).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400 with one entry per invalid field, for example question: size must be between 3 and 500. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::fieldError)
                .toList();
        String summary = errors.stream()
                .map(e -> e.get("field") + ": " + e.get("message"))
                .reduce((a, b) -> a + "; " + b)
                .orElse("the request is not valid");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request: " + summary);
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private static Map<String, String> fieldError(FieldError error) {
        String message = error.getDefaultMessage() == null ? "is not valid" : error.getDefaultMessage();
        return Map.of("field", error.getField(), "message", message);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> rateLimited(RateLimitExceededException ex) {
        String detail = switch (ex.reason()) {
            case PER_IP -> "Too many questions from your address. Please wait %d seconds and try again."
                    .formatted(ex.retryAfterSeconds());
            case DAILY_CAP -> "This demo has reached its daily limit of questions. Please try again tomorrow.";
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail);
        problem.setTitle("Too many requests");
        problem.setProperty("reason", ex.reason().name());
        problem.setProperty("retryAfterSeconds", ex.retryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ProblemDetail> llmUnavailable(LlmUnavailableException ex) {
        log.warn("Answered 503: {}", ex.getMessage(), ex.getCause());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The language model is not available right now. Please try again in a minute.");
        problem.setTitle("LLM unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    /** Anything unexpected: logged in full on the server, a plain message for the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        log.error("Unexpected error, answered 500", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again.");
        problem.setTitle("Internal error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
