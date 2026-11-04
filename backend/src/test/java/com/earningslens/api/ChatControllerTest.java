package com.earningslens.api;

import com.earningslens.api.dto.ChatFilters;
import com.earningslens.api.dto.ChatRequest;
import com.earningslens.api.dto.ChatResponse;
import com.earningslens.api.dto.CitationDto;
import com.earningslens.api.dto.FiscalPeriodDto;
import com.earningslens.ingest.DocType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Roadmap E4.5: the HTTP layer only (MockMvc, no server, no database, no OpenAI). ChatService is a mock,
 * so this checks the JSON shape from spec 10 and the request validation.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ChatService chatService;

    private static final UUID CHUNK = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void returnsTheSpecResponseShape() throws Exception {
        when(chatService.chat(any())).thenReturn(new ChatResponse(
                "Revenue was $81.3 billion [1].", false,
                List.of(new CitationDto(1, CHUNK, "MSFT", 2026, 2, DocType.PRESS_RELEASE, null, "Segment Results",
                        null, "https://example.com/pr", "Revenue was $81.3 billion")),
                List.of("Not found in the cited sources: 52%"),
                new ChatFilters(List.of("MSFT"), List.of(new FiscalPeriodDto(2026, 2)), List.of()),
                2140, "gpt-4o-mini"));

        mvc.perform(post("/api/v1/chat").contentType(MediaType.APPLICATION_JSON).content("""
                        {"question": "What was Microsoft's total revenue in Q2 FY2026?"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Revenue was $81.3 billion [1]."))
                .andExpect(jsonPath("$.abstained").value(false))
                .andExpect(jsonPath("$.citations", hasSize(1)))
                .andExpect(jsonPath("$.citations[0].marker").value(1))
                .andExpect(jsonPath("$.citations[0].chunkId").value(CHUNK.toString()))
                .andExpect(jsonPath("$.citations[0].ticker").value("MSFT"))
                .andExpect(jsonPath("$.citations[0].fiscalYear").value(2026))
                .andExpect(jsonPath("$.citations[0].fiscalQuarter").value(2))
                .andExpect(jsonPath("$.citations[0].docType").value("PRESS_RELEASE"))
                .andExpect(jsonPath("$.citations[0].page").value(nullValue()))
                .andExpect(jsonPath("$.citations[0].section").value("Segment Results"))
                .andExpect(jsonPath("$.citations[0].speaker").value(nullValue()))
                .andExpect(jsonPath("$.citations[0].sourceUrl").value("https://example.com/pr"))
                .andExpect(jsonPath("$.citations[0].snippet").value("Revenue was $81.3 billion"))
                .andExpect(jsonPath("$.groundingWarnings[0]").value("Not found in the cited sources: 52%"))
                .andExpect(jsonPath("$.appliedFilters.tickers[0]").value("MSFT"))
                .andExpect(jsonPath("$.appliedFilters.fiscalPeriods[0].year").value(2026))
                .andExpect(jsonPath("$.appliedFilters.fiscalPeriods[0].quarter").value(2))
                .andExpect(jsonPath("$.appliedFilters.docTypes", hasSize(0)))
                .andExpect(jsonPath("$.latencyMs").value(2140))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"));
    }

    @Test
    void filtersAreReadFromTheRequest() throws Exception {
        when(chatService.chat(any())).thenReturn(emptyResponse());

        mvc.perform(post("/api/v1/chat").contentType(MediaType.APPLICATION_JSON).content("""
                        {"question": "How did Microsoft describe AI capex?",
                         "filters": {"tickers": ["MSFT"], "fiscalPeriods": [{"year": 2026, "quarter": 1}],
                                     "docTypes": ["TRANSCRIPT"]}}"""))
                .andExpect(status().isOk());

        ArgumentCaptor<ChatRequest> sent = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatService).chat(sent.capture());
        assertThat(sent.getValue().filters()).isEqualTo(new ChatFilters(List.of("MSFT"),
                List.of(new FiscalPeriodDto(2026, 1)), List.of(DocType.TRANSCRIPT)));
    }

    @Test
    void questionTooShortIsRejected() throws Exception {
        expectBadRequest("""
                {"question": "ab"}""");
    }

    @Test
    void questionTooLongIsRejected() throws Exception {
        expectBadRequest("{\"question\": \"" + "a".repeat(501) + "\"}");
    }

    @Test
    void missingQuestionIsRejected() throws Exception {
        expectBadRequest("""
                {"filters": {"tickers": ["MSFT"]}}""");
    }

    @Test
    void invalidQuarterIsRejected() throws Exception {
        expectBadRequest("""
                {"question": "Revenue?", "filters": {"fiscalPeriods": [{"year": 2026, "quarter": 5}]}}""");
    }

    @Test
    void unknownDocTypeIsRejected() throws Exception {
        expectBadRequest("""
                {"question": "Revenue?", "filters": {"docTypes": ["BLOG_POST"]}}""");
    }

    private void expectBadRequest(String body) throws Exception {
        mvc.perform(post("/api/v1/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(chatService, never()).chat(any());
    }

    private static ChatResponse emptyResponse() {
        return new ChatResponse("Nothing found.", true, List.of(), List.of(), ChatFilters.none(), 5, null);
    }
}
