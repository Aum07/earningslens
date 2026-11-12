package com.earningslens.figures;

import com.earningslens.config.SecProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the free SEC data API (no key). The SEC requires a User-Agent with a name and contact email
 * (earningslens.sec.user-agent) and at most 10 requests per second; the caller caches results, so this runs
 * about once per company every few hours.
 */
@Component
public class HttpSecFactsClient implements SecFactsClient {

    static final String URL = "https://data.sec.gov/api/xbrl/companyfacts/CIK%s.json";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String userAgent;

    public HttpSecFactsClient(SecProperties sec) {
        this.userAgent = sec.userAgent();
    }

    @Override
    public String companyFacts(String cik) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL.formatted(cik)))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("SEC company facts returned HTTP " + response.statusCode());
        }
        return response.body();
    }
}
