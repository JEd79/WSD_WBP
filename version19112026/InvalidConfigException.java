package uk.gov.dvla.sifftconfig.exceptions;

public class InvalidConfigException extends IllegalArgumentException {

    public InvalidConfigException() {
    }

    public InvalidConfigException(String message) {
        super(message);
    }

    public InvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}