package com.earningslens.eval;

import com.earningslens.generation.CitationCheck.CitedSentence;
import com.earningslens.generation.LlmReply;
import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roadmap E5.1: what JudgeClient sends, and how it handles valid, invalid and cut-off replies.
 * The judge model is a fake that replays canned replies: no OpenAI calls, runs in CI.
 */
class JudgeClientTest {

    private static final String RUBRIC = "# Judge rubric\n\nNotes for people.\n\n## Judge prompt\n\nYou grade answers.\n";

    private static final String VALID = """
            {"claims": [{"claim": "Revenue $81.3B", "source_markers": [2], "reason": "In source 2.", "supported": true}],
             "citations": [{"sentence_id": 1, "marker": 2, "reason": "States it.", "supports": true}],
             "correctness": {"reason": "Matches.", "score": 4}}""";

    private final List<String> systems = new ArrayList<>();
    private final List<String> users = new ArrayList<>();
    private final List<String> schemas = new ArrayList<>();

    /** A fake judge that returns the given replies in order. */
    private JudgeClient clientReplying(LlmReply... replies) {
        List<LlmReply> queue = new ArrayList<>(List.of(replies));
        return new JudgeClient((system, user, schema) -> {
            systems.add(system);
            users.add(user);
            schemas.add(schema);
            return queue.remove(0);
        }, 2, () -> RUBRIC);
    }

    private static LlmReply reply(String text) {
        return new LlmReply(text, "gpt-4.1-2025-04-14", 1000, 200, false);
    }

    @Test
    void sendsRubricPromptCitedSourcesOnlyAndThePairs() {
        JudgeVerdict verdict = clientReplying(reply(VALID)).judge(input());

        assertThat(systems).containsExactly("You grade answers.");   // only the part after "## Judge prompt"
        assertThat(schemas).containsExactly(JudgeResponseParser.SCHEMA);
        String user = users.get(0);
        assertThat(user).contains("QUESTION:\nWhat was Microsoft's total revenue in Q2 FY2026?")
                .contains("REFERENCE ANSWER:\n$81.3 billion, up 17%")
                .contains("ANSWER:\nRevenue was $81.3 billion [2].")
                .contains("[2] (MSFT | FY2026 Q2 | PRESS_RELEASE)\nRevenue was $81,273 million.")
                .contains("S1 [2]: Revenue was $81.3 billion [2].")
                .contains("PAIRS TO JUDGE (1):\nS1 [2]");
        assertThat(user).doesNotContain("[1] (").doesNotContain("Unrelated text");   // uncited source not shown

        assertThat(verdict.correctness()).isEqualTo(4);
        assertThat(verdict.faithfulness()).isEqualTo(1.0);
        assertThat(verdict.citationPrecision()).isEqualTo(1.0);
        assertThat(verdict.model()).isEqualTo("gpt-4.1-2025-04-14");
        assertThat(verdict.attempts()).isEqualTo(1);
        assertThat(verdict.promptTokens()).isEqualTo(1000);
        assertThat(verdict.completionTokens()).isEqualTo(200);
    }

    @Test
    void invalidReplyIsRetriedWithTheErrorAndTokensAddUp() {
        String badScore = VALID.replace("\"score\": 4", "\"score\": 9");
        JudgeVerdict verdict = clientReplying(reply(badScore), reply(VALID)).judge(input());

        assertThat(verdict.attempts()).isEqualTo(2);
        assertThat(verdict.correctness()).isEqualTo(4);
        assertThat(verdict.promptTokens()).isEqualTo(2000);
        assertThat(verdict.completionTokens()).isEqualTo(400);
        assertThat(users.get(1)).startsWith(users.get(0))
                .contains("YOUR PREVIOUS REPLY WAS REJECTED: correctness.score must be 1 to 5, got 9");
    }

    @Test
    void failsAfterMaxAttempts() {
        JudgeClient client = clientReplying(reply("not json"), new LlmReply(VALID.substring(0, 40), "m", 1, 1, true));

        assertThatThrownBy(() -> client.judge(input()))
                .isInstanceOf(JudgeOutputException.class)
                .hasMessageContaining("after 2 attempts")
                .hasMessageContaining("cut off");
        assertThat(users).hasSize(2);
    }

    @Test
    void unknownTokenCountsGiveNullTotals() {
        JudgeVerdict verdict = clientReplying(new LlmReply(VALID, "m", null, null, false)).judge(input());

        assertThat(verdict.promptTokens()).isNull();
        assertThat(verdict.completionTokens()).isNull();
    }

    @Test
    void repeatedMarkerInOneSentenceIsOnePair() {
        JudgeInput input = new JudgeInput("q?", "gold", "A [2] and B [2].", sources(),
                List.of(new CitedSentence("A [2] and B [2].", List.of(2, 2))));

        assertThat(input.pairs()).containsExactly(new JudgeVerdict.Pair(1, 2));
        assertThat(input.citedMarkers()).containsExactly(2);
    }

    @Test
    void markerWithoutSourceIsRefused() {
        assertThatThrownBy(() -> new JudgeInput("q?", "gold", "A [3].", sources(),
                List.of(new CitedSentence("A [3].", List.of(3)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("[3]");
    }

    @Test
    void promptIsTheTextAfterTheHeading() {
        assertThat(JudgeClient.extractPrompt("x\r\n## Judge prompt\r\nLine 1\r\nLine 2\r\n")).isEqualTo("Line 1\nLine 2");
        assertThatThrownBy(() -> JudgeClient.extractPrompt("# Rubric\nno prompt here\n"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> JudgeClient.extractPrompt("x\n## Judge prompt\n\n"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JudgeInput input() {
        return new JudgeInput("What was Microsoft's total revenue in Q2 FY2026?", "$81.3 billion, up 17%",
                "Revenue was $81.3 billion [2].", sources(),
                List.of(new CitedSentence("Revenue was $81.3 billion [2].", List.of(2))));
    }

    private static List<RetrievedChunk> sources() {
        return List.of(chunk("Unrelated text about devices."), chunk("Revenue was $81,273 million."));
    }

    private static RetrievedChunk chunk(String content) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), 0, "MSFT", 2026, 2, DocType.PRESS_RELEASE,
                content, 10, null, null, null, null, false);
    }
}
