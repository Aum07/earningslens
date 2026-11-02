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

    /** The quarter before this one (FY2026 Q1 -> FY2025 Q4). */
    public FiscalPeriod previous() {
        return quarter == 1 ? new FiscalPeriod(year - 1, 4) : new FiscalPeriod(year, quarter - 1);
    }

    /** The quarter after this one (FY2025 Q4 -> FY2026 Q1). */
    public FiscalPeriod next() {
        return quarter == 4 ? new FiscalPeriod(year + 1, 1) : new FiscalPeriod(year, quarter + 1);
    }

    /** The same quarter one fiscal year earlier, for year-over-year questions. */
    public FiscalPeriod yearAgo() {
        return new FiscalPeriod(year - 1, quarter);
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
