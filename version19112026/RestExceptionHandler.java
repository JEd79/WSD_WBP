package uk.gov.dvla.sifftconfig.controllers;

import uk.gov.dvla.sifftconfig.exceptions.InvalidConfigException;
import uk.gov.dvla.sifftconfig.exceptions.DuplicateConfigException;
import uk.gov.dvla.sifftconfig.exceptions.ConfigNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {DuplicateConfigException.class, InvalidConfigException.class})
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(e.getClass().getSimpleName()+": "+e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(ConfigNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getClass().getSimpleName()+": "+e.getMessage());
    }
}
