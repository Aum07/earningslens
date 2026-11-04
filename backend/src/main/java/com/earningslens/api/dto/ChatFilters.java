package com.earningslens.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.earningslens.ingest.DocType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Filters in a chat request, and the filters actually applied in the response (spec 10).
 * A missing (null) list is read as an empty list, which means "no filter" for that field.
 */
public record ChatFilters(
        @Schema(description = "Tickers to search; empty means all companies", example = "[\"MSFT\"]")
        @Size(max = 10) List<@NotBlank @Size(max = 10) String> tickers,
        @Schema(description = "Fiscal quarters to search; empty means the question decides")
        @Size(max = 16) List<@NotNull @Valid FiscalPeriodDto> fiscalPeriods,
        @Schema(description = "Document types to search; empty means all", example = "[\"TRANSCRIPT\"]")
        @Size(max = 4) List<@NotNull DocType> docTypes
) {

    public ChatFilters {
        tickers = tickers == null ? List.of() : List.copyOf(tickers);
        fiscalPeriods = fiscalPeriods == null ? List.of() : List.copyOf(fiscalPeriods);
        docTypes = docTypes == null ? List.of() : List.copyOf(docTypes);
    }

    public static ChatFilters none() {
        return new ChatFilters(List.of(), List.of(), List.of());
    }
}
