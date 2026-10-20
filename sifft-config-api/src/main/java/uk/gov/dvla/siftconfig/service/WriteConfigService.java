package uk.gov.dvla.siftconfig.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import uk.gov.dvla.siftconfig.config.ConfigVocabulary;
import uk.gov.dvla.siftconfig.dto.WriteConfigRequest;
import uk.gov.dvla.siftconfig.exception.BusinessValidationException;
import uk.gov.dvla.siftconfig.exception.ResourceNotFoundException;
import uk.gov.dvla.siftconfig.model.WriteConfig;
import uk.gov.dvla.siftconfig.repository.WriteConfigRepository;

/**
 * Business logic and validation for write configs, mirroring SIFFT's
 * {@code table_writing/validation.py} rules.
 */
@Service
public class WriteConfigService {

    private static final Pattern IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_.]+$");
    private static final int MAX_IDENTIFIER_LENGTH = 255;

    private final WriteConfigRepository repository;

    public WriteConfigService(WriteConfigRepository repository) {
        this.repository = repository;
    }

    public List<WriteConfig> findAll() {
        return repository.findAll();
    }

    public WriteConfig findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "write_config_not_found", "No write config with id " + id));
    }

    public WriteConfig create(WriteConfigRequest request) {
        validate(request);
        WriteConfig entity = new WriteConfig();
        entity.setId(UUID.randomUUID().toString());
        apply(entity, request);
        return repository.save(entity);
    }

    public WriteConfig replace(String id, WriteConfigRequest request) {
        WriteConfig entity = findById(id);
        validate(request);
        apply(entity, request);
        return repository.save(entity);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "write_config_not_found", "No write config with id " + id);
        }
        repository.deleteById(id);
    }

    private void apply(WriteConfig entity, WriteConfigRequest request) {
        entity.setTargetTable(request.targetTable());
        entity.setFormat(request.format());
        entity.setMode(request.mode());
        entity.setSchemaMode(request.schemaMode());
        entity.setPartitionBy(request.partitionBy() == null
                ? new ArrayList<>() : new ArrayList<>(request.partitionBy()));
        entity.setMergeKeys(request.mergeKeys() == null
                ? new ArrayList<>() : new ArrayList<>(request.mergeKeys()));
        entity.setAddSourceMetadata(Boolean.TRUE.equals(request.addSourceMetadata()));
    }

    /**
     * Semantic validation (HTTP 422 for vocabulary/rule violations; 400 for a structurally
     * invalid identifier).
     */
    private void validate(WriteConfigRequest r) {
        // Identifier rule — structural (400). Mirrors _validate_identifier.
        if (r.targetTable().length() > MAX_IDENTIFIER_LENGTH) {
            throw new BusinessValidationException("invalid_input",
                    "targetTable exceeds maximum length of " + MAX_IDENTIFIER_LENGTH,
                    "Length: " + r.targetTable().length(), false);
        }
        if (!IDENTIFIER.matcher(r.targetTable()).matches()) {
            throw new BusinessValidationException("invalid_input",
                    "targetTable contains invalid characters",
                    "Only alphanumeric, underscore, and dot allowed", false);
        }

        // Vocabulary rules — semantic (422).
        if (!ConfigVocabulary.FORMATS.contains(r.format())) {
            throw new BusinessValidationException("invalid_format",
                    "Invalid format: " + r.format(),
                    "format must be one of: " + ConfigVocabulary.FORMATS, true);
        }
        if (!ConfigVocabulary.MODES.contains(r.mode())) {
            throw new BusinessValidationException("invalid_mode",
                    "Invalid mode: " + r.mode(),
                    "mode must be one of: " + ConfigVocabulary.MODES, true);
        }
        if (!ConfigVocabulary.SCHEMA_MODES.contains(r.schemaMode())) {
            throw new BusinessValidationException("invalid_schema_mode",
                    "Invalid schemaMode: " + r.schemaMode(),
                    "schemaMode must be one of: " + ConfigVocabulary.SCHEMA_MODES, true);
        }

        // Conditional merge rule — semantic (422). Mirrors _validate_merge_keys.
        if (ConfigVocabulary.MODE_MERGE.equals(r.mode())) {
            if (!ConfigVocabulary.FORMAT_DELTA.equals(r.format())) {
                throw new BusinessValidationException("invalid_mode",
                        "Merge mode only supported for Delta format",
                        "Current format: " + r.format(), true);
            }
            if (r.mergeKeys() == null || r.mergeKeys().isEmpty()) {
                throw new BusinessValidationException("merge_key_missing",
                        "mergeKeys required for merge mode",
                        "Specify at least one merge key", true);
            }
        }

        // Partition / merge key identifiers — structural (400).
        validateIdentifierList(r.partitionBy(), "partition column");
        validateIdentifierList(r.mergeKeys(), "merge key");
    }

    private void validateIdentifierList(List<String> values, String field) {
        if (values == null) {
            return;
        }
        for (String v : values) {
            if (v == null || !IDENTIFIER.matcher(v).matches()) {
                throw new BusinessValidationException("invalid_input",
                        field + " contains invalid characters",
                        "Only alphanumeric, underscore, and dot allowed. Invalid: " + v, false);
            }
        }
    }
}
