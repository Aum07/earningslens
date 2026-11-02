package com.earningslens.eval;

/** A fiscal quarter, for example FY2026 Q1. */
public record FiscalPeriod(int year, int quarter) implements Comparable<FiscalPeriod> {

    public FiscalPeriod {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("fiscal year looks wrong: " + year);
        }
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("fiscal quarter must be between 1 and 4, got " + quarter);
        }
    }

    @Override
    public int compareTo(FiscalPeriod other) {
        return year != other.year ? Integer.compare(year, other.year) : Integer.compare(quarter, other.quarter);
    }

    @Override
    public String toString() {
        return "FY%d Q%d".formatted(year, quarter);
    }
}
