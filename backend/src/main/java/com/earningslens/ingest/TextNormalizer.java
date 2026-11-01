package com.earningslens.ingest;

import java.util.regex.Pattern;

/** Shared text clean-up used by every parser, so HTML and PDF text look the same. */
public final class TextNormalizer {

    private static final Pattern SPACE_LIKE = Pattern.compile("[\\u00a0\\u2007\\u202f]");
    private static final Pattern INVISIBLE = Pattern.compile("[\\u00ad\\u200b\\ufeff]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    /** Unifies special spaces and hyphens, removes invisible characters, collapses whitespace. */
    public static String normalize(String text) {
        String result = SPACE_LIKE.matcher(text).replaceAll(" ");
        result = INVISIBLE.matcher(result).replaceAll("");
        result = result.replace('‐', '-').replace('‑', '-');
        return WHITESPACE.matcher(result).replaceAll(" ").trim();
    }
}
