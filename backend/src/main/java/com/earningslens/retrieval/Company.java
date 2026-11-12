package com.earningslens.retrieval;

import java.util.List;

/**
 * One company from companies.yaml.
 *
 * @param fiscalYearEndMonth month the fiscal year ends (12 for Alphabet and Meta, 6 for Microsoft)
 * @param aliases            names a question may use for the company, matched case-insensitively
 * @param cik                SEC Central Index Key, 10 digits with leading zeros (for the SEC data APIs), or null
 */
public record Company(String ticker, String name, int fiscalYearEndMonth, List<String> aliases, String cik) {

    public Company {
        if (fiscalYearEndMonth < 1 || fiscalYearEndMonth > 12) {
            throw new IllegalArgumentException(ticker + ": fiscal-year-end-month must be 1 to 12");
        }
        aliases = List.copyOf(aliases);
    }

    /** Without a CIK (tests and callers that do not need SEC lookups). */
    public Company(String ticker, String name, int fiscalYearEndMonth, List<String> aliases) {
        this(ticker, name, fiscalYearEndMonth, aliases, null);
    }
}
