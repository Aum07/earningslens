package com.earningslens.api;

import com.earningslens.generation.LlmUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Roadmap E4.7: every error is an RFC 7807 ProblemDetail (application/problem+json) and no body ever shows
 * a stack trace, an exception class name or an internal message.
 */
@WebMvcTest(ChatController.class)
class ErrorHandlingTest {

    @Autowired MockMvc mvc;
    @Autowired RateLimitInterceptor rateLimit;

    @MockitoBean ChatService chatService;

    @BeforeEach
    void resetLimits() {
        rateLimit.reset();
    }

    @Test
    void validationErrorNamesTheFieldAndTheRule() throws Exception {
        noLeaks(chat("{\"question\": \"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value(containsString("question")))
                .andExpect(jsonPath("$.errors[0].field").value("question"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("between 3 and 500")));
    }

    @Test
    void brokenJsonIsA400Problem() throws Exception {
        noLeaks(chat("{\"question\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void llmFailureIsA503Problem() throws Exception {
        when(chatService.chat(any())).thenThrow(new LlmUnavailableException("The language model is unavailable",
                new IllegalStateException("HTTP 500 from api.openai.com, key sk-secret")));

        noLeaks(chat("{\"question\": \"What was Microsoft revenue?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("LLM unavailable"))
                .andExpect(content().string(not(containsString("sk-secret"))));
    }

    @Test
    void unexpectedErrorIsA500WithoutInternalDetails() throws Exception {
        when(chatService.chat(any())).thenThrow(new IllegalStateException("SELECT * FROM secret_table failed"));

        noLeaks(chat("{\"question\": \"What was Microsoft revenue?\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("secret_table"))));
    }

    @Test
    void unknownUrlIsA404Problem() throws Exception {
        noLeaks(mvc.perform(get("/api/v1/does-not-exist")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void wrongMethodIsA405Problem() throws Exception {
        noLeaks(mvc.perform(get("/api/v1/chat")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    private ResultActions chat(String body) throws Exception {
        return mvc.perform(post("/api/v1/chat").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Problem JSON, and nothing that looks like a stack trace or a Java class name. */
    private static ResultActions noLeaks(ResultActions result) throws Exception {
        return result
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("\tat "))))
                .andExpect(content().string(not(containsString("com.earningslens"))))
                .andExpect(content().string(not(containsString("trace"))));
    }
}
