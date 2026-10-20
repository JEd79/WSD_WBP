package uk.gov.dvla.siftconfig.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.dvla.siftconfig.config.ConfigVocabulary;
import uk.gov.dvla.siftconfig.dto.ConfigOptionsResponse;

/**
 * Read-only resource publishing the controlled vocabulary that read/write configs are
 * validated against. GET-only by design: the vocabulary is server-owned reference data.
 */
@RestController
@RequestMapping("/api/v1/config-options")
public class ConfigOptionsController {

    // C1 — return the allowed delimiters/formats/modes/schema-modes (200)
    @GetMapping
    public ConfigOptionsResponse getOptions() {
        return new ConfigOptionsResponse(
                ConfigVocabulary.DELIMITERS,
                ConfigVocabulary.HEADER_OPTIONS,
                ConfigVocabulary.FORMATS,
                ConfigVocabulary.MODES,
                ConfigVocabulary.SCHEMA_MODES,
                ConfigVocabulary.MAX_CORRUPT_PERCENT_DEFAULT);
    }
}
