package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roadmap E3.4: QueryParser + PeriodResolver against the case table in
 * src/test/resources/retrieval/query-cases.txt (question in, filters out). No database:
 * the corpus periods are a fixed in-memory catalog, the companies come from the real companies.yaml.
 */
class QueryParserTest {

    /** The periods in the real corpus (STATUS: corpus coverage). */
    static final PeriodCatalog CORPUS = () -> {
        Map<String, NavigableSet<FiscalPeriod>> map = new TreeMap<>();
        map.put("GOOGL", periods(2025, 4, 2026, 1, 2026, 2, 2026, 3));
        map.put("META", periods(2025, 4, 2026, 1, 2026, 2, 2026, 3));
        map.put("MSFT", periods(2026, 2, 2026, 3, 2026, 4, 2027, 1));
        return map;
    };

    static CompanyDirectory companies;
    static QueryParser parser;
    static PeriodResolver resolver;

    @BeforeAll
    static void setUp() throws IOException {
        try (InputStream in = QueryParserTest.class.getResourceAsStream("/companies.yaml")) {
            companies = CompanyDirectory.load(in);
        }
        parser = new QueryParser(companies);
        resolver = new PeriodResolver(companies, CORPUS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void caseTable(String question, String tickers, String targets, String missing) {
        ResolvedQuery resolved = resolver.resolve(parser.parse(question));

        assertThat(join(new TreeSet<>(resolved.tickers()))).as("tickers").isEqualTo(tickers);
        assertThat(join(resolved.targets())).as("targets").isEqualTo(targets);
        assertThat(join(resolved.missing())).as("missing").isEqualTo(missing);
    }

    static Stream<Arguments> cases() throws IOException {
        List<Arguments> cases = new ArrayList<>();
        try (InputStream in = QueryParserTest.class.getResourceAsStream("/retrieval/query-cases.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.strip().startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length != 4) {
                    throw new IllegalArgumentException("expected 4 columns: " + line);
                }
                cases.add(Arguments.of(Arrays.stream(parts).map(String::strip).toArray()));
            }
        }
        assertThat(cases).as("roadmap asks for at least 25 cases").hasSizeGreaterThanOrEqualTo(25);
        return cases.stream();
    }

    @Test
    void calendarMonthsMapToFiscalQuartersPerCompany() {
        // Microsoft: fiscal year ends in June, named after the calendar year it ends in.
        assertThat(PeriodResolver.fromCalendarMonth(2026, 7, 6)).isEqualTo(new FiscalPeriod(2027, 1));
        assertThat(PeriodResolver.fromCalendarMonth(2026, 9, 6)).isEqualTo(new FiscalPeriod(2027, 1));
        assertThat(PeriodResolver.fromCalendarMonth(2026, 12, 6)).isEqualTo(new FiscalPeriod(2027, 2));
        assertThat(PeriodResolver.fromCalendarMonth(2026, 3, 6)).isEqualTo(new FiscalPeriod(2026, 3));
        assertThat(PeriodResolver.fromCalendarMonth(2026, 6, 6)).isEqualTo(new FiscalPeriod(2026, 4));
        // Alphabet and Meta: fiscal year = calendar year.
        assertThat(PeriodResolver.fromCalendarMonth(2026, 1, 12)).isEqualTo(new FiscalPeriod(2026, 1));
        assertThat(PeriodResolver.fromCalendarMonth(2026, 9, 12)).isEqualTo(new FiscalPeriod(2026, 3));
        assertThat(PeriodResolver.fromCalendarMonth(2025, 12, 12)).isEqualTo(new FiscalPeriod(2025, 4));
    }

    @Test
    void companiesYamlIsLoaded() {
        assertThat(companies.all()).extracting(Company::ticker).containsExactly("MSFT", "GOOGL", "META");
        assertThat(companies.byTicker("MSFT").fiscalYearEndMonth()).isEqualTo(6);
        assertThat(companies.findTickers("Meta Platforms and META and Facebook")).containsExactly("META");
        assertThat(companies.findTickers("metadata, googled, microsofts")).isEmpty();
    }

    @Test
    void periodMentionedTellsNoPeriodApartFromMissingPeriod() {
        ResolvedQuery noPeriod = resolver.resolve(parser.parse("What did Meta say about AI?"));
        assertThat(noPeriod.periodMentioned()).isFalse();
        assertThat(noPeriod.onlyMissingPeriods()).isFalse();

        ResolvedQuery missing = resolver.resolve(parser.parse("Meta revenue in Q4 2026"));
        assertThat(missing.periodMentioned()).isTrue();
        assertThat(missing.onlyMissingPeriods()).isTrue();
    }

    @Test
    void twoDigitAndFiscalYearSpellings() {
        assertThat(QueryParser.year("fy26")).isEqualTo(2026);
        assertThat(QueryParser.year("fy '27")).isEqualTo(2027);
        assertThat(QueryParser.year("fiscal year 2026")).isEqualTo(2026);
        assertThat(QueryParser.year("2025")).isEqualTo(2025);
    }

    // ---- helpers ----

    private static NavigableSet<FiscalPeriod> periods(int... yearQuarterPairs) {
        NavigableSet<FiscalPeriod> set = new TreeSet<>();
        for (int i = 0; i < yearQuarterPairs.length; i += 2) {
            set.add(new FiscalPeriod(yearQuarterPairs[i], yearQuarterPairs[i + 1]));
        }
        return set;
    }

    private static String join(Iterable<?> items) {
        List<String> parts = new ArrayList<>();
        items.forEach(i -> parts.add(String.valueOf(i)));
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }
}
