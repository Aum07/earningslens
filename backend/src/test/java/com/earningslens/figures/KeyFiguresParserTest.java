package com.earningslens.figures;

import com.earningslens.eval.FiscalPeriod;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The SEC company-facts quirks, using real values from the MSFT and GOOGL files on data.sec.gov (2026-11-05):
 * repeated comparatives, fy/fp describing the filing, Q4 only as a full year, different revenue concept names.
 */
class KeyFiguresParserTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** One XBRL row as data.sec.gov writes it. */
    private static String row(String start, String end, String val, String form, String filed, String accn) {
        return """
                {"start":"%s","end":"%s","val":%s,"accn":"%s","fy":2026,"fp":"Q1","form":"%s","filed":"%s"}"""
                .formatted(start, end, val, accn, form, filed);
    }

    private static String facts(String revenueConcept, String revenueRows, String netRows, String epsRows) {
        return """
                {"cik":789019,"entityName":"Test","facts":{"us-gaap":{
                  "%s":{"units":{"USD":[%s]}},
                  "NetIncomeLoss":{"units":{"USD":[%s]}},
                  "EarningsPerShareDiluted":{"units":{"USD/shares":[%s]}}
                }}}""".formatted(revenueConcept, revenueRows, netRows, epsRows);
    }

    /** Microsoft FY2026 (year ends in June): Q1-Q3 from 10-Qs, nine months, full year from the 10-K. */
    private static final String MSFT = facts("RevenueFromContractWithCustomerExcludingAssessedTax",
            String.join(",",
                    row("2025-07-01", "2025-09-30", "77673000000", "10-Q", "2025-10-29", "0000950170-25-000001"),
                    // the same quarter repeated as a comparative in the FY2027 Q1 10-Q, filed later
                    row("2025-07-01", "2025-09-30", "77673000000", "10-Q", "2026-10-28", "0000950170-26-000009"),
                    row("2025-10-01", "2025-12-31", "81273000000", "10-Q", "2026-01-28", "0000950170-26-000002"),
                    row("2026-01-01", "2026-03-31", "82886000000", "10-Q", "2026-04-29", "0000950170-26-000003"),
                    row("2025-07-01", "2026-03-31", "241832000000", "10-Q", "2026-04-29", "0000950170-26-000003"),
                    row("2025-07-01", "2026-06-30", "331839000000", "10-K", "2026-07-29", "0000950170-26-000004"),
                    // proxy statements repeat figures too; only 10-Q / 10-K count
                    row("2025-07-01", "2026-06-30", "1", "DEF 14A", "2026-10-21", "0001193125-26-000005")),
            String.join(",",
                    row("2025-10-01", "2025-12-31", "38458000000", "10-Q", "2026-01-28", "0000950170-26-000002"),
                    row("2025-07-01", "2026-03-31", "97983000000", "10-Q", "2026-04-29", "0000950170-26-000003"),
                    row("2025-07-01", "2026-06-30", "133749000000", "10-K", "2026-07-29", "0000950170-26-000004")),
            String.join(",",
                    row("2025-10-01", "2025-12-31", "5.16", "10-Q", "2026-01-28", "0000950170-26-000002"),
                    row("2025-07-01", "2026-06-30", "17.95", "10-K", "2026-07-29", "0000950170-26-000004")));

    private static Map<FiscalPeriod, QuarterFigures> parse(String json, int fiscalYearEndMonth) {
        return KeyFiguresParser.parse(JSON.readTree(json), fiscalYearEndMonth);
    }

    @Test
    void mapsQuartersToTheCompanysFiscalYearNotTheFilingsFyField() {
        Map<FiscalPeriod, QuarterFigures> figures = parse(MSFT, 6);

        QuarterFigures q2 = figures.get(new FiscalPeriod(2026, 2));
        assertThat(q2.revenue()).isEqualByComparingTo("81273000000");
        assertThat(q2.netIncome()).isEqualByComparingTo("38458000000");
        assertThat(q2.epsDiluted()).isEqualByComparingTo("5.16");
        assertThat(q2.derived()).isFalse();
        assertThat(q2.periodEnd()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(q2.form()).isEqualTo("10-Q");
        assertThat(q2.accessionNumber()).isEqualTo("0000950170-26-000002");

        assertThat(figures.keySet()).containsExactly(new FiscalPeriod(2026, 1), new FiscalPeriod(2026, 2),
                new FiscalPeriod(2026, 3), new FiscalPeriod(2026, 4));
    }

    @Test
    void derivesQ4AsFullYearMinusNineMonthsWithoutEps() {
        QuarterFigures q4 = parse(MSFT, 6).get(new FiscalPeriod(2026, 4));

        assertThat(q4.derived()).isTrue();
        assertThat(q4.revenue()).isEqualByComparingTo("90007000000");    // 331,839 - 241,832 million
        assertThat(q4.netIncome()).isEqualByComparingTo("35766000000");  // 133,749 - 97,983 million
        assertThat(q4.epsDiluted()).isNull();                            // EPS cannot be subtracted
        assertThat(q4.form()).isEqualTo("10-K");
        assertThat(q4.periodEnd()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void aRepeatedPeriodKeepsTheMostRecentFiling() {
        QuarterFigures q1 = parse(MSFT, 6).get(new FiscalPeriod(2026, 1));

        assertThat(q1.revenue()).isEqualByComparingTo("77673000000");
        assertThat(q1.filed()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(q1.netIncome()).isNull(); // not in this fixture: shown as missing, never as 0
    }

    @Test
    void fallsBackToTheRevenuesConceptAndUsesTheCalendarForADecemberYearEnd() {
        String googl = facts("Revenues",
                String.join(",",
                        row("2025-07-01", "2025-09-30", "102346000000", "10-Q", "2025-10-30", "a-1"),
                        row("2025-01-01", "2025-09-30", "289007000000", "10-Q", "2025-10-30", "a-1"),
                        row("2025-01-01", "2025-12-31", "402836000000", "10-K", "2026-02-05", "a-2")),
                "", "");

        Map<FiscalPeriod, QuarterFigures> figures = parse(googl, 12);

        assertThat(figures.get(new FiscalPeriod(2025, 3)).revenue()).isEqualByComparingTo("102346000000");
        assertThat(figures.get(new FiscalPeriod(2025, 4)).revenue()).isEqualByComparingTo("113829000000");
        assertThat(figures.get(new FiscalPeriod(2025, 4)).derived()).isTrue();
    }

    @Test
    void fiscalPeriodFromEndDate() {
        // Microsoft, June year end
        assertThat(KeyFiguresParser.fiscalPeriod(LocalDate.of(2025, 9, 30), 6)).isEqualTo(new FiscalPeriod(2026, 1));
        assertThat(KeyFiguresParser.fiscalPeriod(LocalDate.of(2026, 3, 31), 6)).isEqualTo(new FiscalPeriod(2026, 3));
        assertThat(KeyFiguresParser.fiscalPeriod(LocalDate.of(2026, 6, 30), 6)).isEqualTo(new FiscalPeriod(2026, 4));
        // calendar year
        assertThat(KeyFiguresParser.fiscalPeriod(LocalDate.of(2026, 3, 31), 12)).isEqualTo(new FiscalPeriod(2026, 1));
        assertThat(KeyFiguresParser.fiscalPeriod(LocalDate.of(2025, 12, 31), 12)).isEqualTo(new FiscalPeriod(2025, 4));
        // 52/53-week years can end a few days into the next month
        assertThat(KeyFiguresParser.fiscalPeriod(LocalDate.of(2026, 1, 3), 12)).isEqualTo(new FiscalPeriod(2025, 4));
    }

    @Test
    void skipsMalformedRowsAndMissingConcepts() {
        String broken = """
                {"facts":{"us-gaap":{"Revenues":{"units":{"USD":[
                  {"start":"not-a-date","end":"2025-09-30","val":1,"form":"10-Q","filed":"2025-10-30"},
                  {"end":"2025-09-30","val":2,"form":"10-Q","filed":"2025-10-30"}
                ]}}}}}""";

        assertThat(parse(broken, 12)).isEmpty();
        assertThat(parse("{}", 12)).isEmpty();
    }
}
