package com.earningslens.figures;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Official figures for one fiscal quarter, from the company's own XBRL data on SEC EDGAR.
 *
 * JAVA CONCEPT, BigDecimal: an exact decimal number. Used instead of double so that money (81273000000) and
 * EPS (3.72) are kept exactly as filed, and so that "full year minus nine months" has no rounding error.
 *
 * @param revenue          total revenue in US dollars, or null if not reported
 * @param netIncome        net income in US dollars, or null if not reported
 * @param epsDiluted       diluted earnings per share in US dollars, or null (never derived for Q4)
 * @param derived          true when revenue and net income were calculated as full year minus the first nine
 *                         months, because 10-K filings report the fourth quarter only as part of the full year
 * @param accessionNumber  SEC accession number of the filing the figures come from (the 10-K for a derived Q4)
 */
public record QuarterFigures(
        int fiscalYear,
        int fiscalQuarter,
        LocalDate periodEnd,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal epsDiluted,
        boolean derived,
        String form,
        LocalDate filed,
        String accessionNumber
) {
}
