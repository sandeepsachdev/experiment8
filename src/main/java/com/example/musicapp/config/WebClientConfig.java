package com.example.musicapp.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient deezerClient() {
        return buildClient("https://api.deezer.com");
    }

    @Bean
    public WebClient itunesClient() {
        return buildClient("https://itunes.apple.com");
    }

    @Bean
    public WebClient appleRssClient() {
        return buildClient("https://rss.applemarketingtools.com");
    }

    private WebClient buildClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(15));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .defaultHeader("User-Agent", "MusicDiscoveryApp/1.0")
                .build();
    }
}
