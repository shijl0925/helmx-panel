package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortHelperTest {

    @Test
    void settersAndGetters_workCorrectly() {
        PortHelper ph = new PortHelper();
        ph.setHostIP("0.0.0.0");
        ph.setHostPort("8080");
        ph.setContainerPort("80");
        ph.setProtocol("tcp");

        assertEquals("0.0.0.0", ph.getHostIP());
        assertEquals("8080", ph.getHostPort());
        assertEquals("80", ph.getContainerPort());
        assertEquals("tcp", ph.getProtocol());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        PortHelper ph = new PortHelper();
        assertNull(ph.getHostIP());
        assertNull(ph.getHostPort());
        assertNull(ph.getContainerPort());
        assertNull(ph.getProtocol());
    }

    @Test
    void equals_sameFields_areEqual() {
        PortHelper p1 = new PortHelper();
        p1.setHostPort("80");
        p1.setContainerPort("80");
        p1.setProtocol("tcp");

        PortHelper p2 = new PortHelper();
        p2.setHostPort("80");
        p2.setContainerPort("80");
        p2.setProtocol("tcp");

        assertEquals(p1, p2);
    }

    @Test
    void equals_differentProtocol_notEqual() {
        PortHelper p1 = new PortHelper();
        p1.setProtocol("tcp");

        PortHelper p2 = new PortHelper();
        p2.setProtocol("udp");

        assertNotEquals(p1, p2);
    }
}
