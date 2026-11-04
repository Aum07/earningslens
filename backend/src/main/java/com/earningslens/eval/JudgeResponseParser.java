package com.earningslens.eval;

import com.earningslens.eval.JudgeVerdict.CitationSupport;
import com.earningslens.eval.JudgeVerdict.Claim;
import com.earningslens.eval.JudgeVerdict.Pair;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and validates the judge's JSON reply (roadmap E5.1).
 *
 * The OpenAI schema (strict mode) already fixes the shape. This class checks what a schema cannot:
 * - correctness is 1 to 5 and every reason is filled in;
 * - at least one claim; a supported claim names a source; claims only name sources the answer cites;
 * - the citations list answers exactly the pairs that were sent, each once, in that order.
 * Any problem throws {@link JudgeOutputException} with a message that is sent back to the judge on retry.
 */
final class JudgeResponseParser {

    /** The JSON schema sent with every judge request. Field order = the order the model writes them. */
    static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "claims": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "claim": { "type": "string" },
                      "source_markers": { "type": "array", "items": { "type": "integer" } },
                      "reason": { "type": "string" },
                      "supported": { "type": "boolean" }
                    },
                    "required": ["claim", "source_markers", "reason", "supported"],
                    "additionalProperties": false
                  }
                },
                "citations": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "sentence_id": { "type": "integer" },
                      "marker": { "type": "integer" },
                      "reason": { "type": "string" },
                      "supports": { "type": "boolean" }
                    },
                    "required": ["sentence_id", "marker", "reason", "supports"],
                    "additionalProperties": false
                  }
                },
                "correctness": {
                  "type": "object",
                  "properties": {
                    "reason": { "type": "string" },
                    "score": { "type": "integer", "enum": [1, 2, 3, 4, 5] }
                  },
                  "required": ["reason", "score"],
                  "additionalProperties": false
                }
              },
              "required": ["claims", "citations", "correctness"],
              "additionalProperties": false
            }
            """;

    /** The validated content of one reply. */
    record Parsed(List<Claim> claims, List<CitationSupport> citations, int correctness, String correctnessReason) {
    }

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JudgeResponseParser() {
    }

    /**
     * @param citedMarkers  the source numbers shown to the judge (claims may only name these)
     * @param expectedPairs the pairs sent under PAIRS TO JUDGE, in order
     */
    static Parsed parse(String reply, Collection<Integer> citedMarkers, List<Pair> expectedPairs) {
        if (reply == null || reply.isBlank()) {
            throw new JudgeOutputException("empty reply");
        }
        Map<?, ?> root;
        try {
            root = MAPPER.readValue(reply, Map.class);
        } catch (JacksonException e) {
            throw new JudgeOutputException("not valid JSON: " + e.getOriginalMessage(), e);
        }
        checkFields(root, Set.of("claims", "citations", "correctness"), "the top level");

        List<Claim> claims = claims(list(root.get("claims"), "claims"), Set.copyOf(citedMarkers));
        List<CitationSupport> citations = citations(list(root.get("citations"), "citations"), expectedPairs);

        Map<?, ?> correctness = map(root.get("correctness"), "correctness");
        checkFields(correctness, Set.of("reason", "score"), "correctness");
        String reason = text(correctness.get("reason"), "correctness.reason");
        int score = integer(correctness.get("score"), "correctness.score");
        if (score < 1 || score > 5) {
            throw new JudgeOutputException("correctness.score must be 1 to 5, got " + score);
        }
        return new Parsed(claims, citations, score, reason);
    }

    private static List<Claim> claims(List<?> raw, Set<Integer> citedMarkers) {
        if (raw.isEmpty()) {
            throw new JudgeOutputException("claims is empty: every answer states at least one fact");
        }
        List<Claim> claims = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String where = "claims[" + i + "]";
            Map<?, ?> item = map(raw.get(i), where);
            checkFields(item, Set.of("claim", "source_markers", "reason", "supported"), where);
            String claim = text(item.get("claim"), where + ".claim");
            List<Integer> markers = new ArrayList<>();
            for (Object marker : list(item.get("source_markers"), where + ".source_markers")) {
                int m = integer(marker, where + ".source_markers");
                if (!citedMarkers.contains(m)) {
                    throw new JudgeOutputException(where + " names source [" + m
                            + "], which is not one of the SOURCES given " + sorted(citedMarkers));
                }
                markers.add(m);
            }
            String reason = text(item.get("reason"), where + ".reason");
            boolean supported = bool(item.get("supported"), where + ".supported");
            if (supported && markers.isEmpty()) {
                throw new JudgeOutputException(where + " is supported but source_markers is empty");
            }
            claims.add(new Claim(claim, markers, reason, supported));
        }
        return claims;
    }

    private static List<CitationSupport> citations(List<?> raw, List<Pair> expectedPairs) {
        if (raw.size() != expectedPairs.size()) {
            throw new JudgeOutputException("citations must have exactly " + expectedPairs.size()
                    + " entries (one per pair in PAIRS TO JUDGE), got " + raw.size());
        }
        List<CitationSupport> citations = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String where = "citations[" + i + "]";
            Map<?, ?> item = map(raw.get(i), where);
            checkFields(item, Set.of("sentence_id", "marker", "reason", "supports"), where);
            CitationSupport support = new CitationSupport(
                    integer(item.get("sentence_id"), where + ".sentence_id"),
                    integer(item.get("marker"), where + ".marker"),
                    text(item.get("reason"), where + ".reason"),
                    bool(item.get("supports"), where + ".supports"));
            Pair expected = expectedPairs.get(i);
            if (!support.pair().equals(expected)) {
                throw new JudgeOutputException(where + " is S" + support.sentenceId() + " [" + support.marker()
                        + "] but the pair in that position is S" + expected.sentenceId() + " [" + expected.marker() + "]");
            }
            citations.add(support);
        }
        return citations;
    }

    private static void checkFields(Map<?, ?> map, Set<String> required, String where) {
        for (String field : required) {
            if (!map.containsKey(field)) {
                throw new JudgeOutputException(where + " is missing '" + field + "'");
            }
        }
        for (Object key : map.keySet()) {
            if (!required.contains(String.valueOf(key))) {
                throw new JudgeOutputException(where + " has an unknown field '" + key + "'");
            }
        }
    }

    private static Map<?, ?> map(Object value, String where) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new JudgeOutputException(where + " must be an object");
    }

    private static List<?> list(Object value, String where) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new JudgeOutputException(where + " must be a list");
    }

    private static String text(Object value, String where) {
        if (value instanceof String s && !s.isBlank()) {
            return s.strip();
        }
        throw new JudgeOutputException(where + " must be a non-empty string");
    }

    private static boolean bool(Object value, String where) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new JudgeOutputException(where + " must be true or false");
    }

    private static int integer(Object value, String where) {
        // Jackson reads JSON whole numbers as Integer (or Long if large); 4.0 is a Double and is refused.
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        throw new JudgeOutputException(where + " must be a whole number");
    }

    private static List<Integer> sorted(Collection<Integer> markers) {
        return markers.stream().sorted().toList();
    }
}
