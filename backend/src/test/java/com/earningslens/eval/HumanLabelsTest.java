package com.earningslens.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roadmap E5.2: the labels file is written blank, read strictly, and finished work is detected. */
class HumanLabelsTest {

    @TempDir
    Path dir;

    @Test
    void templateHasOneBlankLinePerSampleAndReadsBack() throws IOException {
        Path file = dir.resolve("human-labels.jsonl");
        HumanLabels.writeTemplate(file, List.of(LabelPrepRunTest.sample("q001", "C", "A [1]."),
                LabelPrepRunTest.sample("q002", "A", "B [1].")));

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).containsExactly(
                "{\"label_id\":\"q001-C\",\"item_id\":\"q001\",\"config\":\"C\",\"faithful\":null,\"correctness\":null,\"note\":\"\"}",
                "{\"label_id\":\"q002-A\",\"item_id\":\"q002\",\"config\":\"A\",\"faithful\":null,\"correctness\":null,\"note\":\"\"}");

        List<HumanLabel> labels = HumanLabels.load(file);
        assertThat(labels).extracting(HumanLabel::labelId).containsExactly("q001-C", "q002-A");
        assertThat(labels).noneMatch(HumanLabel::started);
        assertThat(HumanLabels.hasWork(file)).isFalse();
        assertThatThrownBy(() -> HumanLabels.requireComplete(labels)).hasMessageContaining("q001-C, q002-A");
    }

    @Test
    void filledLabelsAreReadAndCountAsWork() throws IOException {
        Path file = write("""
                {"label_id": "q001-C", "item_id": "q001", "config": "C", "faithful": true, "correctness": 4, "note": "misses 15%"}
                {"label_id": "q002-A", "item_id": "q002", "config": "A", "faithful": false, "correctness": 1, "note": ""}
                """);

        List<HumanLabel> labels = HumanLabels.load(file);

        assertThat(labels.get(0)).isEqualTo(new HumanLabel("q001-C", "q001", "C", true, 4, "misses 15%"));
        assertThat(labels.get(1).faithful()).isFalse();
        HumanLabels.requireComplete(labels);   // no exception
        assertThat(HumanLabels.hasWork(file)).isTrue();
    }

    @Test
    void typosAreReportedWithTheLineNumber() throws IOException {
        String good = "{\"label_id\": \"q001-C\", \"item_id\": \"q001\", \"config\": \"C\", \"faithful\": true, \"correctness\": 4, \"note\": \"\"}\n";

        assertThatThrownBy(() -> HumanLabels.load(write(good + good.replace("true", "\"yes\""))))
                .hasMessageContaining("line 2").hasMessageContaining("faithful must be true, false or null");
        assertThatThrownBy(() -> HumanLabels.load(write(good.replace("\"correctness\": 4", "\"correctness\": 6"))))
                .hasMessageContaining("line 1").hasMessageContaining("correctness must be a whole number 1 to 5");
        assertThatThrownBy(() -> HumanLabels.load(write(good + good)))
                .hasMessageContaining("duplicate label_id q001-C");
        assertThatThrownBy(() -> HumanLabels.load(write(good.replace("\"note\"", "\"notes\""))))
                .hasMessageContaining("unknown field 'notes'");
        assertThatThrownBy(() -> HumanLabels.load(write(good.replace("}", ""))))
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void missingFileHasNoWork() throws IOException {
        assertThat(HumanLabels.hasWork(dir.resolve("nothing.jsonl"))).isFalse();
    }

    private Path write(String content) throws IOException {
        Path file = Files.createTempFile(dir, "labels", ".jsonl");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
