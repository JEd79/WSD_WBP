package uk.gov.dvla.siftconfig.exception;

/** Thrown when a config resource cannot be found by id. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    private final String errorType;

    public ResourceNotFoundException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
