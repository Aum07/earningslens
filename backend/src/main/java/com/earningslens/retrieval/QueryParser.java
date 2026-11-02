package com.earningslens.retrieval;

import com.earningslens.eval.FiscalPeriod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic question parser (roadmap E3.4, spec section 8 step 1): finds company tickers
 * (through {@link CompanyDirectory}) and period references. No LLM, no database.
 *
 * How it works: the question is lower-cased, then pattern groups run in a fixed order, most
 * specific first. Every match is blanked out of the working text, so a later, looser pattern
 * cannot match the same words again (for example "Q2 to Q4 of fiscal 2026" is read as one range,
 * not as "Q2" plus "Q4 of fiscal 2026").
 *
 * Conventions (see DECISIONS.md):
 * - "Qn YYYY", "Qn FYYYYY" and "nth quarter of (fiscal) YYYY" are the company's own fiscal labels.
 * - Month names ("December 2025") and "calendar Qn YYYY" are calendar dates; PeriodResolver
 *   converts them per company (Microsoft's fiscal year ends in June).
 * - A bare year ("2026 capex outlook", "full-year 2026") is a topic, not a period, and is ignored.
 */
public class QueryParser {

    /** Optional "the ", "its ", "Microsoft's " between "of" and the year. */
    private static final String OWNER = "(?:(?:the|its|their)\\s+)?(?:[a-z.&]+'s\\s+)?";
    /** One capture group: "fiscal 2026", "fiscal year 2026", "fy2026", "fy26", "fy '26" or "2026". */
    private static final String YEAR = "((?:fiscal(?:\\s+year)?|fy)\\s*'?(?:20\\d{2}|\\d{2})(?!\\d)|20\\d{2}(?!\\d))";
    /** Between a quarter and its year: "of Microsoft's ", "in ", or just a space, comma or hyphen. */
    private static final String Q_LINK = "(?:\\s*(?:of|in)\\s+" + OWNER + "|[\\s,-]*)";
    private static final String ORD = "(first|second|third|fourth|1st|2nd|3rd|4th)";
    private static final String ORD_LINK = "(?:\\s+(?:of|in)\\s+" + OWNER + "|\\s*,?\\s*)";
    private static final String COUNT = "(\\d{1,2}|two|three|four|five|six|seven|eight)";
    private static final String MONTH = "(january|february|march|april|may|june|july|august|september|october|november|december"
            + "|jan|feb|mar|apr|jun|jul|aug|sept|sep|oct|nov|dec)";

    // Year-level phrases.
    private static final Pattern FIRST_N_OF_YEAR = Pattern.compile(
            "\\bfirst\\s+(two|three|four|2|3|4)\\s+(?:fiscal\\s+)?quarters\\s+(?:of|in)\\s+" + OWNER + YEAR);
    private static final Pattern LAST_N_OF_YEAR = Pattern.compile(
            "\\blast\\s+(two|three|four|2|3|4)\\s+(?:fiscal\\s+)?quarters\\s+(?:of|in)\\s+" + OWNER + YEAR);
    private static final Pattern EACH_QUARTER_OF_YEAR = Pattern.compile(
            "\\b(?:each|every|all)\\s+(?:of\\s+(?:the|its|their)\\s+)?(?:(?:four|4)\\s+)?(?:fiscal\\s+)?quarters?\\s+(?:of|in)\\s+"
                    + OWNER + YEAR);

    // Calendar quarters.
    private static final Pattern CALENDAR_Q_YEAR = Pattern.compile("\\bcalendar(?:\\s+year)?\\s+q([1-4])[\\s,-]*(20\\d{2})\\b");
    private static final Pattern CALENDAR_YEAR_Q = Pattern.compile("\\bcalendar(?:\\s+year)?\\s+(20\\d{2})[\\s,-]*q([1-4])\\b");
    private static final Pattern CQ = Pattern.compile("\\bcq([1-4])[\\s,-]*(20\\d{2})\\b");
    private static final Pattern Q_OF_CALENDAR = Pattern.compile("\\bq([1-4])\\s+(?:of\\s+)?calendar(?:\\s+year)?\\s+(20\\d{2})\\b");

    // Half years: "first half of 2026", "H1 2026", "second half of fiscal 2026", "H2 FY26".
    private static final Pattern FIRST_HALF = Pattern.compile(
            "\\b(?:(?:first|1st)\\s+half\\s+(?:of\\s+)?" + OWNER + "|h1[\\s,-]*)" + YEAR);
    private static final Pattern SECOND_HALF = Pattern.compile(
            "\\b(?:(?:second|2nd)\\s+half\\s+(?:of\\s+)?" + OWNER + "|h2[\\s,-]*)" + YEAR);

    // Two quarters sharing one year.
    private static final Pattern Q_RANGE_YEAR = Pattern.compile(
            "\\bq([1-4])\\s*(?:to|through|thru|until|-)\\s*q([1-4])" + Q_LINK + YEAR);
    private static final Pattern BETWEEN_Q_AND_Q_YEAR = Pattern.compile(
            "\\bbetween\\s+q([1-4])\\s+and\\s+q([1-4])" + Q_LINK + YEAR);
    private static final Pattern Q_AND_Q_YEAR = Pattern.compile(
            "\\bq([1-4])\\s*(?:,|and|&|or|vs\\.?|versus)\\s*q([1-4])" + Q_LINK + YEAR);

    // One explicit fiscal quarter.
    private static final Pattern Q_YEAR = Pattern.compile("\\bq([1-4])" + Q_LINK + YEAR);
    private static final Pattern YEAR_Q = Pattern.compile("\\b" + YEAR + "[\\s,-]*q([1-4])\\b");
    /** Analyst shorthand: "2Q26", "3Q 2026". */
    private static final Pattern NQ_YEAR = Pattern.compile("\\b([1-4])q[\\s,-]*'?(20\\d{2}|\\d{2})(?!\\d)");
    private static final Pattern ORD_QUARTER_YEAR = Pattern.compile("\\b" + ORD + "\\s+(?:fiscal\\s+)?quarter" + ORD_LINK + YEAR);
    private static final Pattern YEAR_ORD_QUARTER = Pattern.compile("\\b" + YEAR + "\\s+" + ORD + "\\s+(?:fiscal\\s+)?quarter\\b");

    // Calendar month with a year: "December 2025", "September 30, 2026", "May 2026".
    private static final Pattern MONTH_YEAR = Pattern.compile(
            "\\b" + MONTH + "\\.?\\s+(?:\\d{1,2}(?:st|nd|rd|th)?,?\\s+)?(20\\d{2})\\b");

    // Relative to the latest quarter in the corpus.
    private static final Pattern LAST_N = Pattern.compile(
            "\\b(?:last|past|previous|prior|recent|trailing|latest|most\\s+recent)\\s+" + COUNT
                    + "\\s+(?:reported\\s+)?(?:fiscal\\s+)?(?:quarters|earnings\\s+calls|calls|earnings\\s+reports|reports"
                    + "|earnings\\s+releases|releases)\\b");
    private static final Pattern LATEST = Pattern.compile(
            "\\b(?:latest|most\\s+recent|last|past|recent)\\s+(?:reported\\s+)?(?:fiscal\\s+)?"
                    + "(?:quarter|earnings\\s+call|earnings\\s+report|earnings\\s+release|earnings|results|call)\\b");

    // A quarter with no year.
    private static final Pattern Q_ONLY = Pattern.compile("\\bq([1-4])\\b");
    private static final Pattern ORD_QUARTER_ONLY = Pattern.compile("\\b" + ORD + "\\s+(?:fiscal\\s+)?quarter\\b");

    // Modifiers.
    private static final Pattern PREVIOUS_QUARTER = Pattern.compile(
            "\\b(?:previous|prior|preceding)\\s+quarter\\b|\\bquarter[-\\s]over[-\\s]quarter\\b|\\bqoq\\b|\\bq/q\\b|\\bsequential(?:ly)?\\b");
    private static final Pattern YEAR_AGO = Pattern.compile(
            "\\byear[-\\s]over[-\\s]year\\b|\\byoy\\b|\\by/y\\b|\\ba\\s+year\\s+(?:ago|earlier)\\b|\\byear-ago\\b"
                    + "|\\bprior[-\\s]year\\s+quarter\\b|\\bsame\\s+quarter\\s+(?:of\\s+)?(?:last|the\\s+previous|the\\s+prior|a)\\s+year\\b");

    /** Text between two explicit quarters that makes them a range: " to ", " call to the ", " - ". */
    private static final Pattern RANGE_GAP = Pattern.compile(
            "\\s*(?:(?:earnings\\s+)?(?:call|calls|results|report|release|quarter))?\\s*(?:to|through|thru|until|till|-)\\s*"
                    + "(?:(?:the|its|their)\\s+)?");
    private static final Pattern BETWEEN_GAP = Pattern.compile(
            "\\s*(?:(?:earnings\\s+)?(?:call|results|report|release|quarter))?\\s*and\\s*(?:(?:the|its|their)\\s+)?");
    private static final Pattern ENDS_WITH_BETWEEN = Pattern.compile("(?s).*\\bbetween\\s+(?:the\\s+)?$");
    /** "since Q4 2025", "starting with Q4 2025", "from Q4 2025 onwards": from that quarter to the latest. */
    private static final Pattern ENDS_WITH_SINCE = Pattern.compile(
            "(?s).*\\b(?:since|starting\\s+(?:in|with|from)|beginning\\s+(?:in|with))\\s+(?:the\\s+)?$");
    private static final Pattern ENDS_WITH_FROM = Pattern.compile("(?s).*\\bfrom\\s+(?:the\\s+)?$");
    private static final Pattern STARTS_WITH_ONWARDS = Pattern.compile(
            "^\\s*(?:(?:earnings\\s+)?(?:call|results|report|quarter)\\s+)?(?:onwards?|on)\\b.*", Pattern.DOTALL);

    private static final Map<String, Integer> NUMBER_WORDS = Map.of(
            "two", 2, "three", 3, "four", 4, "five", 5, "six", 6, "seven", 7, "eight", 8);
    private static final Map<String, Integer> ORDINALS = Map.of(
            "first", 1, "second", 2, "third", 3, "fourth", 4, "1st", 1, "2nd", 2, "3rd", 3, "4th", 4);
    private static final List<String> MONTHS = List.of(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");

    /** Longest range we expand, so a typo such as "Q1 2016 to Q1 2026" cannot create 40 quarters. */
    private static final int MAX_RANGE_QUARTERS = 12;

    private final CompanyDirectory companies;

    public QueryParser(CompanyDirectory companies) {
        this.companies = companies;
    }

    public ParsedQuestion parse(String question) {
        if (question == null) {
            throw new IllegalArgumentException("question is null");
        }
        // Same length as the input (1:1 replacements), so match positions stay valid.
        String normalized = question.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u2013', '-').replace('\u2014', '-');
        StringBuilder text = new StringBuilder(normalized);
        List<Hit> hits = new ArrayList<>();

        scan(text, hits, FIRST_N_OF_YEAR, m -> quartersOfYear(year(m.group(2)), 1, count(m.group(1))));
        scan(text, hits, LAST_N_OF_YEAR, m -> quartersOfYear(year(m.group(2)), 5 - count(m.group(1)), 4));
        scan(text, hits, EACH_QUARTER_OF_YEAR, m -> List.of(new PeriodRef.WholeYear(year(m.group(1)))));

        scan(text, hits, FIRST_HALF, m -> quartersOfYear(year(m.group(1)), 1, 2));
        scan(text, hits, SECOND_HALF, m -> quartersOfYear(year(m.group(1)), 3, 4));

        scan(text, hits, CALENDAR_Q_YEAR, m -> calendarQuarter(m.group(2), m.group(1)));
        scan(text, hits, CALENDAR_YEAR_Q, m -> calendarQuarter(m.group(1), m.group(2)));
        scan(text, hits, CQ, m -> calendarQuarter(m.group(2), m.group(1)));
        scan(text, hits, Q_OF_CALENDAR, m -> calendarQuarter(m.group(2), m.group(1)));

        scan(text, hits, Q_RANGE_YEAR, m -> range(year(m.group(3)), q(m.group(1)), q(m.group(2))));
        scan(text, hits, BETWEEN_Q_AND_Q_YEAR, m -> range(year(m.group(3)), q(m.group(1)), q(m.group(2))));
        scan(text, hits, Q_AND_Q_YEAR, m -> List.of(
                fiscal(year(m.group(3)), q(m.group(1))), fiscal(year(m.group(3)), q(m.group(2)))));

        scan(text, hits, Q_YEAR, m -> List.of(fiscal(year(m.group(2)), q(m.group(1)))));
        scan(text, hits, YEAR_Q, m -> List.of(fiscal(year(m.group(1)), q(m.group(2)))));
        scan(text, hits, NQ_YEAR, m -> List.of(fiscal(year(m.group(2)), q(m.group(1)))));
        scan(text, hits, ORD_QUARTER_YEAR, m -> List.of(fiscal(year(m.group(2)), ORDINALS.get(m.group(1)))));
        scan(text, hits, YEAR_ORD_QUARTER, m -> List.of(fiscal(year(m.group(1)), ORDINALS.get(m.group(2)))));

        scan(text, hits, MONTH_YEAR, m -> List.of(new PeriodRef.CalendarMonth(
                Integer.parseInt(m.group(2)), MONTHS.indexOf(m.group(1).substring(0, 3)) + 1)));

        scan(text, hits, LAST_N, m -> List.of(new PeriodRef.Latest(count(m.group(1)))));
        scan(text, hits, LATEST, m -> List.of(new PeriodRef.Latest(1)));

        scan(text, hits, Q_ONLY, m -> List.of(new PeriodRef.QuarterOnly(q(m.group(1)))));
        scan(text, hits, ORD_QUARTER_ONLY, m -> List.of(new PeriodRef.QuarterOnly(ORDINALS.get(m.group(1)))));

        scan(text, hits, PREVIOUS_QUARTER, m -> List.of(new PeriodRef.PreviousQuarter()));
        scan(text, hits, YEAR_AGO, m -> List.of(new PeriodRef.YearAgo()));

        hits.sort(Comparator.comparingInt(Hit::start));
        List<PeriodRef> refs = new ArrayList<>();
        for (Hit hit : mergeRanges(hits, normalized)) {
            for (PeriodRef ref : hit.refs()) {
                if (!refs.contains(ref)) {
                    refs.add(ref);
                }
            }
        }
        return new ParsedQuestion(companies.findTickers(question), refs);
    }

    /** Joins "Q4 2025 ... to ... Q3 2026" and "between Q1 2026 and Q3 2026" into every quarter in between. */
    private static List<Hit> mergeRanges(List<Hit> hits, String normalized) {
        List<Hit> merged = new ArrayList<>();
        int i = 0;
        while (i < hits.size()) {
            Hit a = hits.get(i);
            if (i + 1 < hits.size() && a.singleFiscal() != null && hits.get(i + 1).singleFiscal() != null) {
                Hit b = hits.get(i + 1);
                String gap = normalized.substring(a.end(), b.start());
                boolean isRange = RANGE_GAP.matcher(gap).matches()
                        || (BETWEEN_GAP.matcher(gap).matches()
                            && ENDS_WITH_BETWEEN.matcher(normalized.substring(0, a.start())).matches());
                if (isRange) {
                    FiscalPeriod from = a.singleFiscal();
                    FiscalPeriod to = b.singleFiscal();
                    merged.add(new Hit(a.start(), b.end(), expand(from, to)));
                    i += 2;
                    continue;
                }
            }
            FiscalPeriod single = a.singleFiscal();
            if (single != null && isSince(normalized, a)) {
                merged.add(new Hit(a.start(), a.end(), List.of(new PeriodRef.Since(single))));
            } else {
                merged.add(a);
            }
            i++;
        }
        return merged;
    }

    private static boolean isSince(String normalized, Hit hit) {
        String before = normalized.substring(0, hit.start());
        String after = normalized.substring(hit.end());
        return ENDS_WITH_SINCE.matcher(before).matches()
                || (ENDS_WITH_FROM.matcher(before).matches() && STARTS_WITH_ONWARDS.matcher(after).matches());
    }

    private static void scan(StringBuilder text, List<Hit> hits, Pattern pattern, Function<Matcher, List<PeriodRef>> toRefs) {
        Matcher m = pattern.matcher(text);
        List<int[]> spans = new ArrayList<>();
        while (m.find()) {
            hits.add(new Hit(m.start(), m.end(), toRefs.apply(m)));
            spans.add(new int[] {m.start(), m.end()});
        }
        for (int[] span : spans) {
            for (int i = span[0]; i < span[1]; i++) {
                text.setCharAt(i, ' ');
            }
        }
    }

    private static List<PeriodRef> quartersOfYear(int year, int fromQuarter, int toQuarter) {
        List<PeriodRef> refs = new ArrayList<>();
        for (int quarter = fromQuarter; quarter <= toQuarter; quarter++) {
            refs.add(fiscal(year, quarter));
        }
        return refs;
    }

    private static List<PeriodRef> range(int year, int fromQuarter, int toQuarter) {
        return quartersOfYear(year, Math.min(fromQuarter, toQuarter), Math.max(fromQuarter, toQuarter));
    }

    private static List<PeriodRef> expand(FiscalPeriod from, FiscalPeriod to) {
        if (from.compareTo(to) > 0) {
            FiscalPeriod swap = from;
            from = to;
            to = swap;
        }
        List<PeriodRef> refs = new ArrayList<>();
        for (FiscalPeriod p = from; p.compareTo(to) <= 0 && refs.size() < MAX_RANGE_QUARTERS; p = p.next()) {
            refs.add(new PeriodRef.Fiscal(p));
        }
        return refs;
    }

    private static List<PeriodRef> calendarQuarter(String year, String quarter) {
        return List.of(new PeriodRef.CalendarQuarter(Integer.parseInt(year), q(quarter)));
    }

    private static PeriodRef fiscal(int year, int quarter) {
        return new PeriodRef.Fiscal(new FiscalPeriod(year, quarter));
    }

    private static int q(String digit) {
        return Integer.parseInt(digit);
    }

    private static int count(String text) {
        Integer word = NUMBER_WORDS.get(text);
        return word != null ? word : Integer.parseInt(text);
    }

    /** "fiscal 2026", "fy26", "fy '26", "2026" -> 2026. */
    static int year(String text) {
        String digits = text.replaceAll("\\D", "");
        return digits.length() == 2 ? 2000 + Integer.parseInt(digits) : Integer.parseInt(digits);
    }

    private record Hit(int start, int end, List<PeriodRef> refs) {

        /** The period if this hit is exactly one explicit fiscal quarter, else null. */
        FiscalPeriod singleFiscal() {
            return refs.size() == 1 && refs.get(0) instanceof PeriodRef.Fiscal f ? f.period() : null;
        }
    }
}
