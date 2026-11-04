package com.earningslens.api;

import com.earningslens.config.ApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads eval/reports/latest.json, which the eval runner keeps up to date (spec 11.5). Read on every request,
 * so a new report shows up without restarting the app. The file is checked to be valid JSON, never edited:
 * the UI must only show numbers the eval runner produced (spec rule 7).
 */
@Component
public class LatestEvalReport {

    private final Path path;
    private final JsonMapper json = JsonMapper.builder().build();

    // Two constructors: @Autowired tells Spring to use this one (the other is for tests).
    @Autowired
    public LatestEvalReport(ApiProperties api) {
        this(Path.of(api.latestEvalReport()));
    }

    LatestEvalReport(Path path) {
        this.path = path;
    }

    /** The report, or empty if no eval has written one yet. A file that is not valid JSON is an error. */
    public Optional<JsonNode> read() {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.readTree(Files.readString(path)));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }

    Path path() {
        return path;
    }
}
