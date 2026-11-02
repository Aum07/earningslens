package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;

/**
 * A period reference found in a question, before it is turned into concrete fiscal periods.
 *
 * "Explicit" references name a period directly; they are kept even when the corpus does not
 * have them, so the answer can say "not in the corpus" instead of answering about another
 * quarter. "Derived" references are worked out from the periods that exist in the database.
 */
public sealed interface PeriodRef {

    /** Explicit fiscal label: "Q2 FY2026", "Q1 2026", "first quarter of fiscal 2027". */
    record Fiscal(FiscalPeriod period) implements PeriodRef {
    }

    /** Explicit calendar month, converted per company: "December 2025", "quarter ended September 30, 2026". */
    record CalendarMonth(int year, int month) implements PeriodRef {
    }

    /** Explicit calendar quarter, converted per company: "calendar Q3 2026", "CQ3 2026". */
    record CalendarQuarter(int year, int quarter) implements PeriodRef {
    }

    /** Explicit start, derived rest: "since Q4 2025" is FY2025 Q4 plus every later quarter in the corpus. */
    record Since(FiscalPeriod from) implements PeriodRef {
    }

    /** Derived: a quarter without a year ("Q3", "the third quarter"): the latest such quarter in the corpus. */
    record QuarterOnly(int quarter) implements PeriodRef {
    }

    /** Derived: every quarter of a fiscal year that is in the corpus ("each quarter of 2026 so far"). */
    record WholeYear(int fiscalYear) implements PeriodRef {
    }

    /** Derived: the latest n quarters in the corpus ("latest quarter", "last three quarters"). */
    record Latest(int count) implements PeriodRef {
    }

    /** Modifier: also the quarter before each other period ("previous quarter", "quarter over quarter"). */
    record PreviousQuarter() implements PeriodRef {
    }

    /** Modifier: also the same quarter a year earlier ("year over year"), if it is in the corpus. */
    record YearAgo() implements PeriodRef {
    }
}
