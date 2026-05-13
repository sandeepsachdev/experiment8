package com.example.musicapp.model;

public record Track(
        Long id,
        String title,
        String artistName,
        Long artistId,
        String albumTitle,
        String albumCover,
        String previewUrl,
        String link,
        Integer durationSeconds,
        Integer rank
) {}
