package com.repro;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 *
 */
@RestController
@RequestMapping("/api/repro")
public class ReproController {

    private final ReproClient reproClient;

    public ReproController(ReproClient reproClient) {
        this.reproClient = reproClient;
    }

    @GetMapping()
    public Mono<ResponseEntity<?>> getRetryRequest() {

        Mono<String> getRetry = reproClient.getRetry();
        return getRetry.map(o -> ResponseEntity.ok().body(o));
    }
}
