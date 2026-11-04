package com.earningslens.api;

import com.earningslens.PgvectorContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Roadmap E4.8: the OpenAPI document lists every endpoint, the chat endpoint has request and response examples,
 * and Swagger UI is served. Starts the whole app (springdoc is auto-configured), so it needs Docker.
 */
@AutoConfigureMockMvc
class ApiDocsTest extends PgvectorContainerTest {

    @Autowired MockMvc mvc;

    @Test
    void openApiDocumentListsEveryEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("EarningsLens API"))
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/companies'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/eval/latest'].get").exists());
    }

    @Test
    void chatEndpointHasExamplesForRequestAndEveryResponse() throws Exception {
        String chat = "$.paths['/api/v1/chat'].post";
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(chat + ".summary").value("Ask a question about earnings"))
                .andExpect(jsonPath(chat + ".requestBody.content['application/json'].examples",
                        hasKey("Simple question")))
                .andExpect(jsonPath(chat + ".requestBody.content['application/json'].examples",
                        hasKey("With filters")))
                .andExpect(jsonPath(chat + ".responses['200'].content['application/json'].examples",
                        hasKey("Answer")))
                .andExpect(jsonPath(chat + ".responses['200'].content['application/json'].examples",
                        hasKey("Abstention")))
                .andExpect(jsonPath(chat + ".responses['400'].content['application/problem+json'].examples")
                        .value(notNullValue()))
                .andExpect(jsonPath(chat + ".responses['429']").exists())
                .andExpect(jsonPath(chat + ".responses['503']").exists());
    }

    @Test
    void schemasCarryFieldExamples() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.ChatRequest.properties.question.example")
                        .value(containsString("Microsoft")))
                .andExpect(jsonPath("$.components.schemas.CompanyDto.properties.fiscalYearEndMonth.example")
                        .exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui/index.html")));
    }
}
