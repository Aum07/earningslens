package com.earningslens.eval;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roadmap E5.1: the real eval/judge-rubric.md has a usable judge prompt that covers all three tasks.
 * Reads the file only (tests run from backend/), no network.
 */
class JudgeRubricFileTest {

    private static final Path RUBRIC = Path.of("../eval/judge-rubric.md");

    @Test
    void rubricHasAJudgePromptForAllThreeTasks() throws IOException {
        String prompt = JudgeClient.extractPrompt(Files.readString(RUBRIC, StandardCharsets.UTF_8));

        assertThat(prompt).contains("Task 1: claims", "Task 2: citation support", "Task 3: correctness");
        // Every field name in the schema is explained to the judge.
        assertThat(prompt).contains("source_markers", "supported", "sentence_id", "marker", "supports", "reason");
        for (int score = 1; score <= 5; score++) {
            assertThat(prompt).contains("- " + score + ": ");
        }
        assertThat(prompt).contains("PAIRS TO JUDGE", "REFERENCE ANSWER", "SOURCES", "SENTENCES");
        // The notes above the heading are for people and are not sent.
        assertThat(prompt).doesNotContain("Change log");
    }

    @Test
    void rubricFollowsTheDocumentationStyle() throws IOException {
        assertThat(Files.readString(RUBRIC, StandardCharsets.UTF_8)).doesNotContain("—");   // no em dashes (spec 0.9)
    }
}
