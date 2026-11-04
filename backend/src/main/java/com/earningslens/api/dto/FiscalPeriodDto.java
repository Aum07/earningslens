package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** A fiscal quarter in the API: { "year": 2026, "quarter": 1 }. */
public record FiscalPeriodDto(
        @Schema(description = "Fiscal year as the company reports it", example = "2026")
        @NotNull @Min(2000) @Max(2100) Integer year,
        @Schema(description = "Fiscal quarter, 1 to 4", example = "2")
        @NotNull @Min(1) @Max(4) Integer quarter
) {
}
