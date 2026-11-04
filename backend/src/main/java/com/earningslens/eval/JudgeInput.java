package com.earningslens.eval;

import com.earningslens.generation.CitationCheck.CitedSentence;
import com.earningslens.retrieval.RetrievedChunk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Everything the judge needs for one answered question.
 *
 * @param question    the dataset question
 * @param goldAnswer  the dataset's reference answer
 * @param answerText  the answer after CitationValidator (invalid markers removed)
 * @param sources     all chunks sent to the answer model, in citation order: [1] is sources.get(0)
 * @param sentences   the answer's sentences with their markers (CitationCheck.sentences())
 */
public record JudgeInput(String question, String goldAnswer, String answerText, List<RetrievedChunk> sources,
                         List<CitedSentence> sentences) {

    public JudgeInput {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(goldAnswer, "goldAnswer");
        Objects.requireNonNull(answerText, "answerText");
        sources = List.copyOf(sources);
        sentences = List.copyOf(sentences);
        if (answerText.isBlank()) {
            throw new IllegalArgumentException("answerText must not be blank (abstentions are not judged)");
        }
        for (CitedSentence sentence : sentences) {
            for (int marker : sentence.markers()) {
                if (marker < 1 || marker > sources.size()) {
                    throw new IllegalArgumentException("marker [" + marker + "] has no source (there are "
                            + sources.size() + ")");
                }
            }
        }
    }

    /** Source numbers the answer cites, ascending. Only these sources are shown to the judge. */
    public List<Integer> citedMarkers() {
        TreeSet<Integer> markers = new TreeSet<>();
        sentences.forEach(s -> markers.addAll(s.markers()));
        return List.copyOf(markers);
    }

    /** Every (sentence id, marker) pair the judge must check for citation precision; sentence ids start at 1. */
    public List<JudgeVerdict.Pair> pairs() {
        List<JudgeVerdict.Pair> pairs = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            // LinkedHashSet: a marker repeated in one sentence ("[2] ... [2]") is one pair, first-use order kept.
            for (int marker : new LinkedHashSet<>(sentences.get(i).markers())) {
                pairs.add(new JudgeVerdict.Pair(i + 1, marker));
            }
        }
        return List.copyOf(pairs);
    }
}
