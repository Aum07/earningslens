package com.earningslens.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading latest.json from disk: present, missing, and broken. Uses a temporary folder, not eval/reports. */
class LatestEvalReportTest {

    @TempDir Path dir;

    @Test
    void readsTheFile() throws Exception {
        Path file = dir.resolve("latest.json");
        Files.writeString(file, "{\"split\": \"test\", \"items\": 50}");

        assertThat(new LatestEvalReport(file).read())
                .hasValueSatisfying(json -> {
                    assertThat(json.get("split").asString()).isEqualTo("test");
                    assertThat(json.get("items").asInt()).isEqualTo(50);
                });
    }

    @Test
    void missingFileMeansNoReportYet() {
        assertThat(new LatestEvalReport(dir.resolve("latest.json")).read()).isEmpty();
    }

    @Test
    void brokenJsonIsAnError() throws Exception {
        Path file = dir.resolve("latest.json");
        Files.writeString(file, "{not json");

        assertThatThrownBy(() -> new LatestEvalReport(file).read()).isInstanceOf(RuntimeException.class);
    }
}
