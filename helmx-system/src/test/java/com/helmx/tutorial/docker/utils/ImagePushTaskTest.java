package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImagePushTaskTest {

    @Test
    void imageName_setAndGet() {
        ImagePushTask task = new ImagePushTask();
        task.setImageName("registry.example.com/my-app:1.0");
        assertEquals("registry.example.com/my-app:1.0", task.getImageName());
    }

    @Test
    void isCompleted_successStatus_returnsTrue() {
        ImagePushTask task = new ImagePushTask();
        task.setStatus("SUCCESS");
        assertTrue(task.isCompleted());
    }

    @Test
    void isCompleted_failedStatus_returnsTrue() {
        ImagePushTask task = new ImagePushTask();
        task.setStatus("FAILED");
        assertTrue(task.isCompleted());
    }

    @Test
    void isCompleted_pendingOrRunning_returnsFalse() {
        for (String status : new String[]{"PENDING", "RUNNING", null}) {
            ImagePushTask task = new ImagePushTask();
            task.setStatus(status);
            assertFalse(task.isCompleted(), "status=" + status + " should not be completed");
        }
    }

    @Test
    void taskId_setAndGet() {
        ImagePushTask task = new ImagePushTask();
        task.setTaskId("push-999");
        assertEquals("push-999", task.getTaskId());
    }
}
