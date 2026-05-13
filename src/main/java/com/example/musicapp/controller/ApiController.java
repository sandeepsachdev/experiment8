package com.example.musicapp.controller;

import com.example.musicapp.model.Chart;
import com.example.musicapp.model.Recommendations;
import com.example.musicapp.model.SearchResult;
import com.example.musicapp.service.ChartService;
import com.example.musicapp.service.MusicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final MusicService musicService;
    private final ChartService chartService;

    public ApiController(MusicService musicService, ChartService chartService) {
        this.musicService = musicService;
        this.chartService = chartService;
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam("q") String q) {
        return musicService.search(q);
    }

    @GetMapping("/recommendations/artist/{id}")
    public Recommendations forArtist(@PathVariable("id") long id) {
        return musicService.recommendForArtist(id);
    }

    @GetMapping("/recommendations/track/{id}")
    public Recommendations forTrack(@PathVariable("id") long id) {
        return musicService.recommendForTrack(id);
    }

    @GetMapping("/charts/apple")
    public Chart appleChart(@RequestParam(value = "country", defaultValue = "us") String country) {
        return chartService.getAppleChart(country);
    }

    @GetMapping("/charts/deezer")
    public Chart deezerChart(@RequestParam(value = "genre", defaultValue = "0") int genre) {
        return chartService.getDeezerChart(genre);
    }
}
