package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Official quarterly figures for one company (GET /api/v1/key-figures), from SEC EDGAR XBRL data.
 *
 * @param available false when the SEC data could not be loaded right now (then quarters is empty)
 * @param quarters  only the quarters that are in the corpus, oldest first
 */
public record KeyFiguresDto(
        @Schema(example = "MSFT") String ticker,
        @Schema(example = "Microsoft Corporation") String name,
        boolean available,
        @Schema(description = "The SEC API document the figures were read from")
        String sourceUrl,
        @Schema(description = "When the backend last downloaded the SEC data") Instant fetchedAt,
        List<Quarter> quarters
) {
    public KeyFiguresDto {
        quarters = List.copyOf(quarters);
    }

    /**
     * @param derived true when revenue and net income are full year minus nine months (fourth quarters)
     */
    public record Quarter(
            @Schema(example = "2026") int fiscalYear,
            @Schema(example = "2") int fiscalQuarter,
            LocalDate periodEnd,
            @Schema(description = "US dollars", example = "81273000000") BigDecimal revenue,
            @Schema(description = "US dollars", example = "38458000000") BigDecimal netIncome,
            @Schema(description = "US dollars per share; empty for derived quarters", example = "5.16")
            BigDecimal epsDiluted,
            boolean derived,
            @Schema(example = "10-Q") String form,
            LocalDate filed,
            @Schema(description = "Index page of the filing on SEC EDGAR") String filingUrl
    ) {
    }
}
