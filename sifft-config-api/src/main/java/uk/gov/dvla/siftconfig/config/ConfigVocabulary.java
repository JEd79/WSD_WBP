package uk.gov.dvla.siftconfig.config;

import java.util.List;
import java.util.Set;

/**
 * The controlled vocabulary for ingestion configuration.
 *
 * <p>These values mirror the constants defined in SIFFT's own configuration modules
 * ({@code file_processing/config.py} and {@code table_writing/config.py}). Keeping them
 * in one place gives the API a single source of truth and lets the config-options
 * endpoint publish exactly what the validation layer enforces.</p>
 */
public final class ConfigVocabulary {

    private ConfigVocabulary() {
    }

    /** Allowed single-character delimiters (see SIFFT CANDIDATE_DELIMITERS / EXTENSION_DEFAULTS). */
    public static final List<String> DELIMITERS = List.of(",", "|", "\t", ";");

    /** Allowed header options; "auto" triggers SIFFT header detection. */
    public static final List<String> HEADER_OPTIONS = List.of("true", "false", "auto");

    /** Allowed table formats (SIFFT FORMATS). */
    public static final Set<String> FORMATS = Set.of("delta", "parquet", "orc");

    /** Allowed write modes (SIFFT MODES). */
    public static final Set<String> MODES = Set.of("append", "overwrite", "merge", "errorIfExists");

    /** Allowed schema-evolution modes (SIFFT SCHEMA_MODES). */
    public static final Set<String> SCHEMA_MODES = Set.of("strict", "merge", "overwrite");

    /** Default maximum fraction of corrupt records tolerated (SIFFT MAX_CORRUPT_RECORDS_PERCENT). */
    public static final double MAX_CORRUPT_PERCENT_DEFAULT = 0.1;

    /** Format required for merge mode. */
    public static final String FORMAT_DELTA = "delta";

    /** Write mode that requires merge keys. */
    public static final String MODE_MERGE = "merge";
}
