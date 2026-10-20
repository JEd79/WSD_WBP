package uk.gov.dvla.siftconfig.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/** Request body for the config-test endpoint (R6): a sample data line to parse. */
public record ConfigTestRequest(
        @NotNull(message = "sampleLine is required")
        String sampleLine) {
}
