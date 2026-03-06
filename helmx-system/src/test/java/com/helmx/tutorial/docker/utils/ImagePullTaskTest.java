package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ImagePullTaskTest {

    @Test
    void imageName_setAndGet() {
        ImagePullTask task = new ImagePullTask();
        task.setImageName("nginx:latest");
        assertEquals("nginx:latest", task.getImageName());
    }

    @Test
    void baseTaskFields_setAndGet() {
        ImagePullTask task = new ImagePullTask();
        task.setTaskId("pull-001");
        task.setStatus("PENDING");
        task.setMessage("Waiting to pull");

        assertEquals("pull-001", task.getTaskId());
        assertEquals("PENDING", task.getStatus());
        assertEquals("Waiting to pull", task.getMessage());
    }

    @Test
    void isCompleted_runningStatus_returnsFalse() {
        ImagePullTask task = new ImagePullTask();
        task.setStatus("RUNNING");
        assertFalse(task.isCompleted());
    }

    @Test
    void isCompleted_successStatus_returnsTrue() {
        ImagePullTask task = new ImagePullTask();
        task.setStatus("SUCCESS");
        assertTrue(task.isCompleted());
    }

    @Test
    void isCompleted_failedStatus_returnsTrue() {
        ImagePullTask task = new ImagePullTask();
        task.setStatus("FAILED");
        assertTrue(task.isCompleted());
    }

    @Test
    void startAndEndTime_setAndGet() {
        ImagePullTask task = new ImagePullTask();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(10);
        task.setStartTime(start);
        task.setEndTime(end);

        assertEquals(start, task.getStartTime());
        assertEquals(end, task.getEndTime());
    }
}
