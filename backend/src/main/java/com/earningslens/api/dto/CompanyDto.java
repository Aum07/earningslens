package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One company and the fiscal quarters held for it, for the filter dropdowns (GET /api/v1/companies).
 *
 * @param fiscalYearEndMonth 6 for Microsoft (fiscal year ends in June), 12 for Alphabet and Meta,
 *                           so the UI can explain why "FY2026 Q1" means different months per company
 * @param periods            quarters in the corpus, oldest first
 */
public record CompanyDto(
        @Schema(example = "MSFT") String ticker,
        @Schema(example = "Microsoft Corporation") String name,
        @Schema(description = "Month the fiscal year ends: 6 for Microsoft, 12 for Alphabet and Meta", example = "6") int fiscalYearEndMonth,
        @Schema(description = "Quarters in the corpus, oldest first") List<FiscalPeriodDto> periods) {

    public CompanyDto {
        periods = List.copyOf(periods);
    }
}
