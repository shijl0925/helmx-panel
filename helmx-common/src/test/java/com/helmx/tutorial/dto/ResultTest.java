package com.helmx.tutorial.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void noArgConstructor_createsEmptyResult() {
        Result result = new Result();
        assertNull(result.getMessage());
        assertNull(result.getData());
        assertNull(result.getCode());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        Result result = new Result("success", "payload", 200);
        assertEquals("success", result.getMessage());
        assertEquals("payload", result.getData());
        assertEquals(200, result.getCode());
    }

    @Test
    void setters_updateFields() {
        Result result = new Result();
        result.setMessage("error");
        result.setData(42);
        result.setCode(500);

        assertEquals("error", result.getMessage());
        assertEquals(42, result.getData());
        assertEquals(500, result.getCode());
    }

    @Test
    void equals_sameValues_areEqual() {
        Result r1 = new Result("msg", "data", 200);
        Result r2 = new Result("msg", "data", 200);
        assertEquals(r1, r2);
    }
}
