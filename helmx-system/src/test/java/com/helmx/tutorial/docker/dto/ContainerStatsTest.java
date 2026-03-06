package com.helmx.tutorial.docker.dto;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerStatsTest {

    /**
     * Build a stats JSON matching the Docker API shape:
     * cpu_stats.cpu_usage.total_usage, cpu_stats.system_cpu_usage, cpu_stats.online_cpus
     * precpu_stats.cpu_usage.total_usage, precpu_stats.system_cpu_usage
     * memory_stats.usage, memory_stats.limit, memory_stats.stats.cache
     */
    private JSONObject buildStatsJson(
            long totalUsage, long preTotalUsage,
            long systemUsage, long preSystemUsage,
            int onlineCpus,
            long memUsage, long memLimit,
            int cache) {

        JSONObject cpuUsage = new JSONObject();
        cpuUsage.put("total_usage", totalUsage);

        JSONObject cpuStats = new JSONObject();
        cpuStats.put("cpu_usage", cpuUsage);
        cpuStats.put("system_cpu_usage", systemUsage);
        cpuStats.put("online_cpus", onlineCpus);

        JSONObject preCpuUsage = new JSONObject();
        preCpuUsage.put("total_usage", preTotalUsage);

        JSONObject preCpuStats = new JSONObject();
        preCpuStats.put("cpu_usage", preCpuUsage);
        preCpuStats.put("system_cpu_usage", preSystemUsage);

        JSONObject memStatsDetail = new JSONObject();
        memStatsDetail.put("cache", cache);

        JSONObject memoryStats = new JSONObject();
        memoryStats.put("usage", memUsage);
        memoryStats.put("limit", memLimit);
        memoryStats.put("stats", memStatsDetail);

        JSONObject stats = new JSONObject();
        stats.put("cpu_stats", cpuStats);
        stats.put("precpu_stats", preCpuStats);
        stats.put("memory_stats", memoryStats);

        return stats;
    }

    @Test
    void cpuPercent_calculatedCorrectly() {
        // totalDelta=100_000_000, systemDelta=500_000_000, 4 CPUs
        // cpuPercent = (100_000_000 / 500_000_000) * 4 * 100 = 80%
        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                4,
                104_857_600L, 8_589_934_592L, 52_428_800);

        ContainerStats stats = new ContainerStats(json);

        assertEquals(80.0f, stats.getCpuPercent(), 0.01f);
        assertEquals(4, stats.getOnlineCPUs());
    }

    @Test
    void cpuPercent_zeroSystemDelta_returnsZero() {
        // system delta = 0 → cpuPercent should be 0 (no division by zero)
        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 1_000_000_000L,   // systemDelta = 0
                4,
                104_857_600L, 8_589_934_592L, 0);

        ContainerStats stats = new ContainerStats(json);

        assertEquals(0.0f, stats.getCpuPercent(), 0.001f);
    }

    @Test
    void memoryPercent_calculatedCorrectly() {
        // 104 857 600 / 8 589 934 592 * 100 ≈ 1.22%
        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                2,
                104_857_600L, 8_589_934_592L, 0);

        ContainerStats stats = new ContainerStats(json);

        float expected = 104_857_600f * 100f / 8_589_934_592f;
        assertEquals(expected, stats.getMemoryPercent(), 0.01f);
    }

    @Test
    void memoryPercent_zeroLimit_returnsZero() {
        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                2,
                104_857_600L, 0L, 0);   // limit = 0

        ContainerStats stats = new ContainerStats(json);

        assertEquals(0.0f, stats.getMemoryPercent(), 0.001f);
    }

    @Test
    void memoryFields_formattedWithByteUtils() {
        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                2,
                1024L * 1024L, 1024L * 1024L * 1024L, 0);

        ContainerStats stats = new ContainerStats(json);

        assertEquals("1.00 MB", stats.getMemoryUsage());
        assertEquals("1.00 GB", stats.getMemoryLimit());
    }

    @Test
    void cpuTotalUsage_formattedWithTimeUtils() {
        // totalDelta = 500_000_000 ns = 500 ms
        JSONObject json = buildStatsJson(
                600_000_000L, 100_000_000L,
                2_000_000_000L, 1_000_000_000L,
                2,
                0L, 1L, 0);

        ContainerStats stats = new ContainerStats(json);

        assertEquals("500ms", stats.getCpuTotalUsage());
    }

    @Test
    void memoryCache_parsedCorrectly() {
        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                4,
                104_857_600L, 8_589_934_592L, 52_428_800);

        ContainerStats stats = new ContainerStats(json);

        assertEquals(52_428_800, stats.getMemoryCache());
    }

    @Test
    void missingCpuStats_doesNotThrow() {
        // Partial JSON missing cpu_stats should not throw, just skip
        JSONObject json = new JSONObject();
        json.put("cpu_stats", (Object) null);
        json.put("precpu_stats", (Object) null);
        json.put("memory_stats", (Object) null);

        assertDoesNotThrow(() -> new ContainerStats(json));
    }
}
