package com.earningslens.ingest;

import com.earningslens.config.IngestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The ingestion job: for every manifest document, download it (if missing) and ingest it
 * (parse, clean, chunk, embed, store). Runs only with the "ingest" profile, then exits.
 *
 * Safe to run again: files already downloaded are not fetched, and documents already in
 * the database (same SHA-256) are skipped, so a second run inserts nothing and costs nothing.
 *
 * SPRING CONCEPT, @Profile("ingest"): Spring only creates this bean when the ingest profile
 * is active, so normal app starts and tests never run it.
 */
@Component
@Profile("ingest")
public class IngestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestRunner.class);

    private final ManifestLoader manifestLoader;
    private final DocumentFetcher fetcher;
    private final IngestionService ingestion;
    private final EmbeddingService embeddingService;
    private final IngestProperties ingest;

    public IngestRunner(ManifestLoader manifestLoader, DocumentFetcher fetcher, IngestionService ingestion,
                        EmbeddingService embeddingService, IngestProperties ingest) {
        this.manifestLoader = manifestLoader;
        this.fetcher = fetcher;
        this.ingestion = ingestion;
        this.embeddingService = embeddingService;
        this.ingest = ingest;
    }

    @Override
    public void run(String... args) throws Exception {
        Path manifestPath = Path.of(ingest.manifestPath());
        List<ManifestEntry> entries = manifestLoader.load(manifestPath);
        log.info("Manifest {}: {} documents", manifestPath.toAbsolutePath().normalize(), entries.size());

        int inserted = 0;
        int skipped = 0;
        int chunks = 0;
        List<String> failures = new ArrayList<>();
        for (ManifestEntry entry : entries) {
            try {
                fetcher.fetch(entry);
                IngestResult result = ingestion.ingest(entry, fetcher.targetFor(entry));
                if (result.inserted()) {
                    inserted++;
                    chunks += result.chunks();
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                // Keep going so one bad document does not hide problems with the others.
                // Nothing half-written stays behind: each document is stored in one transaction.
                log.warn("FAILED {}: {}", entry.rawFileName(), e.toString());
                failures.add(entry.rawFileName());
            }
        }

        log.info("Ingest finished: {} documents inserted ({} chunks), {} already in the database, {} failed",
                inserted, chunks, skipped, failures.size());
        log.info("Embedding tokens used in this run: {}", embeddingService.totalTokens());
        if (!failures.isEmpty()) {
            throw new IllegalStateException(failures.size() + " document(s) failed: " + failures);
        }
    }
}
