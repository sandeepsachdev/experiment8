package com.example.musicapp.model;

public record Artist(
        Long id,
        String name,
        String pictureUrl,
        Integer nbFans,
        Integer nbAlbums,
        String link
) {}
