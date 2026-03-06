package com.helmx.tutorial.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThrowableUtilTest {

    @Test
    void getStackTrace_withException_returnsNonEmptyString() {
        RuntimeException ex = new RuntimeException("test error");
        String stackTrace = ThrowableUtil.getStackTrace(ex);
        assertNotNull(stackTrace);
        assertFalse(stackTrace.isBlank());
    }

    @Test
    void getStackTrace_containsExceptionClassAndMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("invalid argument");
        String stackTrace = ThrowableUtil.getStackTrace(ex);
        assertTrue(stackTrace.contains("IllegalArgumentException"));
        assertTrue(stackTrace.contains("invalid argument"));
    }

    @Test
    void getStackTrace_containsCallerClassName() {
        RuntimeException ex = new RuntimeException("sample");
        String stackTrace = ThrowableUtil.getStackTrace(ex);
        assertTrue(stackTrace.contains("ThrowableUtilTest"));
    }
}
