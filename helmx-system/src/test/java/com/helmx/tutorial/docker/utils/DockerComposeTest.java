package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DockerComposeTest {

    @Test
    void waitForProcessReturnsExitCodeWithoutDoubleWait() throws Exception {
        DockerCompose dockerCompose = new DockerCompose();
        Process process = Mockito.mock(Process.class);

        when(process.waitFor(eq(300L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(process.exitValue()).thenReturn(0);

        int exitCode = dockerCompose.waitForProcess(process, 300L, "Deployment timeout after 5 minutes");

        assertEquals(0, exitCode);
        verify(process, times(1)).waitFor(eq(300L), eq(TimeUnit.SECONDS));
        verify(process, times(1)).exitValue();
        verify(process, never()).destroyForcibly();
    }

    @Test
    void waitForProcessTimeoutDestroysProcessAndThrows() throws Exception {
        DockerCompose dockerCompose = new DockerCompose();
        Process process = Mockito.mock(Process.class);

        when(process.waitFor(eq(120L), eq(TimeUnit.SECONDS))).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> dockerCompose.waitForProcess(process, 120L, "docker-compose down command timeout during cleanup"));

        verify(process, times(1)).waitFor(eq(120L), eq(TimeUnit.SECONDS));
        verify(process, times(1)).destroyForcibly();
        verify(process, never()).exitValue();
    }
}
