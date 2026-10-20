package uk.gov.dvla.siftconfig.dto;

import java.util.List;

import uk.gov.dvla.siftconfig.model.ReadConfig;

/** Response body representing a stored read config. */
public record ReadConfigResponse(
        String id,
        String pattern,
        String delimiter,
        String header,
        List<String> columns,
        double maxCorruptPercent,
        String writeConfigId) {

    public static ReadConfigResponse fromEntity(ReadConfig e) {
        return new ReadConfigResponse(
                e.getId(),
                e.getPattern(),
                e.getDelimiter(),
                e.getHeader(),
                List.copyOf(e.getColumns()),
                e.getMaxCorruptPercent(),
                e.getWriteConfigId());
    }
}
