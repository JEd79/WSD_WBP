package uk.gov.dvla.siftconfig.exception;

import java.time.Instant;

/**
 * Uniform error representation returned by every endpoint.
 *
 * <p>The {@code errorType / message / details} triad mirrors SIFFT's own
 * {@code Exception.to_dict()} shape, so the API is faithful to the system it fronts.</p>
 */
public record ApiError(
        String timestamp,
        int status,
        String errorType,
        String message,
        String details,
        String path) {

    public static ApiError of(int status, String errorType, String message, String details, String path) {
        return new ApiError(Instant.now().toString(), status, errorType, message, details, path);
    }
}
