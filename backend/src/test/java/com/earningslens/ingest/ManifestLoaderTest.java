package com.earningslens.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: no Spring context and no database needed. */
class ManifestLoaderTest {

    private final ManifestLoader loader = new ManifestLoader();

    @TempDir
    Path tempDir;

    @Test
    void realManifestHasThreeDocumentsForEachOfTwelvePeriods() throws IOException {
        // Maven and IntelliJ run tests from backend/, so the manifest is one level up.
        List<ManifestEntry> entries = loader.load(Path.of("../corpus/manifest.yaml"));

        assertThat(entries).hasSize(36);
        Map<String, List<ManifestEntry>> byPeriod = entries.stream()
                .collect(Collectors.groupingBy(e -> e.ticker() + " " + e.periodLabel()));
        assertThat(byPeriod).hasSize(12);

        byPeriod.forEach((period, docs) -> {
            Set<DocType> types = docs.stream().map(ManifestEntry::docType).collect(Collectors.toSet());
            DocType filing = docs.get(0).fiscalQuarter() == 4 ? DocType.FORM_10K : DocType.FORM_10Q;
            assertThat(types).as(period)
                    .containsExactlyInAnyOrder(DocType.PRESS_RELEASE, DocType.TRANSCRIPT, filing);
        });
    }

    @Test
    void rawFileNameIsReadable() {
        ManifestEntry entry = new ManifestEntry("MSFT", "Microsoft Corporation", 2026, 2,
                DocType.PRESS_RELEASE, "https://example.com/a.htm", DocumentFormat.HTML);

        assertThat(entry.rawFileName()).isEqualTo("MSFT_FY2026_Q2_PRESS_RELEASE.html");
        assertThat(entry.periodLabel()).isEqualTo("FY2026 Q2");
    }

    @Test
    void rejectsDuplicateDocument() throws IOException {
        Path manifest = write("""
                documents:
                  - {ticker: MSFT, company_name: Microsoft, fiscal_year: 2026, fiscal_quarter: 2,
                     doc_type: PRESS_RELEASE, url: "https://example.com/a", format: HTML}
                  - {ticker: MSFT, company_name: Microsoft, fiscal_year: 2026, fiscal_quarter: 2,
                     doc_type: PRESS_RELEASE, url: "https://example.com/b", format: HTML}
                """);

        assertThatThrownBy(() -> loader.load(manifest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("document #2")
                .hasMessageContaining("duplicate document");
    }

    @Test
    void rejectsQuarterOutOfRange() throws IOException {
        Path manifest = write("""
                documents:
                  - {ticker: MSFT, company_name: Microsoft, fiscal_year: 2026, fiscal_quarter: 5,
                     doc_type: PRESS_RELEASE, url: "https://example.com/a", format: HTML}
                """);

        assertThatThrownBy(() -> loader.load(manifest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fiscal_quarter");
    }

    @Test
    void rejectsUnknownDocType() throws IOException {
        Path manifest = write("""
                documents:
                  - {ticker: MSFT, company_name: Microsoft, fiscal_year: 2026, fiscal_quarter: 2,
                     doc_type: SLIDES, url: "https://example.com/a", format: HTML}
                """);

        assertThatThrownBy(() -> loader.load(manifest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("document #1");
    }

    private Path write(String yaml) throws IOException {
        Path file = tempDir.resolve("manifest.yaml");
        Files.writeString(file, yaml);
        return file;
    }
}
