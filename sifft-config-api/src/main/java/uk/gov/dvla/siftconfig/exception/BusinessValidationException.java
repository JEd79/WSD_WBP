package uk.gov.dvla.siftconfig.exception;

/**
 * Thrown by the service layer when a well-formed request violates a SIFFT business rule.
 *
 * <p>The {@code semantic} flag selects the HTTP status in the global handler:
 * <ul>
 *   <li>{@code false} &rarr; 400 Bad Request (structurally invalid, e.g. multi-char delimiter)</li>
 *   <li>{@code true}  &rarr; 422 Unprocessable Entity (value outside the allowed vocabulary)</li>
 * </ul>
 */
public class BusinessValidationException extends RuntimeException {

    private final String errorType;
    private final String details;
    private final boolean semantic;

    public BusinessValidationException(String errorType, String message, String details, boolean semantic) {
        super(message);
        this.errorType = errorType;
        this.details = details;
        this.semantic = semantic;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getDetails() {
        return details;
    }

    public boolean isSemantic() {
        return semantic;
    }
}
