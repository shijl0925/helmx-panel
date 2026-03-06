package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VolumeHelperTest {

    @Test
    void settersAndGetters_workCorrectly() {
        VolumeHelper vh = new VolumeHelper();
        vh.setHostPath("/data/nginx");
        vh.setContainerPath("/usr/share/nginx/html");
        vh.setMode("rw");

        assertEquals("/data/nginx", vh.getHostPath());
        assertEquals("/usr/share/nginx/html", vh.getContainerPath());
        assertEquals("rw", vh.getMode());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        VolumeHelper vh = new VolumeHelper();
        assertNull(vh.getHostPath());
        assertNull(vh.getContainerPath());
        assertNull(vh.getMode());
    }

    @Test
    void equals_sameFields_areEqual() {
        VolumeHelper v1 = new VolumeHelper();
        v1.setHostPath("/a");
        v1.setContainerPath("/b");
        v1.setMode("ro");

        VolumeHelper v2 = new VolumeHelper();
        v2.setHostPath("/a");
        v2.setContainerPath("/b");
        v2.setMode("ro");

        assertEquals(v1, v2);
    }

    @Test
    void equals_differentMode_notEqual() {
        VolumeHelper v1 = new VolumeHelper();
        v1.setMode("rw");

        VolumeHelper v2 = new VolumeHelper();
        v2.setMode("ro");

        assertNotEquals(v1, v2);
    }
}
