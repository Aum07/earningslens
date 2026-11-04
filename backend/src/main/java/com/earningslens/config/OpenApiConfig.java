package com.earningslens.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Title and description of the API docs (spec 4, roadmap E4.8). springdoc-openapi builds the OpenAPI document
 * from the controllers and their annotations; Swagger UI is at /swagger-ui.html, the JSON at /v3/api-docs.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "EarningsLens API",
                version = "v1",
                description = """
                        Questions about Microsoft, Alphabet and Meta earnings, answered only from their press \
                        releases, earnings call transcripts and 10-Q/10-K filings. Every factual sentence carries \
                        [n] citation markers; the server checks the markers and the numbers. When the documents do \
                        not contain the answer, the response says so (abstained: true).

                        Not investment advice. Answers come only from the listed documents."""),
        tags = {
                @Tag(name = "Chat", description = "Ask a question and get a cited answer"),
                @Tag(name = "Corpus", description = "Which companies, quarters and documents are covered"),
                @Tag(name = "Evaluation", description = "The latest evaluation report")
        })
public class OpenApiConfig {
}
