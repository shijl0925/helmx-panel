package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteHostMetricsCollectorTest {

    private final RemoteHostMetricsCollector collector = new RemoteHostMetricsCollector();

    @Test
    void parseMetricsOutput_mapsCpuMemoryAndDiskValues() {
        Map<String, Object> metrics = collector.parseMetricsOutput("""
                cpu=12.34
                mem=2147483648 8589934592 25.00
                disk=10737418240 21474836480 50.00
                """);

        assertTrue((Boolean) metrics.get("hostMetricsAvailable"));
        assertEquals(12.34D, metrics.get("hostCpuUsage"));
        assertEquals(25.0D, metrics.get("hostMemoryUsage"));
        assertEquals("2.00 GB", metrics.get("hostMemoryUsed"));
        assertEquals("8.00 GB", metrics.get("hostMemoryTotal"));
        assertEquals(50.0D, metrics.get("hostDiskUsage"));
        assertEquals("10.00 GB", metrics.get("hostDiskUsed"));
        assertEquals("20.00 GB", metrics.get("hostDiskTotal"));
    }

    @Test
    void parseMetricsOutput_blankOutput_returnsUnavailableDefaults() {
        Map<String, Object> metrics = collector.parseMetricsOutput(" ");

        assertFalse((Boolean) metrics.get("hostMetricsAvailable"));
        assertEquals(0D, metrics.get("hostCpuUsage"));
        assertEquals("0B", metrics.get("hostMemoryTotal"));
        assertEquals("0B", metrics.get("hostDiskTotal"));
    }
}
