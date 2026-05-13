package com.example.musicapp.service;

import com.example.musicapp.model.Chart;
import com.example.musicapp.model.ChartEntry;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aggregates charts from two sources:
 *
 *  • Deezer (https://api.deezer.com/chart/{genre_id}) — global "Deezer" chart
 *    optionally filtered by genre.
 *
 *  • Apple Music / iTunes RSS feed
 *    (https://rss.applemarketingtools.com/api/v2/{country}/music/most-played/25/songs.json)
 *    — most-played songs per country.
 */
@Service
public class ChartService {

    private final WebClient deezer;
    private final WebClient appleRss;

    public ChartService(WebClient deezerClient, WebClient appleRssClient) {
        this.deezer = deezerClient;
        this.appleRss = appleRssClient;
    }

    /** Two-letter ISO country code → display name, used by the iTunes RSS feed. */
    public static final Map<String, String> COUNTRIES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("us", "United States");
        m.put("gb", "United Kingdom");
        m.put("ca", "Canada");
        m.put("au", "Australia");
        m.put("de", "Germany");
        m.put("fr", "France");
        m.put("br", "Brazil");
        m.put("mx", "Mexico");
        m.put("jp", "Japan");
        m.put("kr", "South Korea");
        m.put("in", "India");
        m.put("it", "Italy");
        m.put("es", "Spain");
        m.put("nl", "Netherlands");
        m.put("se", "Sweden");
        m.put("ng", "Nigeria");
        m.put("za", "South Africa");
        COUNTRIES = Collections.unmodifiableMap(m);
    }

    /** Deezer genre id → display name. */
    public static final Map<Integer, String> GENRES;
    static {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(0, "All Genres");
        m.put(132, "Pop");
        m.put(116, "Rap / Hip Hop");
        m.put(152, "Rock");
        m.put(113, "Dance");
        m.put(165, "R&B");
        m.put(85, "Alternative");
        m.put(106, "Electro");
        m.put(129, "Jazz");
        m.put(98, "Classical");
        m.put(173, "Films / Games");
        m.put(464, "Metal");
        m.put(169, "Soul & Funk");
        m.put(2, "African Music");
        m.put(16, "Asian Music");
        m.put(153, "Blues");
        m.put(75, "Latin Music");
        m.put(81, "Reggae");
        GENRES = Collections.unmodifiableMap(m);
    }

    /** Apple Music country chart (most-played songs, top 25). */
    @Cacheable("appleChart")
    public Chart getAppleChart(String countryCode) {
        String code = (countryCode == null || countryCode.isBlank())
                ? "us" : countryCode.toLowerCase();
        String country = COUNTRIES.getOrDefault(code, code.toUpperCase());
        JsonNode root;
        try {
            root = appleRss.get()
                    .uri("/api/v2/{country}/music/most-played/25/songs.json", code)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(e -> Mono.empty())
                    .block();
        } catch (Exception e) {
            root = null;
        }
        if (root == null) {
            return new Chart("Apple Music Top Songs", "Apple Music", country, "All Genres", List.of());
        }
        List<ChartEntry> entries = new ArrayList<>();
        JsonNode results = root.path("feed").path("results");
        AtomicInteger pos = new AtomicInteger(1);
        if (results.isArray()) {
            results.forEach(item -> entries.add(new ChartEntry(
                    pos.getAndIncrement(),
                    item.path("name").asText(null),
                    item.path("artistName").asText(null),
                    item.path("artworkUrl100").asText(null),
                    item.path("url").asText(null),
                    null
            )));
        }
        return new Chart("Apple Music Top Songs", "Apple Music", country, "All Genres", entries);
    }

    /** Deezer chart, optionally filtered by genre id. */
    @Cacheable("deezerChart")
    public Chart getDeezerChart(int genreId) {
        String genreName = GENRES.getOrDefault(genreId, "All Genres");
        JsonNode root;
        try {
            root = deezer.get()
                    .uri("/chart/{id}", genreId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(e -> Mono.empty())
                    .block();
        } catch (Exception e) {
            root = null;
        }
        if (root == null) {
            return new Chart("Deezer Top Tracks", "Deezer", "Global", genreName, List.of());
        }
        List<ChartEntry> entries = new ArrayList<>();
        JsonNode tracks = root.path("tracks").path("data");
        AtomicInteger pos = new AtomicInteger(1);
        if (tracks.isArray()) {
            tracks.forEach(t -> {
                JsonNode album = t.path("album");
                JsonNode artist = t.path("artist");
                entries.add(new ChartEntry(
                        pos.getAndIncrement(),
                        t.path("title").asText(null),
                        artist.path("name").asText(null),
                        album.path("cover_medium").asText(album.path("cover").asText(null)),
                        t.path("link").asText(null),
                        t.path("preview").asText(null)
                ));
            });
        }
        return new Chart("Deezer Top Tracks", "Deezer", "Global", genreName, entries);
    }
}
