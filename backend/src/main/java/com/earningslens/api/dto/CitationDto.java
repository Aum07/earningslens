package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.earningslens.ingest.DocType;

import java.util.UUID;

/**
 * One source cited in the answer (spec 10).
 *
 * @param marker  the number used in the answer text, [marker]
 * @param page    first page of the chunk (PDF only), else null
 * @param snippet the first 300 characters of the chunk text, without its header lines
 */
public record CitationDto(
        @Schema(description = "The n in [n] in the answer", example = "13")
        int marker,
        UUID chunkId,
        @Schema(example = "MSFT")
        String ticker,
        int fiscalYear,
        int fiscalQuarter,
        DocType docType,
        @Schema(description = "First page of the chunk (PDF documents only)")
        Integer page,
        String section,
        @Schema(description = "Speaker (transcripts only)")
        String speaker,
        @Schema(description = "Link to the original document", example = "https://www.sec.gov/Archives/edgar/data/789019/000119312526027198/msft-ex99_1.htm")
        String sourceUrl,
        @Schema(description = "First 300 characters of the chunk")
        String snippet
) {
}
