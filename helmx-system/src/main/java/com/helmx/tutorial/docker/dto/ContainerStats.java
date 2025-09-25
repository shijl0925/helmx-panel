package com.helmx.tutorial.docker.dto;

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
    private Long perCpuUsage;

    private String memoryUsage;
    private String memoryLimit;
    private Integer memoryCache;
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

        long systemCpuUsage = cpuStats.getLongValue("system_cpu_usage");
        this.cpuSystemUsage = TimeUtils.formatNanosecondsDetailed(systemCpuUsage);

        this.onlineCPUs = cpuStats.getInteger("online_cpus");
        if (systemCpuUsage != 0) {
            this.cpuPercent = (totalUsage * 100.0f / systemCpuUsage);
        } else {
            this.cpuPercent = 0.0f;
        }

        long memoryUsage = memoryStats.getLongValue("usage");
        this.memoryUsage = ByteUtils.formatBytes(memoryUsage);

        long memoryLimit = memoryStats.getLongValue("limit");
        this.memoryLimit = ByteUtils.formatBytes(memoryLimit);

        JSONObject memoryStatsDetail = memoryStats.getJSONObject("stats");
        if (memoryStatsDetail != null) {
            this.memoryCache = memoryStatsDetail.getInteger("cache");
        }
        if (memoryLimit != 0) {
            this.memoryPercent = (memoryUsage * 100.0f / memoryLimit);
        } else {
            this.memoryPercent = 0.0f;
        }
    }
}
