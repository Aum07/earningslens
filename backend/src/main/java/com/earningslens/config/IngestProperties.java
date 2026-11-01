package com.earningslens.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ingestion paths, read from "earningslens.ingest".
 *
 * @param manifestPath the corpus manifest (the only list of documents to ingest)
 * @param rawDir       folder for downloaded files (gitignored)
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.ingest")
public record IngestProperties(
        @NotBlank String manifestPath,
        @NotBlank String rawDir
) {
}
