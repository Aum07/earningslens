package com.earningslens.ingest;

import java.util.Objects;

/**
 * One row of corpus/manifest.yaml.
 *
 * JAVA CONCEPT, compact constructor: the block below runs every time a record is
 * created, so an invalid entry (for example quarter 5) can never exist.
 */
public record ManifestEntry(
        String ticker,
        String companyName,
        int fiscalYear,
        int fiscalQuarter,
        DocType docType,
        String url,
        DocumentFormat format
) {

    public ManifestEntry {
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(companyName, "company_name");
        Objects.requireNonNull(docType, "doc_type");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(format, "format");
        if (ticker.isBlank() || ticker.length() > 10) {
            throw new IllegalArgumentException("ticker must be 1 to 10 characters, got '" + ticker + "'");
        }
        if (fiscalYear < 2000 || fiscalYear > 2100) {
            throw new IllegalArgumentException("fiscal_year looks wrong: " + fiscalYear);
        }
        if (fiscalQuarter < 1 || fiscalQuarter > 4) {
            throw new IllegalArgumentException("fiscal_quarter must be between 1 and 4, got " + fiscalQuarter);
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IllegalArgumentException("url must start with http(s)://, got '" + url + "'");
        }
    }

    /** File name in corpus/raw, for example MSFT_FY2026_Q2_PRESS_RELEASE.html. */
    public String rawFileName() {
        return "%s_FY%d_Q%d_%s.%s".formatted(ticker, fiscalYear, fiscalQuarter, docType, format.extension());
    }

    /** Period label used in logs and chunk headers, for example "FY2026 Q2". */
    public String periodLabel() {
        return "FY%d Q%d".formatted(fiscalYear, fiscalQuarter);
    }
}
