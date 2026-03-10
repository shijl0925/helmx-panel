package com.helmx.tutorial.configuration;

import com.helmx.tutorial.dto.Result;
import com.helmx.tutorial.utils.ResponseUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Locale;

@ControllerAdvice
public class ValidationExceptionHandler {

    private static final String GENERIC_DUPLICATE_MESSAGE = "A value that must be unique is already in use.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder errorMessage = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errorMessage.append(error.getDefaultMessage()).append("; ");
        });
        return ResponseUtil.failed(400, null, errorMessage.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseUtil.failed(400, null, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause != null && cause.getMessage() != null ? cause.getMessage() : ex.getMessage();
        String message = mapDuplicateValueMessage(detail);
        return ResponseUtil.failed(400, null, message);
    }

    private String mapDuplicateValueMessage(String detail) {
        if (detail == null) {
            return GENERIC_DUPLICATE_MESSAGE;
        }
        String normalized = detail.toLowerCase(Locale.ROOT);
        if (normalized.contains("email")) {
            return "Error: Email is already in use!";
        }
        if (normalized.contains("username")
                || (normalized.contains("tb_users") && hasUniqueConstraintIndicators(normalized))) {
            return "Error: Username is already taken!";
        }
        return GENERIC_DUPLICATE_MESSAGE;
    }

    private boolean hasUniqueConstraintIndicators(String normalized) {
        return normalized.contains("duplicate")
                || normalized.contains("unique")
                || normalized.contains("constraint")
                || normalized.contains("index");
    }
}
