package edu.sjsu.cmpe272.issuesgateway.controller;

/*
 * Author: Ranadhir
 * Contribution: Health check endpoint for container and CI probes.
 */

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final Instant startedAt = Instant.now();

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        return Map.of(
                "status", "ok",
                "uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds()
        );
    }
}
