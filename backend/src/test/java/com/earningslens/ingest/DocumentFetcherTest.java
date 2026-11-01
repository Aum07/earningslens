package com.earningslens.ingest;

import com.earningslens.config.IngestProperties;
import com.earningslens.config.SecProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the fetcher against a tiny HTTP server on localhost (built into the JDK),
 * so no internet connection is needed.
 */
class DocumentFetcherTest {

    private static final String USER_AGENT = "EarningsLens test test@example.com";

    @TempDir
    Path rawDir;

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final List<String> userAgentsSeen = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            userAgentsSeen.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/missing")) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] body = ("<html><body>" + path + "</body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsFileAndSendsUserAgent() throws Exception {
        DocumentFetcher fetcher = fetcher(10);
        ManifestEntry entry = entry("/report.html", 2);

        FetchResult result = fetcher.fetch(entry);

        assertThat(result).isEqualTo(FetchResult.DOWNLOADED);
        Path file = rawDir.resolve("MSFT_FY2026_Q2_PRESS_RELEASE.html");
        assertThat(Files.readString(file)).contains("/report.html");
        assertThat(userAgentsSeen).containsExactly(USER_AGENT);
    }

    @Test
    void secondFetchSkipsExistingFile() throws Exception {
        DocumentFetcher fetcher = fetcher(10);
        ManifestEntry entry = entry("/report.html", 2);

        fetcher.fetch(entry);
        FetchResult second = fetcher.fetch(entry);

        assertThat(second).isEqualTo(FetchResult.SKIPPED);
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void httpErrorThrowsAndLeavesNoFile() {
        DocumentFetcher fetcher = fetcher(10);
        ManifestEntry entry = entry("/missing.html", 2);

        assertThatThrownBy(() -> fetcher.fetch(entry))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("HTTP 404");
        assertThat(rawDir).isEmptyDirectory();
    }

    @Test
    void waitsBetweenRequests() throws Exception {
        long delayMillis = 300;
        DocumentFetcher fetcher = fetcher(delayMillis);

        long start = System.nanoTime();
        fetcher.fetch(entry("/q1.html", 1));
        fetcher.fetch(entry("/q2.html", 2));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(delayMillis);
        assertThat(requestCount).hasValue(2);
    }

    private DocumentFetcher fetcher(long minDelayMillis) {
        return new DocumentFetcher(
                new SecProperties(USER_AGENT, minDelayMillis),
                new IngestProperties("unused", rawDir.toString()));
    }

    private ManifestEntry entry(String path, int quarter) {
        return new ManifestEntry("MSFT", "Microsoft Corporation", 2026, quarter,
                DocType.PRESS_RELEASE, baseUrl + path, DocumentFormat.HTML);
    }
}
