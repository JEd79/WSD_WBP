package uk.gov.dvla.siftconfig.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating or replacing a write config.
 *
 * <p>Structural checks (required fields) are Bean Validation (HTTP 400); vocabulary checks
 * (format/mode/schemaMode) and conditional rules (merge requires mergeKeys + delta) are
 * applied in the service layer and yield HTTP 422.</p>
 */
public record WriteConfigRequest(
        @NotBlank(message = "targetTable is required")
        String targetTable,

        @NotBlank(message = "format is required")
        String format,

        @NotBlank(message = "mode is required")
        String mode,

        @NotBlank(message = "schemaMode is required")
        String schemaMode,

        List<String> partitionBy,

        List<String> mergeKeys,

        Boolean addSourceMetadata) {
}
