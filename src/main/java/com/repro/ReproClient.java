package com.repro;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

public class ReproClient {

    private final WebClient webClient;

    public ReproClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getRetry() {
        return webClient.get()
                .uri("https://postman-echo.com/status/404")
                .exchange()
                .flatMap(cr -> {
                    if (!cr.statusCode().is2xxSuccessful()) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Didn't get a 200, retrying...");
                    }
                    else return cr.bodyToMono(String.class);
                })
                .retry(2);
    }
}
