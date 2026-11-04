package com.earningslens.generation;

import java.util.List;

/**
 * Result of {@link CitationValidator#validate(String, int)}.
 *
 * @param text          the answer with invalid markers removed and groups written as "[1][3]"
 * @param citedMarkers  valid markers in order of first use, each once (the API's citations list)
 * @param sentences     every sentence that should carry a citation, with its own markers
 *                      (headings and very short fragments are left out); used by the eval judge
 * @param warnings      one per removed marker, plus one if any sentence has no marker
 */
public record CitationCheck(String text, List<Integer> citedMarkers, List<CitedSentence> sentences,
                            List<String> warnings) {

    /** One sentence of the answer and the markers attached to it (empty = uncited). */
    public record CitedSentence(String text, List<Integer> markers) {

        public CitedSentence {
            markers = List.copyOf(markers);
        }

        public boolean cited() {
            return !markers.isEmpty();
        }
    }

    public CitationCheck {
        citedMarkers = List.copyOf(citedMarkers);
        sentences = List.copyOf(sentences);
        warnings = List.copyOf(warnings);
    }

    public List<CitedSentence> uncitedSentences() {
        return sentences.stream().filter(s -> !s.cited()).toList();
    }
}
