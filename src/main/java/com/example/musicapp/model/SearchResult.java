package com.example.musicapp.model;

import java.util.List;

public record SearchResult(
        String query,
        List<Track> tracks,
        List<Artist> artists
) {}
