package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.docker.entity.DockerEnv;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import net.schmizz.sshj.SSHClient;

class RemoteHostMetricsCollectorTest {

    private final RemoteHostMetricsCollector collector = new RemoteHostMetricsCollector();

    @Test
    void parseMetricsOutput_mapsCpuMemoryAndDiskValues() {
        Map<String, Object> metrics = collector.parseMetricsOutput("""
                cpu=12.34
                mem=2147483648 8589934592 25.00
                disk=10737418240 21474836480 50.00
                diskio=128.50 64.25
                """);

        assertTrue((Boolean) metrics.get("hostMetricsAvailable"));
        assertEquals(12.34D, metrics.get("hostCpuUsage"));
        assertEquals(25.0D, metrics.get("hostMemoryUsage"));
        assertEquals("2.00 GB", metrics.get("hostMemoryUsed"));
        assertEquals("8.00 GB", metrics.get("hostMemoryTotal"));
        assertEquals(50.0D, metrics.get("hostDiskUsage"));
        assertEquals("10.00 GB", metrics.get("hostDiskUsed"));
        assertEquals("20.00 GB", metrics.get("hostDiskTotal"));
        assertEquals(128.5D, metrics.get("DiskReadTrafficNew"));
        assertEquals(64.25D, metrics.get("WriteTrafficNew"));
    }

    @Test
    void parseMetricsOutput_blankOutput_returnsUnavailableDefaults() {
        Map<String, Object> metrics = collector.parseMetricsOutput(" ");

        assertFalse((Boolean) metrics.get("hostMetricsAvailable"));
        assertEquals(0D, metrics.get("hostCpuUsage"));
        assertEquals("0B", metrics.get("hostMemoryTotal"));
        assertEquals("0B", metrics.get("hostDiskTotal"));
        assertEquals(0D, metrics.get("DiskReadTrafficNew"));
        assertEquals(0D, metrics.get("WriteTrafficNew"));
    }

    @Test
    void authenticate_withPassword_usesPasswordAuthentication() throws Exception {
        PasswordUtil passwordUtil = mock(PasswordUtil.class);
        ReflectionTestUtils.setField(collector, "passwordUtil", passwordUtil);
        SSHClient sshClient = mock(SSHClient.class);
        DockerEnv dockerEnv = new DockerEnv();
        dockerEnv.setSshUsername("root");
        dockerEnv.setSshPassword("encrypted");
        when(passwordUtil.decrypt("encrypted")).thenReturn("plain-secret");

        collector.authenticate(sshClient, dockerEnv);

        verify(passwordUtil).decrypt("encrypted");
        verify(sshClient).authPassword("root", "plain-secret");
        verifyNoMoreInteractions(sshClient);
    }

    @Test
    void authenticate_withoutPassword_usesPublicKeyAuthentication() throws Exception {
        ReflectionTestUtils.setField(collector, "passwordUtil", mock(PasswordUtil.class));
        SSHClient sshClient = mock(SSHClient.class);
        DockerEnv dockerEnv = new DockerEnv();
        dockerEnv.setSshUsername("root");
        dockerEnv.setSshPassword(null);

        collector.authenticate(sshClient, dockerEnv);

        verify(sshClient).authPublickey("root");
        verifyNoMoreInteractions(sshClient);
    }
}
