package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;

import java.util.Comparator;

/** One company and fiscal quarter to search, for example MSFT FY2027 Q1. */
public record PeriodTarget(String ticker, FiscalPeriod period) implements Comparable<PeriodTarget> {

    private static final Comparator<PeriodTarget> ORDER =
            Comparator.comparing(PeriodTarget::ticker).thenComparing(PeriodTarget::period);

    @Override
    public int compareTo(PeriodTarget other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return ticker + " " + period;
    }
}
