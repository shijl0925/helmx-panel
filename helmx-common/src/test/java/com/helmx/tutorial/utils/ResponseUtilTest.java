package com.helmx.tutorial.utils;

import com.helmx.tutorial.dto.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResponseUtilTest {

    @Test
    void success_returnsOkStatusAndSuccessBody() {
        ResponseEntity<Result> response = ResponseUtil.success("payload");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().getMessage());
        assertEquals("payload", response.getBody().getData());
        assertEquals(0, response.getBody().getCode());
    }

    @Test
    void failed_usesProvidedHttpStatus() {
        ResponseEntity<Result> response = ResponseUtil.failed(HttpStatus.BAD_REQUEST.value(), null, "bad request");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("bad request", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getCode());
    }
}
