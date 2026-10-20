package uk.gov.dvla.siftconfig.dto;

import java.util.List;

import uk.gov.dvla.siftconfig.model.WriteConfig;

/** Response body representing a stored write config. */
public record WriteConfigResponse(
        String id,
        String targetTable,
        String format,
        String mode,
        String schemaMode,
        List<String> partitionBy,
        List<String> mergeKeys,
        boolean addSourceMetadata) {

    public static WriteConfigResponse fromEntity(WriteConfig e) {
        return new WriteConfigResponse(
                e.getId(),
                e.getTargetTable(),
                e.getFormat(),
                e.getMode(),
                e.getSchemaMode(),
                List.copyOf(e.getPartitionBy()),
                List.copyOf(e.getMergeKeys()),
                e.isAddSourceMetadata());
    }
}
