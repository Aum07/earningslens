package com.earningslens.ingest;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads corpus/manifest.yaml into a list of {@link ManifestEntry}.
 *
 * Fails fast with the row number if a row is invalid, and rejects duplicates
 * (same company, period and doc type, or the same URL twice).
 *
 * SafeConstructor makes SnakeYAML build only plain maps, lists, strings and
 * numbers, never arbitrary Java objects. Always use it for YAML you read from disk.
 */
@Component
public class ManifestLoader {

    public List<ManifestEntry> load(Path path) throws IOException {
        Object root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }
        if (!(root instanceof Map<?, ?> map) || !(map.get("documents") instanceof List<?> rows)) {
            throw new IllegalArgumentException(path + ": expected a top-level 'documents' list");
        }

        List<ManifestEntry> entries = new ArrayList<>();
        Set<String> seenDocuments = new HashSet<>();
        Set<String> seenUrls = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            String where = "%s, document #%d".formatted(path, i + 1);
            if (!(rows.get(i) instanceof Map<?, ?> row)) {
                throw new IllegalArgumentException(where + ": expected a mapping");
            }
            ManifestEntry entry;
            try {
                entry = toEntry(row);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(where + ": " + e.getMessage(), e);
            }
            if (!seenDocuments.add(entry.rawFileName())) {
                throw new IllegalArgumentException(where + ": duplicate document " + entry.rawFileName());
            }
            if (!seenUrls.add(entry.url())) {
                throw new IllegalArgumentException(where + ": duplicate url " + entry.url());
            }
            entries.add(entry);
        }
        return List.copyOf(entries);
    }

    private static ManifestEntry toEntry(Map<?, ?> row) {
        return new ManifestEntry(
                text(row, "ticker"),
                text(row, "company_name"),
                number(row, "fiscal_year"),
                number(row, "fiscal_quarter"),
                DocType.valueOf(text(row, "doc_type")),
                text(row, "url"),
                DocumentFormat.valueOf(text(row, "format")));
    }

    private static String text(Map<?, ?> row, String field) {
        Object value = row.get(field);
        if (value == null) {
            throw new IllegalArgumentException("missing field '" + field + "'");
        }
        return value.toString().trim();
    }

    private static int number(Map<?, ?> row, String field) {
        if (!(row.get(field) instanceof Number number)) {
            throw new IllegalArgumentException("field '" + field + "' must be a number");
        }
        return number.intValue();
    }
}
