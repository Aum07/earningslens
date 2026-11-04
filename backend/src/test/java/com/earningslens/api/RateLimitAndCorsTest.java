package com.earningslens.api;

import com.earningslens.api.dto.ChatFilters;
import com.earningslens.api.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roadmap E4.7: 429 after the per-IP limit and the daily cap, and CORS only for the frontend origin. */
@WebMvcTest(ChatController.class)
@TestPropertySource(properties = {
        "earningslens.rate-limit.requests-per-minute-per-ip=2",
        "earningslens.rate-limit.daily-chat-cap=3",
        "earningslens.api.cors-allowed-origins=http://localhost:4200"
})
class RateLimitAndCorsTest {

    @Autowired MockMvc mvc;
    @Autowired RateLimitInterceptor rateLimit;

    @MockitoBean ChatService chatService;

    @BeforeEach
    void setUp() {
        rateLimit.reset();
        when(chatService.chat(any())).thenReturn(
                new ChatResponse("ok", true, List.of(), List.of(), ChatFilters.none(), 1, null));
    }

    @Test
    void thirdRequestInAMinuteFromOneAddressIs429() throws Exception {
        chatFrom("10.0.0.1").andExpect(status().isOk());
        chatFrom("10.0.0.1").andExpect(status().isOk());

        chatFrom("10.0.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.reason").value("PER_IP"));
        verify(chatService, times(2)).chat(any());   // the refused request never reached the service
    }

    @Test
    void dailyCapAppliesAcrossAddresses() throws Exception {
        chatFrom("10.0.0.1").andExpect(status().isOk());
        chatFrom("10.0.0.2").andExpect(status().isOk());
        chatFrom("10.0.0.3").andExpect(status().isOk());

        chatFrom("10.0.0.4")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("DAILY_CAP"));
    }

    @Test
    void frontendOriginMayCallTheApi() throws Exception {
        mvc.perform(options("/api/v1/chat")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void otherOriginsAreRefused() throws Exception {
        mvc.perform(options("/api/v1/chat")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        verify(chatService, never()).chat(any());
    }

    private ResultActions chatFrom(String ip) throws Exception {
        return mvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"What was Microsoft revenue?\"}")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }));
    }
}
