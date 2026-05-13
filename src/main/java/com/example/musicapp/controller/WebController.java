package com.example.musicapp.controller;

import com.example.musicapp.model.Recommendations;
import com.example.musicapp.model.SearchResult;
import com.example.musicapp.service.ChartService;
import com.example.musicapp.service.MusicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final MusicService musicService;
    private final ChartService chartService;

    public WebController(MusicService musicService, ChartService chartService) {
        this.musicService = musicService;
        this.chartService = chartService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activePage", "home");
        return "home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q, Model model) {
        SearchResult result = (q == null || q.isBlank())
                ? new SearchResult(q, java.util.List.of(), java.util.List.of())
                : musicService.search(q);
        model.addAttribute("activePage", "search");
        model.addAttribute("query", q);
        model.addAttribute("result", result);
        return "search";
    }

    @GetMapping("/artist/{id}")
    public String artist(@PathVariable("id") long id, Model model) {
        Recommendations rec = musicService.recommendForArtist(id);
        model.addAttribute("activePage", "search");
        model.addAttribute("rec", rec);
        model.addAttribute("topTracks", musicService.getTopTracks(id, 10));
        model.addAttribute("seedKind", "artist");
        return "recommendations";
    }

    @GetMapping("/track/{id}")
    public String track(@PathVariable("id") long id, Model model) {
        Recommendations rec = musicService.recommendForTrack(id);
        model.addAttribute("activePage", "search");
        model.addAttribute("rec", rec);
        model.addAttribute("seedKind", "track");
        return "recommendations";
    }

    @GetMapping("/charts")
    public String charts(
            @RequestParam(value = "country", defaultValue = "us") String country,
            @RequestParam(value = "genre", defaultValue = "0") int genre,
            Model model) {
        model.addAttribute("activePage", "charts");
        model.addAttribute("appleChart", chartService.getAppleChart(country));
        model.addAttribute("deezerChart", chartService.getDeezerChart(genre));
        model.addAttribute("selectedCountry", country);
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("countries", ChartService.COUNTRIES);
        model.addAttribute("genres", ChartService.GENRES);
        return "charts";
    }
}
