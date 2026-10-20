package uk.gov.dvla.siftconfig.dto;

import java.util.List;
import java.util.Set;

/** Read-only vocabulary published by the config-options endpoint. */
public record ConfigOptionsResponse(
        List<String> delimiters,
        List<String> headerOptions,
        Set<String> formats,
        Set<String> modes,
        Set<String> schemaModes,
        double maxCorruptPercentDefault) {
}
