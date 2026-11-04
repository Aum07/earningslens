package com.earningslens.eval;

import java.util.List;

/**
 * The judge's scores for one answer (spec 11.2), after JudgeClient validated them.
 *
 * @param claims            every atomic claim of the answer and whether the cited sources support it
 * @param citations         one entry per (sentence, marker) pair, in the order they were sent
 * @param correctness       1 to 5 against the gold answer (eval/judge-rubric.md)
 * @param correctnessReason the judge's reason for that score
 * @param model             the judge model as reported by the API
 * @param promptTokens      input tokens over all attempts, or null if the API did not report them
 * @param completionTokens  output tokens over all attempts, or null if the API did not report them
 * @param attempts          judge calls made (1 unless the first output failed validation)
 */
public record JudgeVerdict(List<Claim> claims, List<CitationSupport> citations, int correctness,
                           String correctnessReason, String model, Integer promptTokens, Integer completionTokens,
                           int attempts) {

    /** One atomic claim. sourceMarkers are the sources the judge says state it (empty when unsupported). */
    public record Claim(String claim, List<Integer> sourceMarkers, String reason, boolean supported) {

        public Claim {
            sourceMarkers = List.copyOf(sourceMarkers);
        }
    }

    /** Does source [marker] support sentence S{sentenceId}? */
    public record CitationSupport(int sentenceId, int marker, String reason, boolean supports) {

        public Pair pair() {
            return new Pair(sentenceId, marker);
        }
    }

    /** A sentence id (1 = S1) and one of its markers. */
    public record Pair(int sentenceId, int marker) {
    }

    public JudgeVerdict {
        claims = List.copyOf(claims);
        citations = List.copyOf(citations);
    }

    public long supportedClaims() {
        return claims.stream().filter(Claim::supported).count();
    }

    /** Faithfulness = supported claims / all claims (spec 11.2). The parser guarantees at least one claim. */
    public double faithfulness() {
        return claims.isEmpty() ? 0.0 : (double) supportedClaims() / claims.size();
    }

    public long supportingCitations() {
        return citations.stream().filter(CitationSupport::supports).count();
    }

    /** Citation precision = supporting pairs / all pairs, or null when the answer cites nothing. */
    public Double citationPrecision() {
        return citations.isEmpty() ? null : (double) supportingCitations() / citations.size();
    }
}
