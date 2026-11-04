package com.earningslens.eval;

import com.earningslens.generation.CitationCheck.CitedSentence;
import com.earningslens.ingest.DocType;
import com.earningslens.retrieval.RetrievedChunk;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap E5.2 step 1: which answers are chosen, what the review sheet shows, and that saved answers read back. */
class LabelPrepRunTest {

    @Test
    void aAnswersAreTakenWeakestCategoryFirst() {
        List<LabelSample> c = List.of(sample("q001", "C", "One [1]."));
        List<LabelSample> a = List.of(
                sample("q002", "A", "Easy fact [1]."),                                  // single_fact
                withCategory(sample("q014", "A", "Azure grew ... [1]."), "multi_quarter"),
                withCategory(sample("q021", "A", "Muse is ... [1]."), "qualitative"),
                withCategory(sample("q032", "A", "March revenue was ... [1]."), "unanswerable"),
                withCategory(sample("q012", "A", "Capex outlook ... [1]."), "multi_quarter"));

        assertThat(LabelPrepRun.select(c, a, 5)).extracting(LabelSample::labelId)
                .containsExactly("q001-C", "q032-A", "q012-A", "q014-A", "q021-A");
    }

    private static LabelSample withCategory(LabelSample s, String category) {
        return new LabelSample(s.labelId(), s.itemId(), s.config(), category, s.question(), s.goldAnswer(), s.answer(),
                s.sentences(), s.sources(), s.model(), s.promptTokens(), s.completionTokens());
    }

    @Test
    void allCAnswersFirstThenDifferentAAnswersUpToTheCount() {
        List<LabelSample> c = List.of(sample("q001", "C", "One [1]."), sample("q002", "C", "Two [1]."));
        List<LabelSample> a = List.of(
                sample("q001", "A", "One  [1]. "),       // same text as C (after whitespace), skipped
                sample("q002", "A", "Two, but wrong [1]."),
                sample("q026", "A", "Q1 FY2025 revenue was ... [1]."),   // C abstained on q026
                sample("q027", "A", "Extra [1]."));

        assertThat(LabelPrepRun.select(c, a, 4)).extracting(LabelSample::labelId)
                .containsExactly("q001-C", "q002-C", "q002-A", "q026-A");
        assertThat(LabelPrepRun.select(c, a, 1)).extracting(LabelSample::labelId).containsExactly("q001-C");
        assertThat(LabelPrepRun.select(c, a, 10)).hasSize(5);   // only 5 distinct answers exist
    }

    @Test
    void reviewSheetShowsCitedSourcesOnlyAndNoJudgeScores() {
        LabelSample s = sample("q001", "C", "Revenue was $81.3 billion [2].");

        String md = LabelPrepRun.reviewSheet(List.of(s));

        assertThat(md).contains("## 1. q001-C (single_fact)")
                .contains("**Gold answer:** gold for q001")
                .contains("> Revenue was $81.3 billion [2].")
                .contains("**Cited sources (1):**")
                .contains("[2] MSFT | FY2026 Q2 | PRESS_RELEASE")
                .contains("Revenue was $81,273 million.");
        assertThat(md).doesNotContain("Unrelated text").doesNotContain("—");
        assertThat(md.toLowerCase()).doesNotContain("faithfulness 1").doesNotContain("judge score");
    }

    @Test
    void savedAnswersReadBackUnchanged() {
        JsonMapper mapper = JsonMapper.builder().build();
        List<LabelSample> saved = List.of(sample("q001", "C", "Revenue was $81.3 billion [2]."));

        List<LabelSample> read = mapper.readValue(mapper.writeValueAsString(saved), new TypeReference<List<LabelSample>>() { });

        assertThat(read).isEqualTo(saved);
        assertThat(read.get(0).judgeInput().citedMarkers()).containsExactly(2);
    }

    /** A saved answer citing source [2] of two; source [1] is never cited. */
    static LabelSample sample(String itemId, String config, String answer) {
        List<RetrievedChunk> sources = List.of(chunk("Unrelated text about devices."), chunk("Revenue was $81,273 million."));
        return new LabelSample(LabelSample.labelId(itemId, config), itemId, config, "single_fact", "Question " + itemId + "?",
                "gold for " + itemId, answer, List.of(new CitedSentence(answer, List.of(2))), sources,
                "gpt-4o-mini-2024-07-18", 8000, 40);
    }

    private static RetrievedChunk chunk(String content) {
        return new RetrievedChunk(UUID.nameUUIDFromBytes(content.getBytes()), UUID.randomUUID(), 0, "MSFT", 2026, 2,
                DocType.PRESS_RELEASE, content, 10, null, null, null, null, false);
    }
}
