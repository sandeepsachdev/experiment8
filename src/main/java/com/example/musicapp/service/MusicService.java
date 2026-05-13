package com.example.musicapp.service;

import com.example.musicapp.model.Artist;
import com.example.musicapp.model.Recommendations;
import com.example.musicapp.model.SearchResult;
import com.example.musicapp.model.Track;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Uses the public Deezer API (https://api.deezer.com) — no auth required.
 * Endpoints used:
 *   /search/track?q=...     — track search
 *   /search/artist?q=...    — artist search
 *   /artist/{id}/related    — similar artists
 *   /artist/{id}/top        — top tracks for an artist
 *   /track/{id}             — track detail (used to derive recommendations)
 */
@Service
public class MusicService {

    private final WebClient deezer;

    public MusicService(WebClient deezerClient) {
        this.deezer = deezerClient;
    }

    @Cacheable("search")
    public SearchResult search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResult(query, List.of(), List.of());
        }
        List<Track> tracks = searchTracks(query, 25);
        List<Artist> artists = searchArtists(query, 12);
        return new SearchResult(query, tracks, artists);
    }

    public List<Track> searchTracks(String query, int limit) {
        JsonNode root = getJson("/search/track", uri -> uri
                .queryParam("q", query)
                .queryParam("limit", limit));
        return parseTracks(root);
    }

    public List<Artist> searchArtists(String query, int limit) {
        JsonNode root = getJson("/search/artist", uri -> uri
                .queryParam("q", query)
                .queryParam("limit", limit));
        return parseArtists(root);
    }

    @Cacheable("artist")
    public Optional<Artist> getArtist(long artistId) {
        JsonNode node = getJson("/artist/" + artistId, uri -> uri);
        if (node == null || node.has("error")) return Optional.empty();
        return Optional.of(parseArtist(node));
    }

    @Cacheable("track")
    public Optional<Track> getTrack(long trackId) {
        JsonNode node = getJson("/track/" + trackId, uri -> uri);
        if (node == null || node.has("error")) return Optional.empty();
        return Optional.of(parseTrack(node));
    }

    @Cacheable("similarArtists")
    public List<Artist> getSimilarArtists(long artistId, int limit) {
        JsonNode root = getJson("/artist/" + artistId + "/related", uri -> uri
                .queryParam("limit", limit));
        return parseArtists(root);
    }

    @Cacheable("topTracks")
    public List<Track> getTopTracks(long artistId, int limit) {
        JsonNode root = getJson("/artist/" + artistId + "/top", uri -> uri
                .queryParam("limit", limit));
        return parseTracks(root);
    }

    /**
     * Build recommendations from an artist or track seed. Pulls similar artists
     * for the seed and aggregates their top tracks for "music you may like".
     */
    public Recommendations recommendForArtist(long artistId) {
        Optional<Artist> seed = getArtist(artistId);
        if (seed.isEmpty()) {
            return new Recommendations(null, null, List.of(), List.of());
        }
        List<Artist> similar = getSimilarArtists(artistId, 12);
        List<Track> tracks = new ArrayList<>();
        for (Artist a : similar.stream().limit(6).toList()) {
            tracks.addAll(getTopTracks(a.id(), 3));
        }
        return new Recommendations(seed.get(), null, similar, tracks);
    }

    public Recommendations recommendForTrack(long trackId) {
        Optional<Track> seedTrack = getTrack(trackId);
        if (seedTrack.isEmpty()) {
            return new Recommendations(null, null, List.of(), List.of());
        }
        Long artistId = seedTrack.get().artistId();
        Recommendations base = artistId != null
                ? recommendForArtist(artistId)
                : new Recommendations(null, null, List.of(), List.of());
        return new Recommendations(base.seedArtist(), seedTrack.get(),
                base.similarArtists(), base.similarTracks());
    }

    private JsonNode getJson(String path, java.util.function.Function<
            org.springframework.web.util.UriBuilder,
            org.springframework.web.util.UriBuilder> uriCustomizer) {
        try {
            return deezer.get()
                    .uri(uriBuilder -> uriCustomizer.apply(uriBuilder.path(path)).build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(e -> Mono.empty())
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Track> parseTracks(JsonNode root) {
        if (root == null) return Collections.emptyList();
        JsonNode data = root.has("data") ? root.get("data") : root;
        if (!data.isArray()) return Collections.emptyList();
        List<Track> tracks = new ArrayList<>();
        data.forEach(t -> tracks.add(parseTrack(t)));
        return tracks;
    }

    private Track parseTrack(JsonNode t) {
        JsonNode artistNode = t.path("artist");
        JsonNode albumNode = t.path("album");
        return new Track(
                t.path("id").asLong(),
                t.path("title").asText(null),
                artistNode.path("name").asText(null),
                artistNode.has("id") ? artistNode.get("id").asLong() : null,
                albumNode.path("title").asText(null),
                albumNode.path("cover_medium").asText(albumNode.path("cover").asText(null)),
                t.path("preview").asText(null),
                t.path("link").asText(null),
                t.has("duration") ? t.get("duration").asInt() : null,
                t.has("rank") ? t.get("rank").asInt() : null
        );
    }

    private List<Artist> parseArtists(JsonNode root) {
        if (root == null) return Collections.emptyList();
        JsonNode data = root.has("data") ? root.get("data") : root;
        if (!data.isArray()) return Collections.emptyList();
        List<Artist> artists = new ArrayList<>();
        data.forEach(a -> artists.add(parseArtist(a)));
        return artists;
    }

    private Artist parseArtist(JsonNode a) {
        return new Artist(
                a.path("id").asLong(),
                a.path("name").asText(null),
                a.path("picture_medium").asText(a.path("picture").asText(null)),
                a.has("nb_fan") ? a.get("nb_fan").asInt() : null,
                a.has("nb_album") ? a.get("nb_album").asInt() : null,
                a.path("link").asText(null)
        );
    }
}
