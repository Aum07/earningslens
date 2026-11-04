package com.earningslens.eval;

import com.earningslens.eval.JudgeVerdict.Pair;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roadmap E5.1: the judge's reply is parsed and every rule the schema cannot express is checked. No network. */
class JudgeResponseParserTest {

    private static final List<Integer> CITED = List.of(2, 5);
    private static final List<Pair> PAIRS = List.of(new Pair(1, 2), new Pair(2, 5));

    private static final String CLAIMS = """
            "claims": [
              {"claim": "Revenue $81.3B", "source_markers": [2], "reason": "Source 2 gives $81,273 million.", "supported": true},
              {"claim": "Up 17%", "source_markers": [], "reason": "No source gives the growth rate.", "supported": false}
            ]""";
    private static final String CITATIONS = """
            "citations": [
              {"sentence_id": 1, "marker": 2, "reason": "States revenue.", "supports": true},
              {"sentence_id": 2, "marker": 5, "reason": "About a different quarter.", "supports": false}
            ]""";
    private static final String CORRECTNESS = """
            "correctness": {"reason": "Revenue right, growth right.", "score": 5}""";

    private static String reply(String claims, String citations, String correctness) {
        return "{" + claims + "," + citations + "," + correctness + "}";
    }

    @Test
    void validReplyIsParsed() {
        JudgeResponseParser.Parsed parsed = JudgeResponseParser.parse(reply(CLAIMS, CITATIONS, CORRECTNESS), CITED, PAIRS);

        assertThat(parsed.claims()).hasSize(2);
        assertThat(parsed.claims().get(0).sourceMarkers()).containsExactly(2);
        assertThat(parsed.claims().get(0).supported()).isTrue();
        assertThat(parsed.claims().get(1).supported()).isFalse();
        assertThat(parsed.citations()).extracting(JudgeVerdict.CitationSupport::pair).containsExactlyElementsOf(PAIRS);
        assertThat(parsed.correctness()).isEqualTo(5);
        assertThat(parsed.correctnessReason()).isEqualTo("Revenue right, growth right.");

        // The metrics computed from it (spec 11.2).
        JudgeVerdict verdict = new JudgeVerdict(parsed.claims(), parsed.citations(), parsed.correctness(),
                parsed.correctnessReason(), "gpt-4.1", 100, 50, 1);
        assertThat(verdict.faithfulness()).isEqualTo(0.5);
        assertThat(verdict.citationPrecision()).isEqualTo(0.5);
    }

    @Test
    void noCitationsGivesNullPrecision() {
        JudgeResponseParser.Parsed parsed = JudgeResponseParser.parse(
                reply(CLAIMS.replace("[2]", "[]").replace("true}", "false}"), "\"citations\": []", CORRECTNESS),
                List.of(), List.of());
        JudgeVerdict verdict = new JudgeVerdict(parsed.claims(), parsed.citations(), 1, "x", "m", null, null, 1);

        assertThat(verdict.citationPrecision()).isNull();
        assertThat(verdict.faithfulness()).isZero();
    }

    @Test
    void notJsonIsRejected() {
        assertThatThrownBy(() -> JudgeResponseParser.parse("Sure! Here is my grading: ...", CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("not valid JSON");
        assertThatThrownBy(() -> JudgeResponseParser.parse("  ", CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("empty");
    }

    @Test
    void scoreOutsideOneToFiveIsRejected() {
        for (String score : List.of("0", "6", "4.5", "\"5\"")) {
            String bad = CORRECTNESS.replace("\"score\": 5", "\"score\": " + score);
            assertThatThrownBy(() -> JudgeResponseParser.parse(reply(CLAIMS, CITATIONS, bad), CITED, PAIRS))
                    .as("score %s", score)
                    .isInstanceOf(JudgeOutputException.class).hasMessageContaining("correctness.score");
        }
    }

    @Test
    void missingOrUnknownFieldsAreRejected() {
        assertThatThrownBy(() -> JudgeResponseParser.parse("{" + CLAIMS + "," + CITATIONS + "}", CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("missing 'correctness'");
        String extra = CORRECTNESS.replace("\"score\": 5", "\"score\": 5, \"confidence\": 0.9");
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply(CLAIMS, CITATIONS, extra), CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("unknown field 'confidence'");
        String blankReason = CORRECTNESS.replace("Revenue right, growth right.", " ");
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply(CLAIMS, CITATIONS, blankReason), CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("correctness.reason");
    }

    @Test
    void claimRulesAreChecked() {
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply("\"claims\": []", CITATIONS, CORRECTNESS), CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("claims is empty");

        String unknownSource = CLAIMS.replace("\"source_markers\": [2]", "\"source_markers\": [7]");
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply(unknownSource, CITATIONS, CORRECTNESS), CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("[7]");

        String supportedWithoutSource = CLAIMS.replace("\"source_markers\": [2]", "\"source_markers\": []");
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply(supportedWithoutSource, CITATIONS, CORRECTNESS),
                CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("source_markers is empty");
    }

    @Test
    void citationsMustAnswerExactlyTheSentPairsInOrder() {
        String missingOne = """
                "citations": [{"sentence_id": 1, "marker": 2, "reason": "r", "supports": true}]""";
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply(CLAIMS, missingOne, CORRECTNESS), CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("exactly 2 entries");

        String wrongPair = CITATIONS.replace("\"sentence_id\": 2, \"marker\": 5", "\"sentence_id\": 2, \"marker\": 2");
        assertThatThrownBy(() -> JudgeResponseParser.parse(reply(CLAIMS, wrongPair, CORRECTNESS), CITED, PAIRS))
                .isInstanceOf(JudgeOutputException.class).hasMessageContaining("S2 [2]");
    }

    @Test
    void schemaIsValidJsonWithAllFieldsRequired() {
        Map<?, ?> schema = JsonMapper.builder().build().readValue(JudgeResponseParser.SCHEMA, Map.class);

        // OpenAI strict mode needs every property listed in "required" and additionalProperties false.
        assertThat(schema.get("required")).isEqualTo(List.of("claims", "citations", "correctness"));
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
    }
}
