package com.earningslens.ingest;

import com.earningslens.config.IngestProperties;
import com.earningslens.config.SecProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Downloads manifest documents into corpus/raw.
 *
 * Rules (spec 7.1 and roadmap E1.2):
 * - Skip a file that is already present, so a second run downloads nothing.
 * - Send the configured User-Agent (SEC requires a name and contact email).
 * - Wait at least min-delay-millis between requests (SEC limit is 10 per second).
 *
 * The body is written to "name.part" first and renamed only after a 200 response,
 * so an interrupted or failed download never looks like a finished file.
 *
 * JAVA CONCEPT, java.net.http.HttpClient: the HTTP client built into the JDK
 * (Java 11+). No extra library needed.
 */
@Component
public class DocumentFetcher {

    private static final Logger log = LoggerFactory.getLogger(DocumentFetcher.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient client;
    private final String userAgent;
    private final long minDelayMillis;
    private final Path rawDir;

    private boolean anyRequestSent = false;
    private long lastRequestNanos;

    public DocumentFetcher(SecProperties sec, IngestProperties ingest) {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.userAgent = sec.userAgent();
        this.minDelayMillis = sec.minDelayMillis();
        this.rawDir = Path.of(ingest.rawDir());
    }

    /** Where the downloaded file for this entry lives. */
    public Path targetFor(ManifestEntry entry) {
        return rawDir.resolve(entry.rawFileName());
    }

    /**
     * Downloads one document unless it is already present.
     *
     * @throws IOException on a network error, a non-200 status or an empty body
     */
    public synchronized FetchResult fetch(ManifestEntry entry) throws IOException, InterruptedException {
        Path target = targetFor(entry);
        if (Files.isRegularFile(target) && Files.size(target) > 0) {
            log.debug("Skip {} (already present)", target.getFileName());
            return FetchResult.SKIPPED;
        }

        Files.createDirectories(rawDir);
        waitForRateLimit();

        Path partial = rawDir.resolve(entry.rawFileName() + ".part");
        HttpRequest request = HttpRequest.newBuilder(URI.create(entry.url()))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept", "*/*")
                .GET()
                .build();

        HttpResponse<Path> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofFile(partial));
        } catch (IOException | InterruptedException e) {
            Files.deleteIfExists(partial);
            throw e;
        }

        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial);
            throw new IOException("HTTP %d for %s".formatted(response.statusCode(), entry.url()));
        }
        if (Files.size(partial) == 0) {
            Files.deleteIfExists(partial);
            throw new IOException("Empty response body for " + entry.url());
        }

        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Downloaded {} ({} KB)", target.getFileName(), Files.size(target) / 1024);
        return FetchResult.DOWNLOADED;
    }

    /** Sleeps so that two requests are never closer than minDelayMillis. */
    private void waitForRateLimit() throws InterruptedException {
        if (anyRequestSent) {
            long elapsedMillis = (System.nanoTime() - lastRequestNanos) / 1_000_000;
            long waitMillis = minDelayMillis - elapsedMillis;
            if (waitMillis > 0) {
                Thread.sleep(waitMillis);
            }
        }
        anyRequestSent = true;
        lastRequestNanos = System.nanoTime();
    }
}
