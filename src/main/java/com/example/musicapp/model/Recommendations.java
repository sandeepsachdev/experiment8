package com.example.musicapp.model;

import java.util.List;

public record Recommendations(
        Artist seedArtist,
        Track seedTrack,
        List<Artist> similarArtists,
        List<Track> similarTracks
) {}
