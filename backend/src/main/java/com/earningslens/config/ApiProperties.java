package com.earningslens.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Settings for the REST API, read from "earningslens.api".
 *
 * @param latestEvalReport    path of the eval report served by GET /api/v1/eval/latest (spec 11.5: the eval runner
 *                            keeps eval/reports/latest.json updated). Relative paths start from the working folder,
 *                            which is backend/ when the app runs from IntelliJ or Maven.
 * @param corsAllowedOrigins  browser origins allowed to call the API (spec 13: only the frontend), for example
 *                            http://localhost:4200 for the Angular dev server
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.api")
public record ApiProperties(
        @NotBlank String latestEvalReport,
        @NotEmpty List<@NotBlank String> corsAllowedOrigins
) {
}
