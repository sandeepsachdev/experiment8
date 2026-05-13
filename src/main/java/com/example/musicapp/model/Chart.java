package com.example.musicapp.model;

import java.util.List;

public record Chart(
        String name,
        String source,
        String country,
        String genre,
        List<ChartEntry> entries
) {}
