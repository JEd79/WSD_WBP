package uk.gov.dvla.siftconfig.exception;

/** Thrown when creating a resource would duplicate a unique field (e.g. pattern). Maps to HTTP 409. */
public class ResourceConflictException extends RuntimeException {

    private final String errorType;
    private final String details;

    public ResourceConflictException(String errorType, String message, String details) {
        super(message);
        this.errorType = errorType;
        this.details = details;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getDetails() {
        return details;
    }
}
