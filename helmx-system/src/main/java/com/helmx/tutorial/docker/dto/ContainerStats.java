package com.helmx.tutorial.docker.dto;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.helmx.tutorial.docker.utils.ByteUtils;
import com.helmx.tutorial.docker.utils.TimeUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ContainerStats {
    private String cpuTotalUsage;
    private String cpuSystemUsage;
    private Integer onlineCPUs;
    private Float cpuPercent;

    private String memoryUsage;
    private String memoryLimit;
    private Long memoryCache;
    private Float memoryPercent;

    public ContainerStats(JSONObject stats) {
        JSONObject cpuStats = stats.getJSONObject("cpu_stats");
        JSONObject preCpuStats = stats.getJSONObject("precpu_stats");
        JSONObject memoryStats = stats.getJSONObject("memory_stats");

        if (cpuStats == null || preCpuStats == null || memoryStats == null) {
            log.warn("部分监控数据缺失，stats: {}", stats.toJSONString());
            return;
        }

        JSONObject cpuUsage = cpuStats.getJSONObject("cpu_usage");
        JSONObject preCpuUsage = preCpuStats.getJSONObject("cpu_usage");

        if (cpuUsage == null || preCpuUsage == null) {
            log.warn("CPU 使用率数据缺失");
            return;
        }

        long totalUsage = cpuUsage.getLongValue("total_usage") - preCpuUsage.getLongValue("total_usage");
        this.cpuTotalUsage = TimeUtils.formatNanosecondsDetailed(totalUsage);

        long systemCpuUsage = cpuStats.getLongValue("system_cpu_usage") - preCpuStats.getLongValue("system_cpu_usage");
        this.cpuSystemUsage = TimeUtils.formatNanosecondsDetailed(systemCpuUsage);

        this.onlineCPUs = cpuStats.getInteger("online_cpus");
        if (this.onlineCPUs == null || this.onlineCPUs == 0) {
            // Older Docker daemons / some runtimes omit online_cpus.
            // Fall back to the length of percpu_usage, mirroring Docker CLI behaviour.
            JSONArray perCpuUsage = cpuUsage.getJSONArray("percpu_usage");
            this.onlineCPUs = (perCpuUsage != null && !perCpuUsage.isEmpty())
                    ? perCpuUsage.size() : 1;
        }
        if (systemCpuUsage != 0) {
            this.cpuPercent = ((float) totalUsage / systemCpuUsage) * this.onlineCPUs * 100.0f;
        } else {
            this.cpuPercent = 0.0f;
        }

        long rawMemoryUsage = memoryStats.getLongValue("usage");
        long memoryLimit = memoryStats.getLongValue("limit");

        // Subtract page cache from usage to get actual process memory consumption.
        // Docker stats API: memory_stats.usage includes page cache.
        // cgroup v2 reports inactive_file; cgroup v1 reports cache.
        long cacheBytes = 0L;
        JSONObject memoryStatsDetail = memoryStats.getJSONObject("stats");
        if (memoryStatsDetail != null) {
            // Prefer inactive_file (cgroup v2 primary metric, may also appear in v1) when
            // the key is present; fall back to cache (cgroup v1 standard field).
            if (memoryStatsDetail.containsKey("inactive_file")) {
                cacheBytes = memoryStatsDetail.getLongValue("inactive_file");
            } else if (memoryStatsDetail.containsKey("cache")) {
                cacheBytes = memoryStatsDetail.getLongValue("cache");
            }
            this.memoryCache = cacheBytes;
        }

        long actualMemoryUsage = Math.max(0L, rawMemoryUsage - cacheBytes);
        this.memoryUsage = ByteUtils.formatBytes(actualMemoryUsage);
        this.memoryLimit = ByteUtils.formatBytes(memoryLimit);

        if (memoryLimit != 0) {
            this.memoryPercent = (actualMemoryUsage * 100.0f / memoryLimit);
        } else {
            this.memoryPercent = 0.0f;
        }
    }
}
