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

    // ---- appendToLog / logQueue ----

    @Test
    void appendToLog_addsLineToStreamBuilderAndQueue() {
        ImageBuildTask task = new ImageBuildTask();
        task.appendToLog("Step 1/3\n");

        assertEquals("Step 1/3\n", task.getStream());
        assertEquals("Step 1/3\n", task.getLogQueue().poll());
        assertNull(task.getLogQueue().poll(), "Queue should be empty after polling");
    }

    @Test
    void appendToLog_multipleLines_preservesOrder() {
        ImageBuildTask task = new ImageBuildTask();
        task.appendToLog("line 1\n");
        task.appendToLog("line 2\n");
        task.appendToLog("line 3\n");

        assertEquals("line 1\n", task.getLogQueue().poll());
        assertEquals("line 2\n", task.getLogQueue().poll());
        assertEquals("line 3\n", task.getLogQueue().poll());
        assertNull(task.getLogQueue().poll());
    }

    @Test
    void appendToLog_accumulatesInStreamBuilder() {
        ImageBuildTask task = new ImageBuildTask();
        task.appendToLog("part 1 ");
        task.appendToLog("part 2");

        assertEquals("part 1 part 2", task.getStream());
    }

    @Test
    void appendToLog_doesNotClearExistingStream() {
        ImageBuildTask task = new ImageBuildTask();
        task.setStream("initial content\n");
        task.appendToLog("appended line\n");

        assertEquals("initial content\nappended line\n", task.getStream());
    }

    @Test
    void logQueue_initiallyEmpty() {
        ImageBuildTask task = new ImageBuildTask();
        assertNull(task.getLogQueue().poll());
    }
}
