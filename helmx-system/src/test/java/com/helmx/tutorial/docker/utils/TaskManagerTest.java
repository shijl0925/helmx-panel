package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BaseTaskManager behaviour through ComposeBuildTaskManager (concrete subclass).
 * Also covers ImageBuildTaskManager, ImagePullTaskManager, ImagePushTaskManager
 * since they are identical delegates.
 */
class TaskManagerTest {

    private ComposeBuildTaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new ComposeBuildTaskManager();
    }

    private ComposeBuildTask makeTask(String id, String status) {
        ComposeBuildTask task = new ComposeBuildTask();
        task.setTaskId(id);
        task.setStatus(status);
        task.setStartTime(LocalDateTime.now());
        return task;
    }

    @Test
    void initialState_isEmpty() {
        assertEquals(0, manager.getTaskCount());
        assertFalse(manager.hasTask("any"));
    }

    @Test
    void addTask_thenGetTask_returnsTask() {
        ComposeBuildTask task = makeTask("t1", "PENDING");
        manager.addTask("t1", task);

        ComposeBuildTask retrieved = manager.getTask("t1");
        assertNotNull(retrieved);
        assertEquals("t1", retrieved.getTaskId());
        assertEquals("PENDING", retrieved.getStatus());
    }

    @Test
    void hasTask_afterAdd_returnsTrue() {
        manager.addTask("t2", makeTask("t2", "RUNNING"));
        assertTrue(manager.hasTask("t2"));
    }

    @Test
    void hasTask_missingKey_returnsFalse() {
        assertFalse(manager.hasTask("nonexistent"));
    }

    @Test
    void getTask_missingKey_returnsNull() {
        assertNull(manager.getTask("nonexistent"));
    }

    @Test
    void removeTask_removesEntry() {
        manager.addTask("t3", makeTask("t3", "SUCCESS"));
        manager.removeTask("t3");
        assertFalse(manager.hasTask("t3"));
        assertNull(manager.getTask("t3"));
    }

    @Test
    void getTaskCount_reflectsAddAndRemove() {
        manager.addTask("a", makeTask("a", "PENDING"));
        manager.addTask("b", makeTask("b", "RUNNING"));
        assertEquals(2, manager.getTaskCount());

        manager.removeTask("a");
        assertEquals(1, manager.getTaskCount());
    }

    @Test
    void addTask_overwritesExisting() {
        ComposeBuildTask first = makeTask("dup", "PENDING");
        ComposeBuildTask second = makeTask("dup", "SUCCESS");

        manager.addTask("dup", first);
        manager.addTask("dup", second);

        assertEquals("SUCCESS", manager.getTask("dup").getStatus());
        assertEquals(1, manager.getTaskCount());
    }

    @Test
    void multipleTasks_storedAndRetrievedIndependently() {
        manager.addTask("x", makeTask("x", "PENDING"));
        manager.addTask("y", makeTask("y", "FAILED"));

        assertEquals("PENDING", manager.getTask("x").getStatus());
        assertEquals("FAILED",  manager.getTask("y").getStatus());
    }

    // ---- ImageBuildTaskManager (same contract) ----

    @Test
    void imageBuildTaskManager_addAndGet() {
        ImageBuildTaskManager imgMgr = new ImageBuildTaskManager();
        ImageBuildTask task = new ImageBuildTask();
        task.setTaskId("img-1");
        task.setStatus("RUNNING");
        task.setImageName("my-app:latest");

        imgMgr.addTask("img-1", task);

        ImageBuildTask got = imgMgr.getTask("img-1");
        assertNotNull(got);
        assertEquals("my-app:latest", got.getImageName());
        assertTrue(imgMgr.hasTask("img-1"));
    }

    // ---- ImagePullTaskManager (same contract) ----

    @Test
    void imagePullTaskManager_addAndGet() {
        ImagePullTaskManager pullMgr = new ImagePullTaskManager();
        ImagePullTask task = new ImagePullTask();
        task.setTaskId("pull-1");
        task.setImageName("nginx:latest");
        task.setStatus("PENDING");

        pullMgr.addTask("pull-1", task);
        assertEquals("nginx:latest", pullMgr.getTask("pull-1").getImageName());
    }

    // ---- ImagePushTaskManager (same contract) ----

    @Test
    void imagePushTaskManager_addAndGet() {
        ImagePushTaskManager pushMgr = new ImagePushTaskManager();
        ImagePushTask task = new ImagePushTask();
        task.setTaskId("push-1");
        task.setImageName("registry/my-app:1.0");
        task.setStatus("SUCCESS");

        pushMgr.addTask("push-1", task);
        assertTrue(pushMgr.getTask("push-1").isCompleted());
    }
}
