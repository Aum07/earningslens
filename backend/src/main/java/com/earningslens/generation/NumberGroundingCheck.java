package com.earningslens.generation;

import com.earningslens.generation.GroundingCheck.NumberMention;
import com.earningslens.retrieval.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that every number in an answer appears in at least one CITED source (spec 9.2, roadmap E4.4).
 * A warning, not a block: unmatched numbers go into groundingWarnings and the eval reports the rate.
 *
 * Reading numbers: "1,234", "12.5%", "12.5 percent", "$3.2 billion", "3.2bn", "$3.2B", "50 basis points".
 * Every number becomes a value plus a kind (amount, percent or basis points), so "$3.2 billion" and
 * "3,200 million" are the same amount.
 *
 * Matching allows for rounding: "$70.1 billion" matches 70,066 (in millions) because 70.066 rounds to 70.1.
 * A source number without a unit (usually a table cell under "in millions") is tried as is, as thousands
 * and as millions. A percent only matches a percent, so "29%" is not grounded by a plain "29".
 *
 * Not checked in the answer: citation markers, years (1990 to 2100 written alone), numbers inside words
 * (FY2026, Q1, gpt-4o) and plain whole numbers under 10 (list numbering, "3 quarters").
 */
@Component
public class NumberGroundingCheck {

    enum Kind { AMOUNT, PERCENT, BASIS_POINTS }

    /** A number as read from text. value is always positive; tolerance is half a unit of its last digit. */
    record ParsedNumber(String text, double value, Kind kind, boolean scaled, boolean plain, double tolerance) {
    }

    private static final String UNIT_WORDS = "percentage points?|percent|basis points?|bps|pp"
            + "|trillion|billion|million|thousand|tn|bn|mn";

    /**
     * Optional "$", the digits (with thousands commas and/or decimals), then an optional unit. A unit word may
     * follow a space; a one-letter unit (k, m, b, t) must be attached ("3.2B"). Digits inside words are skipped.
     */
    private static final Pattern NUMBER = Pattern.compile(
            "(?<![\\w.,$])(\\$\\s?)?(\\d{1,3}(?:,\\d{3})+|\\d+)(\\.\\d+)?"
                    + "(?:(%)|\\s?(" + UNIT_WORDS + ")\\b|([kmbt])\\b)?(?![\\w%])",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MARKERS = Pattern.compile("\\[\\s*\\d{1,3}(?:\\s*,\\s*\\d{1,3})*\\s*]");

    private static final double[] TABLE_SCALES = {1, 1e3, 1e6};

    /** Checks the numbers in the validated answer against the sources its valid markers point to. */
    public GroundingCheck check(CitationCheck citations, List<RetrievedChunk> sources) {
        List<RetrievedChunk> cited = new ArrayList<>();
        for (int marker : citations.citedMarkers()) {
            if (marker >= 1 && marker <= sources.size()) {
                cited.add(sources.get(marker - 1));
            }
        }
        return check(citations.text(), cited);
    }

    public GroundingCheck check(String answer, List<RetrievedChunk> citedSources) {
        List<ParsedNumber> sourceNumbers = new ArrayList<>();
        for (RetrievedChunk chunk : citedSources) {
            sourceNumbers.addAll(parse(chunk.content()));
        }

        List<NumberMention> mentions = new ArrayList<>();
        Set<String> unmatched = new LinkedHashSet<>();
        String text = MARKERS.matcher(answer == null ? "" : answer).replaceAll(" ");
        for (ParsedNumber number : parse(text)) {
            if (skipInAnswer(number)) {
                continue;
            }
            boolean grounded = sourceNumbers.stream().anyMatch(source -> matches(number, source));
            mentions.add(new NumberMention(number.text(), grounded));
            if (!grounded) {
                unmatched.add(number.text());
            }
        }

        List<String> warnings = unmatched.stream().map(n -> "Not found in the cited sources: " + n).toList();
        return new GroundingCheck(mentions, warnings);
    }

    // ---------- reading numbers ----------

    static List<ParsedNumber> parse(String text) {
        List<ParsedNumber> numbers = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            boolean dollar = m.group(1) != null;
            String whole = m.group(2).replace(",", "");
            String fraction = m.group(3) == null ? "" : m.group(3);
            String unit = m.group(4) != null ? m.group(4)
                    : m.group(5) != null ? m.group(5)
                    : m.group(6);
            unit = unit == null ? "" : unit.toLowerCase(Locale.ROOT);

            Kind kind = switch (unit) {
                case "%", "percent", "percentage point", "percentage points", "pp" -> Kind.PERCENT;
                case "basis point", "basis points", "bps" -> Kind.BASIS_POINTS;
                default -> Kind.AMOUNT;
            };
            double scale = switch (unit) {
                case "thousand", "k" -> 1e3;
                case "million", "mn", "m" -> 1e6;
                case "billion", "bn", "b" -> 1e9;
                case "trillion", "tn", "t" -> 1e12;
                default -> 1;
            };
            int decimals = fraction.isEmpty() ? 0 : fraction.length() - 1;
            double value = Double.parseDouble(whole + fraction) * scale;
            double tolerance = 0.5 * Math.pow(10, -decimals) * scale;
            boolean plain = !dollar && unit.isEmpty() && fraction.isEmpty() && !m.group(2).contains(",");
            numbers.add(new ParsedNumber(m.group().strip(), value, kind, scale != 1, plain, tolerance));
        }
        return numbers;
    }

    /** Years and small counting numbers in the answer are not figures to check. */
    private static boolean skipInAnswer(ParsedNumber number) {
        if (!number.plain()) {
            return false;
        }
        double v = number.value();
        return v < 10 || (v >= 1990 && v <= 2100);
    }

    // ---------- matching ----------

    static boolean matches(ParsedNumber answer, ParsedNumber source) {
        if (answer.kind() != source.kind()) {
            return false;
        }
        if (answer.kind() != Kind.AMOUNT || source.scaled()) {
            return close(answer, source.value());
        }
        for (double scale : TABLE_SCALES) {          // unitless source: maybe a table "in thousands/millions"
            if (close(answer, source.value() * scale)) {
                return true;
            }
        }
        return false;
    }

    /** True if the source value rounds to what the answer wrote. */
    private static boolean close(ParsedNumber answer, double sourceValue) {
        return Math.abs(answer.value() - sourceValue) <= answer.tolerance() + 1e-9 * Math.max(1, answer.value());
    }
}
