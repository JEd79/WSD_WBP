package uk.gov.dvla.siftconfig.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal health endpoint used to verify the toolchain end-to-end before the
 * configuration resources are implemented.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "sifft-config-api",
                "timestamp", Instant.now().toString()
        );
    }
}
