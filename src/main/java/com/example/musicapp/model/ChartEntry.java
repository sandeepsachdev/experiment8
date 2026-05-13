package com.example.musicapp.model;

public record ChartEntry(
        int position,
        String title,
        String artistName,
        String albumCover,
        String link,
        String previewUrl
) {}
