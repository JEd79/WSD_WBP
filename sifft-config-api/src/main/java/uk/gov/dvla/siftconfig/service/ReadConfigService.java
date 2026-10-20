package uk.gov.dvla.siftconfig.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;

import uk.gov.dvla.siftconfig.config.ConfigVocabulary;
import uk.gov.dvla.siftconfig.dto.ConfigTestResponse;
import uk.gov.dvla.siftconfig.dto.ReadConfigRequest;
import uk.gov.dvla.siftconfig.exception.BusinessValidationException;
import uk.gov.dvla.siftconfig.exception.ResourceConflictException;
import uk.gov.dvla.siftconfig.exception.ResourceNotFoundException;
import uk.gov.dvla.siftconfig.model.ReadConfig;
import uk.gov.dvla.siftconfig.repository.ReadConfigRepository;
import uk.gov.dvla.siftconfig.repository.WriteConfigRepository;

/**
 * Business logic and validation for read configs, mirroring SIFFT's delimited-file rules
 * (single-character delimiter, header vocabulary) plus referential integrity with
 * write configs.
 */
@Service
public class ReadConfigService {

    private static final Pattern IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_.]+$");

    private final ReadConfigRepository repository;
    private final WriteConfigRepository writeConfigRepository;

    public ReadConfigService(ReadConfigRepository repository,
                             WriteConfigRepository writeConfigRepository) {
        this.repository = repository;
        this.writeConfigRepository = writeConfigRepository;
    }

    public List<ReadConfig> findAll(String patternFilter) {
        if (patternFilter == null || patternFilter.isBlank()) {
            return repository.findAll();
        }
        return repository.findByPattern(patternFilter).map(List::of).orElseGet(List::of);
    }

    public ReadConfig findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "read_config_not_found", "No read config with id " + id));
    }

    public ReadConfig create(ReadConfigRequest request) {
        validate(request);
        if (repository.existsByPattern(request.pattern())) {
            throw new ResourceConflictException("duplicate_pattern",
                    "A read config with this pattern already exists",
                    "pattern: " + request.pattern());
        }
        ReadConfig entity = new ReadConfig();
        entity.setId(UUID.randomUUID().toString());
        apply(entity, request);
        return repository.save(entity);
    }

    public ReadConfig replace(String id, ReadConfigRequest request) {
        ReadConfig entity = findById(id);
        validate(request);
        // Allow keeping the same pattern; reject colliding with a different resource.
        repository.findByPattern(request.pattern()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ResourceConflictException("duplicate_pattern",
                        "Another read config already uses this pattern",
                        "pattern: " + request.pattern());
            }
        });
        apply(entity, request);
        return repository.save(entity);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "read_config_not_found", "No read config with id " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Server-side processing (R6): apply the stored delimiter to a sample line and report
     * whether the detected field count matches the configured columns.
     */
    public ConfigTestResponse test(String id, String sampleLine) {
        ReadConfig entity = findById(id);
        String delimiter = entity.getDelimiter();
        // Split on the literal delimiter; -1 keeps trailing empty fields.
        String[] fields = sampleLine.split(Pattern.quote(delimiter), -1);
        int detected = fields.length;
        int expected = entity.getColumns().size();
        return new ConfigTestResponse(
                id,
                delimiter,
                detected,
                expected,
                detected == expected,
                List.of(fields));
    }

    private void apply(ReadConfig entity, ReadConfigRequest request) {
        entity.setPattern(request.pattern());
        entity.setDelimiter(request.delimiter());
        entity.setHeader(request.header());
        entity.setColumns(new ArrayList<>(request.columns()));
        entity.setMaxCorruptPercent(
                request.maxCorruptPercent() == null
                        ? ConfigVocabulary.MAX_CORRUPT_PERCENT_DEFAULT
                        : request.maxCorruptPercent());
        entity.setWriteConfigId(request.writeConfigId());
    }

    private void validate(ReadConfigRequest r) {
        // Single-character delimiter — structural (400). Mirrors delimited_files.py.
        if (r.delimiter().length() != 1) {
            throw new BusinessValidationException("invalid_delimiter",
                    "delimiter must be a single character",
                    "Received '" + r.delimiter() + "' (length " + r.delimiter().length()
                            + "). Spark supports single-character delimiters only.", false);
        }

        // Header vocabulary — structural (400).
        if (!ConfigVocabulary.HEADER_OPTIONS.contains(r.header())) {
            throw new BusinessValidationException("invalid_header",
                    "Invalid header value: " + r.header(),
                    "header must be one of: " + ConfigVocabulary.HEADER_OPTIONS, false);
        }

        // Column identifiers — structural (400).
        for (String c : r.columns()) {
            if (c == null || !IDENTIFIER.matcher(c).matches()) {
                throw new BusinessValidationException("invalid_input",
                        "column name contains invalid characters",
                        "Only alphanumeric, underscore, and dot allowed. Invalid: " + c, false);
            }
        }

        // Pattern must be a compilable regex — structural (400).
        try {
            Pattern.compile(r.pattern());
        } catch (PatternSyntaxException e) {
            throw new BusinessValidationException("invalid_pattern",
                    "pattern is not a valid regular expression",
                    e.getMessage(), false);
        }

        // Referential integrity — semantic (422): referenced write config must exist.
        if (r.writeConfigId() != null && !r.writeConfigId().isBlank()
                && !writeConfigRepository.existsById(r.writeConfigId())) {
            throw new BusinessValidationException("write_config_not_found",
                    "Referenced writeConfigId does not exist",
                    "writeConfigId: " + r.writeConfigId(), true);
        }
    }
}
