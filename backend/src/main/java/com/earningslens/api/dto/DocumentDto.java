package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.earningslens.ingest.DocType;

import java.util.UUID;

/** One document in the corpus, for the Corpus page (GET /api/v1/documents). */
public record DocumentDto(
        UUID id,
        @Schema(example = "MSFT")
        String ticker,
        @Schema(example = "Microsoft Corporation")
        String companyName,
        @Schema(example = "2026")
        int fiscalYear,
        @Schema(example = "2")
        int fiscalQuarter,
        DocType docType,
        @Schema(example = "https://www.sec.gov/Archives/edgar/data/789019/000119312526027198/msft-ex99_1.htm")
        String sourceUrl,
        @Schema(description = "How many chunks the document was split into")
        int chunkCount
) {
}
