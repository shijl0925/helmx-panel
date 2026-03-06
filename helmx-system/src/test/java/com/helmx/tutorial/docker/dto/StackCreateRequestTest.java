package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        StackCreateRequest req = new StackCreateRequest();
        req.setName("my-stack");
        req.setContent("version: '3'\nservices:\n  web:\n    image: nginx");

        assertEquals("my-stack", req.getName());
        assertEquals("version: '3'\nservices:\n  web:\n    image: nginx", req.getContent());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        StackCreateRequest req = new StackCreateRequest();
        assertNull(req.getName());
        assertNull(req.getContent());
    }

    @Test
    void equals_sameFields_areEqual() {
        StackCreateRequest r1 = new StackCreateRequest();
        r1.setName("s1");
        r1.setContent("content");

        StackCreateRequest r2 = new StackCreateRequest();
        r2.setName("s1");
        r2.setContent("content");

        assertEquals(r1, r2);
    }
}
