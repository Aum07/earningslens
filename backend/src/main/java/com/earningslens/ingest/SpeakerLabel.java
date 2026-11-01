package com.earningslens.ingest;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises who is speaking at the start of a transcript paragraph.
 *
 * Formats found in the corpus (checked on all 8 transcripts):
 * - Alphabet: "Sundar Pichai, CEO, Alphabet and Google: Thanks, Jim."   (name, role, colon, speech)
 * - Microsoft: "AMY HOOD:" and "ADAM WOOD, Morgan Stanley:"            (caps name, optional role)
 * - Meta: "Susan Li, CFO" on its own line, later "Susan Li: I'm happy..."
 * - Everyone: "Operator: ..."
 *
 * The name must be 2 or 3 capitalised words ("Operator" is the only one-word name), which
 * rejects false alarms such as "Second: ..." or "Today, I will focus...: ...".
 *
 * @param speaker the label to store, for example "Susan Li, CFO"
 * @param rest    the speech after the label on the same paragraph (may be empty)
 */
public record SpeakerLabel(String speaker, String rest) {

    private static final String WORD = "[A-Z][\\p{L}.'’\\-]*";
    private static final String NAME = WORD + "(?: " + WORD + "){1,2}";
    private static final String ROLE = "(?=[^:()]*\\p{L})[^:()]{1,60}";

    private static final Pattern WITH_COLON = Pattern.compile("(" + NAME + ")(, " + ROLE + ")?:\\s*(.*)", Pattern.DOTALL);
    private static final Pattern OPERATOR = Pattern.compile("(?i)operator(?::\\s*(.*))?", Pattern.DOTALL);
    private static final Pattern LABEL_LINE = Pattern.compile("(" + NAME + ")(, " + ROLE + ")");

    /**
     * @param knownSpeakers name to full label seen so far in this document, for example
     *                      "Susan Li" to "Susan Li, CFO". Updated when a label with a role is found,
     *                      so a later short label ("Susan Li:") is stored with the full role.
     */
    public static Optional<SpeakerLabel> parse(String text, Map<String, String> knownSpeakers) {
        String trimmed = text.strip();

        Matcher operator = OPERATOR.matcher(trimmed);
        if (operator.matches()) {
            return Optional.of(new SpeakerLabel("Operator", operator.group(1) == null ? "" : operator.group(1).strip()));
        }

        Matcher colon = WITH_COLON.matcher(trimmed);
        if (colon.matches()) {
            return Optional.of(new SpeakerLabel(fullLabel(colon.group(1), colon.group(2), knownSpeakers),
                    colon.group(3).strip()));
        }

        Matcher line = LABEL_LINE.matcher(trimmed);
        if (line.matches() && !trimmed.matches(".*[.!?,;]")) {
            return Optional.of(new SpeakerLabel(fullLabel(line.group(1), line.group(2), knownSpeakers), ""));
        }
        return Optional.empty();
    }

    private static String fullLabel(String name, String role, Map<String, String> knownSpeakers) {
        if (role != null) {
            String full = (name + role).strip();
            knownSpeakers.put(name, full);
            return full;
        }
        return knownSpeakers.getOrDefault(name, name);
    }
}
