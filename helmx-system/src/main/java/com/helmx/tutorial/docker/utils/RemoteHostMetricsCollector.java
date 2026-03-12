package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.docker.entity.DockerEnv;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.FingerprintVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RemoteHostMetricsCollector {

    private static final String REMOTE_METRICS_COMMAND = """
            sh -lc 'read _ user nice system idle iowait irq softirq steal _ < /proc/stat;
            total1=$((user+nice+system+idle+iowait+irq+softirq+steal));
            idle1=$((idle+iowait));
            if [ -r /proc/self/mountinfo ]; then
              root_devno=$(awk "\\$5 == \"/\" {print \\$3; exit}" /proc/self/mountinfo);
            else
              root_devno=;
            fi;
            if [ -n "$root_devno" ] && [ -e "/sys/dev/block/$root_devno" ]; then
              root_device=$(basename "$(readlink -f "/sys/dev/block/$root_devno")");
            else
              root_source=$(df / | awk "NR==2 {print \\$1}");
              root_resolved=$(readlink -f "$root_source" 2>/dev/null || printf "%s" "$root_source");
              root_device=$(basename "$root_resolved");
            fi;
            if [ -r "/sys/class/block/$root_device/stat" ]; then
              set -- $(awk "{printf \\"%s %s\\", \\$3*512, \\$7*512}" "/sys/class/block/$root_device/stat");
              read1=${1:-0}; write1=${2:-0};
            else
              read1=0; write1=0;
            fi;
            sleep 1;
            read _ user nice system idle iowait irq softirq steal _ < /proc/stat;
            total2=$((user+nice+system+idle+iowait+irq+softirq+steal));
            idle2=$((idle+iowait));
            if [ -r "/sys/class/block/$root_device/stat" ]; then
              set -- $(awk "{printf \\"%s %s\\", \\$3*512, \\$7*512}" "/sys/class/block/$root_device/stat");
              read2=${1:-0}; write2=${2:-0};
            else
              read2=0; write2=0;
            fi;
            total=$((total2-total1));
            idle=$((idle2-idle1));
            if [ "$total" -le 0 ]; then cpu=0.00; else cpu=$(awk -v total="$total" -v idle="$idle" "BEGIN { printf \\"%.2f\\", (1 - idle / total) * 100 }"); fi;
            mem=$(awk "/MemTotal:/ {total=\\$2} /MemAvailable:/ {avail=\\$2} /MemFree:/ {free=\\$2} /Buffers:/ {buffers=\\$2} /Cached:/ {cached=\\$2} END { if (avail == 0) avail = free + buffers + cached; used=total-avail; if (total<=0) {printf \\"0 0 0.00\\"} else {printf \\"%d %d %.2f\\", used*1024, total*1024, used*100/total}}" /proc/meminfo);
            disk=$(df -B1 / | awk "NR==2 { if (\\$2<=0) printf \\"0 0 0.00\\"; else printf \\"%s %s %.2f\\", \\$3, \\$2, \\$5+0 }");
            diskio=$(awk -v read1="$read1" -v read2="$read2" -v write1="$write1" -v write2="$write2" "BEGIN { read=read2-read1; write=write2-write1; if (read < 0) read = 0; if (write < 0) write = 0; printf \\"%.2f %.2f\\", read/1024, write/1024 }");
            printf "cpu=%s\\nmem=%s\\ndisk=%s\\ndiskio=%s\\n" "$cpu" "$mem" "$disk" "$diskio"'\
            """;

    @Value("${docker.remote-metrics.ssh-timeout-seconds:10}")
    private int sshTimeoutSeconds;

    @Autowired
    private PasswordUtil passwordUtil;

    public Map<String, Object> collect(String dockerHost, DockerEnv dockerEnv) {
        Map<String, Object> metrics = defaultMetrics();
        if (!isRemoteMetricsConfigured(dockerEnv)) {
            return metrics;
        }

        String sshHost = resolveSshHost(dockerHost);
        if (sshHost == null) {
            return metrics;
        }

        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(FingerprintVerifier.getInstance(dockerEnv.getSshHostKeyFingerprint()));
            sshClient.setConnectTimeout(sshTimeoutSeconds * 1000);
            sshClient.setTimeout(sshTimeoutSeconds * 1000);
            sshClient.connect(sshHost, dockerEnv.getSshPort());
            authenticate(sshClient, dockerEnv);

            try (Session session = sshClient.startSession();
                 Session.Command command = session.exec(REMOTE_METRICS_COMMAND)) {
                command.join(sshTimeoutSeconds, TimeUnit.SECONDS);
                Integer exitStatus = command.getExitStatus();
                String output = readFully(command.getInputStream());
                String errorOutput = readFully(command.getErrorStream());
                if (exitStatus == null || exitStatus != 0) {
                    log.warn("Failed to collect remote host metrics from {}: {}", dockerHost, errorOutput);
                    return metrics;
                }
                return parseMetricsOutput(output);
            } finally {
                sshClient.disconnect();
            }
        } catch (Exception ex) {
            log.warn("Unable to collect remote host metrics for {}", dockerHost, ex);
            return metrics;
        }
    }

    Map<String, Object> parseMetricsOutput(String output) {
        Map<String, Object> metrics = defaultMetrics();
        if (output == null || output.isBlank()) {
            return metrics;
        }

        for (String line : output.split("\\R")) {
            if (line.startsWith("cpu=")) {
                metrics.put("hostCpuUsage", parseDouble(line.substring(4)));
            } else if (line.startsWith("mem=")) {
                String[] values = line.substring(4).trim().split("\\s+");
                if (values.length == 3) {
                    long used = parseLong(values[0]);
                    long total = parseLong(values[1]);
                    metrics.put("hostMemoryUsed", ByteUtils.formatBytes(used));
                    metrics.put("hostMemoryTotal", ByteUtils.formatBytes(total));
                    metrics.put("hostMemoryUsage", parseDouble(values[2]));
                }
            } else if (line.startsWith("disk=")) {
                String[] values = line.substring(5).trim().split("\\s+");
                if (values.length == 3) {
                    long used = parseLong(values[0]);
                    long total = parseLong(values[1]);
                    metrics.put("hostDiskUsed", ByteUtils.formatBytes(used));
                    metrics.put("hostDiskTotal", ByteUtils.formatBytes(total));
                    metrics.put("hostDiskUsage", parseDouble(values[2]));
                }
            } else if (line.startsWith("diskio=")) {
                String[] values = line.substring(7).trim().split("\\s+");
                if (values.length == 2) {
                    metrics.put("DiskReadTrafficNew", parseDouble(values[0]));
                    metrics.put("WriteTrafficNew", parseDouble(values[1]));
                }
            }
        }

        boolean available = ((Double) metrics.get("hostCpuUsage")) > 0
                || !"0B".equals(metrics.get("hostMemoryTotal"))
                || !"0B".equals(metrics.get("hostDiskTotal"));
        metrics.put("hostMetricsAvailable", available);
        return metrics;
    }

    private boolean isRemoteMetricsConfigured(DockerEnv dockerEnv) {
        return dockerEnv != null
                && Boolean.TRUE.equals(dockerEnv.getSshEnabled())
                && dockerEnv.getSshPort() != null
                && dockerEnv.getSshUsername() != null && !dockerEnv.getSshUsername().isBlank()
                && dockerEnv.getSshHostKeyFingerprint() != null && !dockerEnv.getSshHostKeyFingerprint().isBlank();
    }

    void authenticate(SSHClient sshClient, DockerEnv dockerEnv) throws IOException {
        String sshPassword = dockerEnv.getSshPassword();
        if (sshPassword != null && !sshPassword.isBlank()) {
            sshClient.authPassword(dockerEnv.getSshUsername(), passwordUtil.decrypt(sshPassword));
            return;
        }

        try {
            sshClient.authPublickey(dockerEnv.getSshUsername());
        } catch (UserAuthException ex) {
            throw new IOException("SSH public key authentication failed for user " + dockerEnv.getSshUsername(), ex);
        }
    }

    private String resolveSshHost(String dockerHost) {
        try {
            URI uri = URI.create(dockerHost);
            return uri.getHost();
        } catch (IllegalArgumentException ex) {
            log.debug("Unable to resolve SSH host from Docker host {}", dockerHost, ex);
            return null;
        }
    }

    private String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private double parseDouble(String value) {
        try {
            return Math.round(Double.parseDouble(value) * 100D) / 100D;
        } catch (NumberFormatException ex) {
            return 0D;
        }
    }

    private Map<String, Object> defaultMetrics() {
        Map<String, Object> hostMetrics = new HashMap<>();
        hostMetrics.put("hostMetricsAvailable", false);
        hostMetrics.put("hostCpuUsage", 0D);
        hostMetrics.put("hostMemoryUsage", 0D);
        hostMetrics.put("hostMemoryUsed", "0B");
        hostMetrics.put("hostMemoryTotal", "0B");
        hostMetrics.put("hostDiskUsage", 0D);
        hostMetrics.put("hostDiskUsed", "0B");
        hostMetrics.put("hostDiskTotal", "0B");
        hostMetrics.put("DiskReadTrafficNew", 0D);
        hostMetrics.put("WriteTrafficNew", 0D);
        return hostMetrics;
    }
}
