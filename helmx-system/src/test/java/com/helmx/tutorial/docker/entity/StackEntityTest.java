package com.helmx.tutorial.docker.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackEntityTest {

    @Test
    void settersAndGetters_workCorrectly() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setName("my-app");
        stack.setContent("version: '3'\nservices:\n  web:\n    image: nginx");

        assertEquals(1L, stack.getId());
        assertEquals("my-app", stack.getName());
        assertEquals("version: '3'\nservices:\n  web:\n    image: nginx", stack.getContent());
    }

    @Test
    void contentField_canBeNull() {
        Stack stack = new Stack();
        assertNull(stack.getContent());
    }

    @Test
    void nameField_canBeUpdated() {
        Stack stack = new Stack();
        stack.setName("v1");
        stack.setName("v2");
        assertEquals("v2", stack.getName());
    }
}
