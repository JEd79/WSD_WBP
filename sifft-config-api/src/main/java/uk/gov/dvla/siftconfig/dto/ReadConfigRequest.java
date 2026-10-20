package uk.gov.dvla.siftconfig.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request body for creating or replacing a read config.
 *
 * <p>Bean Validation handles structural checks (missing/blank fields) and yields HTTP 400.
 * Deeper SIFFT rules (single-char delimiter, header vocabulary, regex validity) are applied
 * in the service layer.</p>
 */
public record ReadConfigRequest(
        @NotBlank(message = "pattern is required")
        String pattern,

        @NotBlank(message = "delimiter is required")
        String delimiter,

        @NotBlank(message = "header is required")
        String header,

        @NotEmpty(message = "columns must contain at least one entry")
        List<@NotBlank(message = "column names must not be blank") String> columns,

        @DecimalMin(value = "0.0", message = "maxCorruptPercent must be >= 0.0")
        @DecimalMax(value = "1.0", message = "maxCorruptPercent must be <= 1.0")
        Double maxCorruptPercent,

        String writeConfigId) {
}
