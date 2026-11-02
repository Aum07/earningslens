package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns a {@link ParsedQuestion} into concrete company + quarter targets, using the periods that
 * are actually in the database (roadmap E3.4, spec section 8 step 1).
 *
 * Periods are worked out per company, because the same words mean different quarters:
 * "latest quarter" is FY2027 Q1 for Microsoft but FY2026 Q3 for Alphabet, and September 2026
 * is in Microsoft's FY2027 Q1 (fiscal year ends in June) but Alphabet's FY2026 Q3.
 *
 * Rules:
 * - No company named: every company in companies.yaml is considered, but only pairs that
 *   exist in the corpus are kept (a question about "Q1 FY2027" only targets Microsoft).
 * - Explicit periods for a named company are kept even when not in the corpus, and listed in
 *   {@code missing}, so the answer can say "not in the corpus" instead of using another quarter.
 * - Derived periods (latest, last N, a quarter without a year, whole year, previous quarter,
 *   year ago, the quarters after a "since") only ever come from the corpus.
 */
public class PeriodResolver {

    private final CompanyDirectory companies;
    private final PeriodCatalog catalog;

    public PeriodResolver(CompanyDirectory companies, PeriodCatalog catalog) {
        this.companies = companies;
        this.catalog = catalog;
    }

    public ResolvedQuery resolve(ParsedQuestion parsed) {
        Map<String, NavigableSet<FiscalPeriod>> available = catalog.periodsByTicker();
        boolean companyNamed = !parsed.tickers().isEmpty();
        List<String> scope = companyNamed
                ? List.copyOf(new TreeSet<>(parsed.tickers()))
                : companies.all().stream().map(Company::ticker).sorted().toList();

        Set<PeriodTarget> targets = new TreeSet<>();
        Set<PeriodTarget> missing = new TreeSet<>();
        for (String ticker : scope) {
            NavigableSet<FiscalPeriod> inCorpus = available.getOrDefault(ticker, new TreeSet<>());
            int fyEnd = companies.byTicker(ticker).fiscalYearEndMonth();

            Set<FiscalPeriod> explicit = new TreeSet<>();
            Set<FiscalPeriod> derived = new TreeSet<>();
            for (PeriodRef ref : parsed.periodRefs()) {
                switch (ref) {
                    case PeriodRef.Fiscal f -> explicit.add(f.period());
                    case PeriodRef.CalendarMonth c -> explicit.add(fromCalendarMonth(c.year(), c.month(), fyEnd));
                    case PeriodRef.CalendarQuarter c -> explicit.add(fromCalendarMonth(c.year(), c.quarter() * 3, fyEnd));
                    case PeriodRef.Since s -> {
                        explicit.add(s.from());
                        inCorpus.tailSet(s.from(), false).forEach(derived::add);
                    }
                    case PeriodRef.QuarterOnly q -> inCorpus.descendingSet().stream()
                            .filter(p -> p.quarter() == q.quarter()).findFirst().ifPresent(derived::add);
                    case PeriodRef.WholeYear y -> {
                        List<FiscalPeriod> ofYear = inCorpus.stream().filter(p -> p.year() == y.fiscalYear()).toList();
                        if (ofYear.isEmpty()) {
                            for (int quarter = 1; quarter <= 4; quarter++) {
                                explicit.add(new FiscalPeriod(y.fiscalYear(), quarter));
                            }
                        } else {
                            derived.addAll(ofYear);
                        }
                    }
                    case PeriodRef.Latest l -> inCorpus.descendingSet().stream().limit(l.count()).forEach(derived::add);
                    case PeriodRef.PreviousQuarter p -> { /* applied below, once every anchor is known */ }
                    case PeriodRef.YearAgo y -> { /* applied below */ }
                }
            }
            applyModifiers(parsed.periodRefs(), inCorpus, explicit, derived);

            for (FiscalPeriod p : explicit) {
                PeriodTarget target = new PeriodTarget(ticker, p);
                if (inCorpus.contains(p)) {
                    targets.add(target);
                } else if (companyNamed) {
                    missing.add(target);
                }
            }
            derived.forEach(p -> targets.add(new PeriodTarget(ticker, p)));
        }

        // No company named and nothing found anywhere: report every explicit pair as missing.
        if (!companyNamed && targets.isEmpty()) {
            for (String ticker : scope) {
                int fyEnd = companies.byTicker(ticker).fiscalYearEndMonth();
                for (PeriodRef ref : parsed.periodRefs()) {
                    FiscalPeriod p = switch (ref) {
                        case PeriodRef.Fiscal f -> f.period();
                        case PeriodRef.Since s -> s.from();
                        case PeriodRef.CalendarMonth c -> fromCalendarMonth(c.year(), c.month(), fyEnd);
                        case PeriodRef.CalendarQuarter c -> fromCalendarMonth(c.year(), c.quarter() * 3, fyEnd);
                        default -> null;
                    };
                    if (p != null) {
                        missing.add(new PeriodTarget(ticker, p));
                    }
                }
            }
        }

        return new ResolvedQuery(parsed.tickers(), new ArrayList<>(targets), new ArrayList<>(missing),
                !parsed.periodRefs().isEmpty());
    }

    private static void applyModifiers(List<PeriodRef> refs, NavigableSet<FiscalPeriod> inCorpus,
                                       Set<FiscalPeriod> explicit, Set<FiscalPeriod> derived) {
        boolean previous = refs.stream().anyMatch(r -> r instanceof PeriodRef.PreviousQuarter);
        boolean yearAgo = refs.stream().anyMatch(r -> r instanceof PeriodRef.YearAgo);

        Set<FiscalPeriod> anchors = new TreeSet<>(explicit);
        anchors.addAll(derived);
        // "How did revenue change from the previous quarter?" with no period: compare with the latest.
        if (previous && anchors.isEmpty() && !inCorpus.isEmpty()) {
            anchors.add(inCorpus.last());
            derived.add(inCorpus.last());
        }
        for (FiscalPeriod anchor : anchors) {
            if (previous && inCorpus.contains(anchor.previous())) {
                derived.add(anchor.previous());
            }
            if (yearAgo && inCorpus.contains(anchor.yearAgo())) {
                derived.add(anchor.yearAgo());
            }
        }
    }

    /**
     * The fiscal quarter that contains a calendar month. The fiscal year is named after the
     * calendar year it ends in: Microsoft (ends June) has September 2026 in FY2027 Q1.
     */
    static FiscalPeriod fromCalendarMonth(int year, int month, int fiscalYearEndMonth) {
        int fiscalYear = month <= fiscalYearEndMonth ? year : year + 1;
        int monthsIntoYear = Math.floorMod(month - fiscalYearEndMonth - 1, 12);
        return new FiscalPeriod(fiscalYear, monthsIntoYear / 3 + 1);
    }
}
