package com.helmx.tutorial.docker.dto;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerStatsTest {

    /**
     * Build a stats JSON matching the Docker API shape:
     * cpu_stats.cpu_usage.total_usage, cpu_stats.system_cpu_usage, cpu_stats.online_cpus
     * precpu_stats.cpu_usage.total_usage, precpu_stats.system_cpu_usage
     * memory_stats.usage, memory_stats.limit, memory_stats.stats.cache, memory_stats.stats.inactive_file
     */
    private JSONObject buildStatsJson(
            long totalUsage, long preTotalUsage,
            long systemUsage, long preSystemUsage,
            int onlineCpus,
            long memUsage, long memLimit,
            long cache, long inactiveFile) {

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
        if (inactiveFile > 0) {
            memStatsDetail.put("inactive_file", inactiveFile);
        }

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

    /** Convenience overload with no cache/inactive_file (both zero). */
    private JSONObject buildStatsJson(
            long totalUsage, long preTotalUsage,
            long systemUsage, long preSystemUsage,
            int onlineCpus,
            long memUsage, long memLimit,
            long cache) {
        return buildStatsJson(totalUsage, preTotalUsage, systemUsage, preSystemUsage,
                onlineCpus, memUsage, memLimit, cache, 0L);
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
        // usage=104_857_600, cache=0 → actual=104_857_600
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

        assertEquals(52_428_800L, stats.getMemoryCache());
    }

    @Test
    void memoryUsage_subtractsCacheV1() {
        // cgroup v1: memory_stats.stats.cache should be subtracted from usage
        // usage=200MB, cache=50MB → actual=150MB
        long usage = 200L * 1024 * 1024;
        long cache = 50L * 1024 * 1024;
        long limit = 1024L * 1024 * 1024;
        long actual = usage - cache;

        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                2,
                usage, limit, cache);

        ContainerStats stats = new ContainerStats(json);

        float expectedPercent = actual * 100.0f / limit;
        assertEquals(expectedPercent, stats.getMemoryPercent(), 0.01f);
        assertEquals(cache, stats.getMemoryCache());
    }

    @Test
    void memoryUsage_subtractsInactiveFileV2() {
        // cgroup v2: memory_stats.stats.inactive_file takes priority over cache
        // usage=300MB, inactive_file=80MB, cache=10MB → actual=300MB-80MB=220MB
        long usage = 300L * 1024 * 1024;
        long inactiveFile = 80L * 1024 * 1024;
        long cache = 10L * 1024 * 1024;
        long limit = 1024L * 1024 * 1024;
        long actual = usage - inactiveFile;

        JSONObject json = buildStatsJson(
                200_000_000L, 100_000_000L,
                1_000_000_000L, 500_000_000L,
                2,
                usage, limit, cache, inactiveFile);

        ContainerStats stats = new ContainerStats(json);

        float expectedPercent = actual * 100.0f / limit;
        assertEquals(expectedPercent, stats.getMemoryPercent(), 0.01f);
        // memoryCache records the effective cache bytes used (inactive_file)
        assertEquals(inactiveFile, stats.getMemoryCache());
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
