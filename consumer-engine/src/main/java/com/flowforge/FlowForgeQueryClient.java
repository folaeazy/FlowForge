package com.flowforge;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class FlowForgeQueryClient {

    public record UrlEntry(String shortCode, String longUrl) {}
    public record UrlDto(String shortCode, String longUrl, String tenantId, String createdAt, long hitCount) {}
    public enum Verdict { OK, WRONG_TARGET, NOT_FOUND, ERROR }
    public record ProbeResult(UrlEntry entry, Verdict verdict, int status, String location,
                              String cache, long latencyMicros) {}

    private final String baseUrl;
    private final RestClient rest;
    private final HttpClient http;

    public FlowForgeQueryClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.rest = RestClient.create(baseUrl);
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)   // inspect the 302, don't follow it
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /** Newest first, up to maxUrls. */
    public List<UrlEntry> discover(int maxUrls) {
        List<UrlEntry> found = new ArrayList<>();
        int pageSize = 100;
        for (int page = 0; found.size() < maxUrls; page++) {
            List<UrlDto> batch = rest.get()
                    .uri("/api/urls?page={p}&size={s}", page, pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UrlDto>>() {});
            if (batch == null || batch.isEmpty()) break;
            batch.forEach(u -> found.add(new UrlEntry(u.shortCode(), u.longUrl())));
            if (batch.size() < pageSize) break;
        }
        return found.size() > maxUrls ? new ArrayList<>(found.subList(0, maxUrls)) : found;
    }

    public ProbeResult probe(UrlEntry entry) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + entry.shortCode()))
                .timeout(Duration.ofSeconds(10)).GET().build();
        long start = System.nanoTime();
        try {
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            long micros = (System.nanoTime() - start) / 1_000;
            int status = response.statusCode();
            String location = response.headers().firstValue("Location").orElse(null);
            String cache = response.headers().firstValue("X-Cache").orElse("?");

            Verdict verdict;
            if (status == 404) verdict = Verdict.NOT_FOUND;
            else if (status == 302 && entry.longUrl().equals(location)) verdict = Verdict.OK;
            else if (status == 302) verdict = Verdict.WRONG_TARGET;
            else verdict = Verdict.ERROR;
            return new ProbeResult(entry, verdict, status, location, cache, micros);

        } catch (IOException e) {
            return new ProbeResult(entry, Verdict.ERROR, -1, null, "?", (System.nanoTime() - start) / 1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProbeResult(entry, Verdict.ERROR, -1, null, "?", (System.nanoTime() - start) / 1_000);
        }
    }
}
