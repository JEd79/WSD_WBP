package uk.gov.dvla.sifftconfig.exceptions;

public class DuplicateConfigException extends IllegalStateException {

    public DuplicateConfigException(String message) {
        super(message);
    }

    public DuplicateConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}