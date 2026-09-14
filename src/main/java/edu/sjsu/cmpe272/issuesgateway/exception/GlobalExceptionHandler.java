package edu.sjsu.cmpe272.issuesgateway.exception;

/*
 * Author: Sai Vineetha Tirumalla
 * Contribution: Central exception handling; GitHub and catch-all handlers added by Ranadhir Reddy Rikkala
 */

import edu.sjsu.cmpe272.issuesgateway.dto.ApiError;
import edu.sjsu.cmpe272.issuesgateway.github.GitHubApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> details = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        details.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ApiError error = new ApiError(
                "Request validation failed",
                400,
                OffsetDateTime.now(),
                details
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidBody(
            HttpMessageNotReadableException exception
    ) {
        ApiError error = new ApiError(
                "Invalid request body or state value",
                400,
                OffsetDateTime.now(),
                Map.of()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ApiError> handleGitHub(GitHubApiException ex) {
        ApiError error = new ApiError(
                ex.getMessage(),
                ex.getStatusCode(),
                OffsetDateTime.now(),
                Map.of()
        );

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.getStatusCode());

        if (ex.getRetryAfter() != null) {
            builder.header("Retry-After", ex.getRetryAfter());
        }

        return builder.body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        ApiError error = new ApiError(
                exception.getMessage(),
                400,
                OffsetDateTime.now(),
                Map.of()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        ApiError error = new ApiError(
                "Unexpected server error",
                500,
                OffsetDateTime.now(),
                Map.of()
        );

        return ResponseEntity.status(500).body(error);
    }
}
