package com.earningslens.figures;

import com.earningslens.eval.FiscalPeriod;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Turns an SEC "company facts" document (data.sec.gov/api/xbrl/companyfacts/CIK##########.json) into quarterly
 * revenue, net income and diluted EPS.
 *
 * Things the raw data does that this class handles (checked on the MSFT, GOOGL and META files, 2026-11-05):
 * - The same period appears many times: later filings repeat earlier quarters as comparatives. Facts are grouped
 *   by (start, end) and the most recently filed one wins.
 * - The "fy" and "fp" fields describe the filing, not the period. The fiscal quarter is worked out from the end
 *   date and the company's fiscal year end month instead.
 * - Companies name revenue differently (Alphabet: Revenues; Microsoft and Meta:
 *   RevenueFromContractWithCustomerExcludingAssessedTax). The first concept with quarterly data is used.
 * - 10-K filings give the fourth quarter only as a full year. Q4 revenue and net income are derived as the full
 *   year minus the nine months to the end of Q3 (both filed figures); Q4 EPS is left empty because EPS cannot be
 *   subtracted (the share count changes during the year).
 */
public final class KeyFiguresParser {

    static final List<String> REVENUE_CONCEPTS =
            List.of("RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues");
    static final String NET_INCOME = "NetIncomeLoss";
    static final String EPS_DILUTED = "EarningsPerShareDiluted";

    private static final Set<String> FORMS = Set.of("10-Q", "10-K", "10-Q/A", "10-K/A");

    private KeyFiguresParser() {
    }

    /** One reported value for a period [start, end]. */
    record Fact(LocalDate start, LocalDate end, BigDecimal value, String form, LocalDate filed, String accn) {
        long days() {
            return ChronoUnit.DAYS.between(start, end);
        }
    }

    /** Quarterly figures by fiscal period, oldest first. */
    public static Map<FiscalPeriod, QuarterFigures> parse(JsonNode companyFacts, int fiscalYearEndMonth) {
        JsonNode gaap = companyFacts.path("facts").path("us-gaap");
        Map<Key, Fact> revenue = facts(gaap, REVENUE_CONCEPTS, "USD");
        Map<Key, Fact> netIncome = facts(gaap, List.of(NET_INCOME), "USD");
        Map<Key, Fact> eps = facts(gaap, List.of(EPS_DILUTED), "USD/shares");

        Map<FiscalPeriod, QuarterFigures> result = new TreeMap<>();
        // Quarters 1 to 3 (and any company that reports Q4 as three months): about 90-day periods.
        for (Fact f : merged(revenue, netIncome).values()) {
            if (!isQuarter(f)) {
                continue;
            }
            FiscalPeriod period = fiscalPeriod(f.end(), fiscalYearEndMonth);
            Key key = new Key(f.start(), f.end());
            Fact r = revenue.get(key);
            Fact n = netIncome.get(key);
            Fact e = eps.get(key);
            Fact source = latest(r, n, e);
            result.put(period, new QuarterFigures(period.year(), period.quarter(), f.end(),
                    value(r), value(n), value(e), false, source.form(), source.filed(), source.accn()));
        }
        // Q4 from the full year: full year minus the nine months that end where Q3 ends.
        for (Fact year : merged(revenue, netIncome).values()) {
            if (!isFullYear(year)) {
                continue;
            }
            FiscalPeriod q4 = fiscalPeriod(year.end(), fiscalYearEndMonth);
            if (q4.quarter() != 4 || result.containsKey(q4)) {
                continue;
            }
            Key yearKey = new Key(year.start(), year.end());
            BigDecimal rev = minusNineMonths(revenue, yearKey);
            BigDecimal net = minusNineMonths(netIncome, yearKey);
            if (rev == null && net == null) {
                continue;
            }
            Fact source = latest(revenue.get(yearKey), netIncome.get(yearKey), null);
            result.put(q4, new QuarterFigures(q4.year(), 4, year.end(), rev, net, null, true,
                    source.form(), source.filed(), source.accn()));
        }
        return result;
    }

    /**
     * The fiscal quarter that ends on this date. Example with a June year end (Microsoft): a quarter ending
     * 2025-09-30 is FY2026 Q1, one ending 2026-06-30 is FY2026 Q4. With a December year end it is the calendar.
     */
    static FiscalPeriod fiscalPeriod(LocalDate end, int fiscalYearEndMonth) {
        // A period that ends a few days into a month (52/53-week years) belongs to the month before.
        LocalDate d = end.getDayOfMonth() <= 7 ? end.minusDays(end.getDayOfMonth()) : end;
        int month = d.getMonthValue();
        int year = month <= fiscalYearEndMonth ? d.getYear() : d.getYear() + 1;
        int monthsIntoYear = Math.floorMod(month - fiscalYearEndMonth - 1, 12); // 0 to 11
        return new FiscalPeriod(year, monthsIntoYear / 3 + 1);
    }

    private record Key(LocalDate start, LocalDate end) {
    }

    private static boolean isQuarter(Fact f) {
        return f.days() >= 80 && f.days() <= 100;
    }

    private static boolean isFullYear(Fact f) {
        return f.days() >= 350 && f.days() <= 380;
    }

    private static boolean isNineMonths(Fact f) {
        return f.days() >= 260 && f.days() <= 285;
    }

    /** Full year minus the nine months starting on the same day, or null if either is missing. */
    private static BigDecimal minusNineMonths(Map<Key, Fact> facts, Key year) {
        Fact full = facts.get(year);
        if (full == null) {
            return null;
        }
        Optional<Fact> nine = facts.values().stream()
                .filter(f -> f.start().equals(year.start()) && isNineMonths(f))
                .findFirst();
        return nine.map(n -> full.value().subtract(n.value())).orElse(null);
    }

    /** Facts of the first concept (in order) that has at least one quarterly value, deduplicated by period. */
    private static Map<Key, Fact> facts(JsonNode gaap, List<String> concepts, String unit) {
        for (String concept : concepts) {
            Map<Key, Fact> byPeriod = new HashMap<>();
            for (JsonNode row : gaap.path(concept).path("units").path(unit)) {
                Optional<Fact> fact = fact(row);
                fact.ifPresent(f -> byPeriod.merge(new Key(f.start(), f.end()), f,
                        (a, b) -> b.filed().isAfter(a.filed()) ? b : a));
            }
            if (byPeriod.values().stream().anyMatch(KeyFiguresParser::isQuarter)) {
                return byPeriod;
            }
        }
        return Map.of();
    }

    private static Optional<Fact> fact(JsonNode row) {
        String form = row.path("form").asString("");
        if (!FORMS.contains(form) || !row.path("val").isNumber()
                || row.path("start").isMissingNode() || row.path("end").isMissingNode()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Fact(LocalDate.parse(row.path("start").asString()),
                    LocalDate.parse(row.path("end").asString()), row.path("val").decimalValue(), form,
                    LocalDate.parse(row.path("filed").asString()), row.path("accn").asString("")));
        } catch (RuntimeException e) {
            return Optional.empty(); // a malformed row is skipped, never fatal
        }
    }

    /** Every period that has revenue or net income. */
    private static Map<Key, Fact> merged(Map<Key, Fact> a, Map<Key, Fact> b) {
        Map<Key, Fact> all = new HashMap<>(b);
        all.putAll(a);
        return all;
    }

    private static BigDecimal value(Fact f) {
        return f == null ? null : f.value();
    }

    private static Fact latest(Fact... facts) {
        List<Fact> present = new ArrayList<>();
        for (Fact f : facts) {
            if (f != null) {
                present.add(f);
            }
        }
        return present.stream().max((x, y) -> x.filed().compareTo(y.filed())).orElseThrow();
    }
}
