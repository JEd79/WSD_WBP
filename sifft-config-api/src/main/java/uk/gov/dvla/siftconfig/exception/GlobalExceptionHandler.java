package uk.gov.dvla.siftconfig.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

/**
 * Translates exceptions into the uniform {@link ApiError} JSON body with the correct HTTP
 * status, centralising error handling away from the controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static String path(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                404, ex.getErrorType(), ex.getMessage(), "", path(request)));
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ResourceConflictException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                409, ex.getErrorType(), ex.getMessage(), ex.getDetails(), path(request)));
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessValidationException ex, WebRequest request) {
        HttpStatus status = ex.isSemantic() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), ex.getErrorType(), ex.getMessage(), ex.getDetails(), path(request)));
    }

    /** Bean Validation failures on @Valid request bodies -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiError.of(
                400, "validation_error", "Request body failed validation", details, path(request)));
    }

    /** Malformed / unparseable JSON -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                400, "malformed_request", "Request body is malformed or unreadable",
                "Ensure the body is well-formed JSON", path(request)));
    }

    /** Catch-all -> 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                500, "internal_error", "An unexpected error occurred",
                ex.getClass().getSimpleName(), path(request)));
    }
}
