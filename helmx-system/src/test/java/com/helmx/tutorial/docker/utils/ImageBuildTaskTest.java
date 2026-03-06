package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ImageBuildTaskTest {

    // ---- getStream / setStream / streamBuilder ----

    @Test
    void getStream_whenStreamBuilderIsNull_returnsEmptyString() {
        ImageBuildTask task = new ImageBuildTask();
        assertEquals("", task.getStream());
    }

    @Test
    void setStream_thenGetStream_returnsSetValue() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStream("Step 1/3 : FROM ubuntu:22.04");
        assertEquals("Step 1/3 : FROM ubuntu:22.04", task.getStream());
    }

    @Test
    void setStream_replacesPreviousContent() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStream("first output");
        task.setStream("second output");
        assertEquals("second output", task.getStream());
    }

    @Test
    void setStream_emptyString_returnsEmptyString() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStream("something");
        task.setStream("");
        assertEquals("", task.getStream());
    }

    // ---- imageName field ----

    @Test
    void imageName_setAndGet() {
        ImageBuildTask task = new ImageBuildTask();
        task.setImageName("my-app:latest");
        assertEquals("my-app:latest", task.getImageName());
    }

    // ---- inherited BaseTask fields ----

    @Test
    void baseTaskFields_setAndGet() {
        ImageBuildTask task = new ImageBuildTask();
        LocalDateTime now = LocalDateTime.now();
        task.setTaskId("task-001");
        task.setStatus("RUNNING");
        task.setMessage("Building layer 2/5");
        task.setStartTime(now);

        assertEquals("task-001", task.getTaskId());
        assertEquals("RUNNING", task.getStatus());
        assertEquals("Building layer 2/5", task.getMessage());
        assertEquals(now, task.getStartTime());
    }

    @Test
    void isCompleted_pendingStatus_returnsFalse() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStatus("PENDING");
        assertFalse(task.isCompleted());
    }

    @Test
    void isCompleted_runningStatus_returnsFalse() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStatus("RUNNING");
        assertFalse(task.isCompleted());
    }

    @Test
    void isCompleted_successStatus_returnsTrue() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStatus("SUCCESS");
        assertTrue(task.isCompleted());
    }

    @Test
    void isCompleted_failedStatus_returnsTrue() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStatus("FAILED");
        assertTrue(task.isCompleted());
    }

    @Test
    void isCompleted_nullStatus_returnsFalse() {
        ImageBuildTask task = new ImageBuildTask();
        // status is null by default
        assertFalse(task.isCompleted());
    }
}
