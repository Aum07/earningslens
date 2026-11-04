package com.earningslens.generation;

import com.earningslens.generation.CitationCheck.CitedSentence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the [n] citation markers in a model answer (spec 9.2, roadmap E4.3). The model is never trusted.
 *
 * 1. Markers: "[2]", "[1][3]" and "[1, 3]" are all read. A marker pointing to a source that does not
 *    exist (0, or above the number of sources) is removed with a warning. Each group is rewritten as
 *    "[1][3]", with repeats inside one group dropped, so the frontend only has to handle one form.
 * 2. Sentences: the answer is split into sentences (per line, so bullet points and "FY2026 Q1" sections
 *    count separately). A marker written just after the full stop ("... billion. [2]") belongs to the
 *    sentence before it. Headings (ending in ":", starting with "#", or only bold text) and fragments
 *    under 3 words are not counted. If any counted sentence has no marker, one warning gives the count.
 */
@Component
public class CitationValidator {

    /** Up to 3 digits, so "[2026]" is not read as a marker. */
    private static final String ONE_GROUP = "\\[\\s*\\d{1,3}(?:\\s*,\\s*\\d{1,3})*\\s*]";

    /** Optional spaces before, then one or more groups next to each other ("[1][3]", "[1] [3]"). */
    private static final Pattern MARKER_RUN =
            Pattern.compile("([ \\t]*)(" + ONE_GROUP + "(?:[ \\t]*" + ONE_GROUP + ")*)");

    private static final Pattern NUMBER = Pattern.compile("\\d{1,3}");

    /** A run of markers at the very start of a piece (moved back to the previous sentence). */
    private static final Pattern LEADING_MARKERS = Pattern.compile("^(?:[ \\t]*" + ONE_GROUP + ")+");

    /** A possible sentence end: . ! or ? followed by whitespace. */
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]+[\"')\\]]*\\s+");

    /** Words that end with a full stop but do not end a sentence. Lower case, without the final full stop. */
    private static final Set<String> ABBREVIATIONS = Set.of(
            "mr", "ms", "mrs", "dr", "inc", "corp", "co", "ltd", "vs", "e.g", "i.e", "u.s", "u.k", "approx",
            "no", "st", "jr", "etc", "fig", "est");

    private static final int MIN_WORDS = 3;

    public CitationCheck validate(String answer, int sourceCount) {
        if (sourceCount < 0) {
            throw new IllegalArgumentException("sourceCount must not be negative");
        }
        List<String> warnings = new ArrayList<>();
        String text = cleanMarkers(answer == null ? "" : answer, sourceCount, warnings);

        List<CitedSentence> sentences = new ArrayList<>();
        Set<Integer> cited = new LinkedHashSet<>();
        for (String sentence : sentences(text)) {
            List<Integer> markers = markersIn(sentence);
            cited.addAll(markers);
            if (counts(sentence)) {
                sentences.add(new CitedSentence(sentence, markers));
            }
        }

        long uncited = sentences.stream().filter(s -> !s.cited()).count();
        if (uncited > 0) {
            warnings.add("%d of %d sentences have no citation.".formatted(uncited, sentences.size()));
        }
        return new CitationCheck(text, List.copyOf(cited), sentences, warnings);
    }

    // ---------- step 1: markers ----------

    private static String cleanMarkers(String answer, int sourceCount, List<String> warnings) {
        Matcher run = MARKER_RUN.matcher(answer);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (run.find()) {
            out.append(answer, last, run.start());
            last = run.end();

            Set<Integer> keep = new LinkedHashSet<>();
            Matcher number = NUMBER.matcher(run.group(2));
            while (number.find()) {
                int marker = Integer.parseInt(number.group());
                if (marker >= 1 && marker <= sourceCount) {
                    keep.add(marker);
                } else {
                    warnings.add("Removed citation [%d]: there %s only %d source%s."
                            .formatted(marker, sourceCount == 1 ? "is" : "are", sourceCount, sourceCount == 1 ? "" : "s"));
                }
            }

            if (keep.isEmpty()) {
                // Drop the whole marker. If it had no space before it, drop one space after it instead.
                if (run.group(1).isEmpty() && last < answer.length() && answer.charAt(last) == ' ') {
                    last++;
                }
            } else {
                out.append(run.group(1));
                keep.forEach(marker -> out.append('[').append(marker).append(']'));
            }
        }
        out.append(answer.substring(last));
        return out.toString().strip();
    }

    private static List<Integer> markersIn(String sentence) {
        Set<Integer> markers = new LinkedHashSet<>();
        Matcher run = MARKER_RUN.matcher(sentence);
        while (run.find()) {
            Matcher number = NUMBER.matcher(run.group(2));
            while (number.find()) {
                markers.add(Integer.parseInt(number.group()));
            }
        }
        return List.copyOf(markers);
    }

    // ---------- step 2: sentences ----------

    static List<String> sentences(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            List<String> pieces = splitLine(line.strip());
            for (String piece : pieces) {
                Matcher leading = LEADING_MARKERS.matcher(piece);
                if (leading.find() && !result.isEmpty()) {
                    // "... billion. [2] Next sentence": [2] belongs to the sentence before.
                    int prev = result.size() - 1;
                    result.set(prev, result.get(prev) + " " + leading.group().strip());
                    piece = piece.substring(leading.end()).strip();
                }
                if (!piece.isEmpty()) {
                    result.add(piece);
                }
            }
        }
        return result;
    }

    private static List<String> splitLine(String line) {
        List<String> pieces = new ArrayList<>();
        Matcher end = SENTENCE_END.matcher(line);
        int start = 0;
        while (end.find()) {
            int after = end.end();
            if (after >= line.length() || isAbbreviation(line, end.start()) || !startsSentence(line.charAt(after))) {
                continue;
            }
            pieces.add(line.substring(start, after).strip());
            start = after;
        }
        if (start < line.length()) {
            pieces.add(line.substring(start).strip());
        }
        return pieces;
    }

    /** A new sentence starts with a capital, a digit, a quote, a bracket (marker or aside), "$" or markdown. */
    private static boolean startsSentence(char c) {
        return Character.isUpperCase(c) || Character.isDigit(c) || "\"'([$*_".indexOf(c) >= 0;
    }

    private static boolean isAbbreviation(String line, int dotIndex) {
        int wordStart = dotIndex;
        while (wordStart > 0 && !Character.isWhitespace(line.charAt(wordStart - 1))
                && line.charAt(wordStart - 1) != '(') {
            wordStart--;
        }
        String word = line.substring(wordStart, dotIndex).toLowerCase();
        return ABBREVIATIONS.contains(word);
    }

    /** Headings and tiny fragments do not need a citation. */
    private static boolean counts(String sentence) {
        String plain = MARKER_RUN.matcher(sentence).replaceAll("").strip();
        if (plain.isEmpty() || plain.startsWith("#") || plain.endsWith(":")) {
            return false;
        }
        String withoutBullet = plain.replaceFirst("^(?:[-*+]|\\d+[.)])\\s+", "");
        if (withoutBullet.matches("\\*\\*[^*]+\\*\\*:?") || withoutBullet.matches("__[^_]+__:?")) {
            return false;   // a line that is only bold text, like "**FY2026 Q1**"
        }
        return withoutBullet.split("\\s+").length >= MIN_WORDS;
    }
}
