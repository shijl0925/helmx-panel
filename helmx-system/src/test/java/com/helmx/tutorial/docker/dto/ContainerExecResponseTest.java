package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerExecResponseTest {

    @Test
    void settersAndGetters_workCorrectly() {
        ContainerExecResponse resp = new ContainerExecResponse();
        resp.setExecId("exec-123");
        resp.setOutput("Hello World\n");
        resp.setStatus("success");
        resp.setError(null);

        assertEquals("exec-123", resp.getExecId());
        assertEquals("Hello World\n", resp.getOutput());
        assertEquals("success", resp.getStatus());
        assertNull(resp.getError());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        ContainerExecResponse resp = new ContainerExecResponse();
        assertNull(resp.getExecId());
        assertNull(resp.getOutput());
        assertNull(resp.getStatus());
        assertNull(resp.getError());
    }

    @Test
    void errorField_canBeSet() {
        ContainerExecResponse resp = new ContainerExecResponse();
        resp.setError("command not found");
        resp.setStatus("failed");
        assertEquals("command not found", resp.getError());
        assertEquals("failed", resp.getStatus());
    }

    @Test
    void equals_sameFields_areEqual() {
        ContainerExecResponse r1 = new ContainerExecResponse();
        r1.setExecId("e1");
        r1.setOutput("out");

        ContainerExecResponse r2 = new ContainerExecResponse();
        r2.setExecId("e1");
        r2.setOutput("out");

        assertEquals(r1, r2);
    }
}
