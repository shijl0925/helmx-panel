package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteUtilsTest {

    @Test
    void formatBytes_lessThan1024_returnsBytes() {
        assertEquals("0 B", ByteUtils.formatBytes(0));
        assertEquals("512 B", ByteUtils.formatBytes(512));
        assertEquals("1023 B", ByteUtils.formatBytes(1023));
    }

    @Test
    void formatBytes_kilobytes_returnsKB() {
        assertEquals("1.00 KB", ByteUtils.formatBytes(1024));
        assertEquals("1.50 KB", ByteUtils.formatBytes(1536));
    }

    @Test
    void formatBytes_megabytes_returnsMB() {
        assertEquals("1.00 MB", ByteUtils.formatBytes(1024 * 1024));
        assertEquals("2.00 MB", ByteUtils.formatBytes(2 * 1024 * 1024));
    }

    @Test
    void formatBytes_gigabytes_returnsGB() {
        assertEquals("1.00 GB", ByteUtils.formatBytes(1024L * 1024 * 1024));
    }

    @Test
    void formatBytes_terabytes_returnsTB() {
        assertEquals("1.00 TB", ByteUtils.formatBytes(1024L * 1024 * 1024 * 1024));
    }
}
