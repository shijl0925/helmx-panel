package com.helmx.tutorial.configuration;

import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValidationExceptionHandlerTest {

    private final ValidationExceptionHandler handler = new ValidationExceptionHandler();

    @Test
    void handleIllegalArgumentException_returnsBadRequest() {
        ResponseEntity<Result> response = handler.handleIllegalArgumentException(new IllegalArgumentException("bad host"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getCode());
        assertEquals("bad host", response.getBody().getMessage());
    }
}
