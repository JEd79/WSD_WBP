package uk.gov.dvla.siftconfig.dto;

import java.util.List;

/**
 * Result of applying a stored read config's delimiter to a sample line (R6).
 * Demonstrates real server-side processing: the service splits the line and compares
 * the detected field count against the configured columns.
 */
public record ConfigTestResponse(
        String id,
        String delimiter,
        int detectedFieldCount,
        int expectedFieldCount,
        boolean matches,
        List<String> detectedFields) {
}
