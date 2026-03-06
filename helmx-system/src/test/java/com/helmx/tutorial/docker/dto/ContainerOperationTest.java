package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerOperationTest {

    @Test
    void defaultHost_isUnixSocket() {
        ContainerOperation op = new ContainerOperation();
        assertEquals("unix:///var/run/docker.sock", op.getHost());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        ContainerOperation op = new ContainerOperation();
        op.setHost("tcp://192.168.1.100:2376");
        op.setContainerId("abc123def456");
        op.setOperation("restart");

        assertEquals("tcp://192.168.1.100:2376", op.getHost());
        assertEquals("abc123def456", op.getContainerId());
        assertEquals("restart", op.getOperation());
    }

    @Test
    void validOperations_canBeSet() {
        for (String operation : new String[]{"start", "stop", "restart", "kill", "pause", "unpause", "remove"}) {
            ContainerOperation op = new ContainerOperation();
            op.setOperation(operation);
            assertEquals(operation, op.getOperation());
        }
    }

    @Test
    void containerIdField_defaultsToNull() {
        ContainerOperation op = new ContainerOperation();
        assertNull(op.getContainerId());
    }
}
