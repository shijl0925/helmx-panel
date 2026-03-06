package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageRequestsTest {

    // ---- DockerEnvUpdateRequest ----

    @Test
    void dockerEnvUpdateRequest_defaultConstructor_allNull() {
        DockerEnvUpdateRequest req = new DockerEnvUpdateRequest();
        assertNull(req.getName());
        assertNull(req.getRemark());
        assertNull(req.getHost());
        assertNull(req.getStatus());
        assertNull(req.getTlsVerify());
    }

    @Test
    void dockerEnvUpdateRequest_settersAndGetters() {
        DockerEnvUpdateRequest req = new DockerEnvUpdateRequest();
        req.setName("updated-env");
        req.setRemark("Updated remark");
        req.setHost("tcp://192.168.1.100:2376");
        req.setStatus(1);
        req.setTlsVerify(true);

        assertEquals("updated-env", req.getName());
        assertEquals("Updated remark", req.getRemark());
        assertEquals("tcp://192.168.1.100:2376", req.getHost());
        assertEquals(1, req.getStatus());
        assertTrue(req.getTlsVerify());
    }

    // ---- ExportImageRequest ----

    @Test
    void exportImageRequest_defaultConstructor_allNull() {
        ExportImageRequest req = new ExportImageRequest();
        assertNull(req.getHost());
        assertNull(req.getImageName());
        assertNull(req.getFilename());
    }

    @Test
    void exportImageRequest_settersAndGetters() {
        ExportImageRequest req = new ExportImageRequest();
        req.setHost("unix:///var/run/docker.sock");
        req.setImageName("nginx:latest");
        req.setFilename("nginx_latest.tar");

        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertEquals("nginx:latest", req.getImageName());
        assertEquals("nginx_latest.tar", req.getFilename());
    }

    // ---- ImageBuildRequest ----

    @Test
    void imageBuildRequest_defaultValues() {
        ImageBuildRequest req = new ImageBuildRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getDockerfile());
        assertNull(req.getBuildArgs());
        assertNull(req.getPull());
        assertNull(req.getNoCache());
        assertNull(req.getLabels());
        assertNull(req.getTags());
    }

    @Test
    void imageBuildRequest_settersAndGetters() {
        ImageBuildRequest req = new ImageBuildRequest();
        req.setHost("tcp://10.0.0.5:2375");
        req.setDockerfile("FROM ubuntu:22.04\nRUN apt-get update");
        req.setBuildArgs("KEY=VALUE");
        req.setPull(true);
        req.setNoCache(false);
        req.setLabels("maintainer=test");
        req.setTags(new String[]{"my-image:latest", "my-image:1.0"});

        assertEquals("tcp://10.0.0.5:2375", req.getHost());
        assertEquals("FROM ubuntu:22.04\nRUN apt-get update", req.getDockerfile());
        assertEquals("KEY=VALUE", req.getBuildArgs());
        assertTrue(req.getPull());
        assertFalse(req.getNoCache());
        assertEquals("maintainer=test", req.getLabels());
        assertArrayEquals(new String[]{"my-image:latest", "my-image:1.0"}, req.getTags());
    }

    @Test
    void imageBuildRequest_noArgConstructor_works() {
        assertDoesNotThrow(ImageBuildRequest::new);
    }

    // ---- ImageInfoRequest ----

    @Test
    void imageInfoRequest_defaultHost() {
        ImageInfoRequest req = new ImageInfoRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getImageId());
    }

    @Test
    void imageInfoRequest_settersAndGetters() {
        ImageInfoRequest req = new ImageInfoRequest();
        req.setHost("tcp://10.0.0.6:2375");
        req.setImageId("sha256:abc123def456");

        assertEquals("tcp://10.0.0.6:2375", req.getHost());
        assertEquals("sha256:abc123def456", req.getImageId());
    }

    // ---- ImagePullRequest ----

    @Test
    void imagePullRequest_defaultHost() {
        ImagePullRequest req = new ImagePullRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getImageName());
    }

    @Test
    void imagePullRequest_settersAndGetters() {
        ImagePullRequest req = new ImagePullRequest();
        req.setHost("tcp://10.0.0.7:2375");
        req.setImageName("redis:alpine");

        assertEquals("tcp://10.0.0.7:2375", req.getHost());
        assertEquals("redis:alpine", req.getImageName());
    }

    @Test
    void imagePullRequest_equals_sameFields() {
        ImagePullRequest r1 = new ImagePullRequest();
        r1.setImageName("nginx:latest");

        ImagePullRequest r2 = new ImagePullRequest();
        r2.setImageName("nginx:latest");

        assertEquals(r1, r2);
    }

    // ---- ImagePushRequest ----

    @Test
    void imagePushRequest_defaultHost() {
        ImagePushRequest req = new ImagePushRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getImageName());
    }

    @Test
    void imagePushRequest_settersAndGetters() {
        ImagePushRequest req = new ImagePushRequest();
        req.setHost("tcp://10.0.0.8:2375");
        req.setImageName("registry.example.com/my-app:1.0");

        assertEquals("tcp://10.0.0.8:2375", req.getHost());
        assertEquals("registry.example.com/my-app:1.0", req.getImageName());
    }

    // ---- ImageQueryRequest ----

    @Test
    void imageQueryRequest_defaultValues() {
        ImageQueryRequest req = new ImageQueryRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
        assertNull(req.getPage());
        assertNull(req.getPageSize());
    }

    @Test
    void imageQueryRequest_settersAndGetters() {
        ImageQueryRequest req = new ImageQueryRequest();
        req.setHost("tcp://10.0.0.9:2375");
        req.setName("nginx");
        req.setPage(2);
        req.setPageSize(50);

        assertEquals("tcp://10.0.0.9:2375", req.getHost());
        assertEquals("nginx", req.getName());
        assertEquals(2, req.getPage());
        assertEquals(50, req.getPageSize());
    }

    // ---- ImageTagRequest ----

    @Test
    void imageTagRequest_defaultHost() {
        ImageTagRequest req = new ImageTagRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getImageId());
        assertNull(req.getImageName());
    }

    @Test
    void imageTagRequest_settersAndGetters() {
        ImageTagRequest req = new ImageTagRequest();
        req.setHost("tcp://10.0.0.10:2375");
        req.setImageId("sha256:zzz999");
        req.setImageName("my-tagged-image:v2");

        assertEquals("tcp://10.0.0.10:2375", req.getHost());
        assertEquals("sha256:zzz999", req.getImageId());
        assertEquals("my-tagged-image:v2", req.getImageName());
    }

    // ---- ImageTaskStatusRequest ----

    @Test
    void imageTaskStatusRequest_defaultConstructor_taskIdNull() {
        ImageTaskStatusRequest req = new ImageTaskStatusRequest();
        assertNull(req.getTaskId());
    }

    @Test
    void imageTaskStatusRequest_settersAndGetters() {
        ImageTaskStatusRequest req = new ImageTaskStatusRequest();
        req.setTaskId("task-abc-123");
        assertEquals("task-abc-123", req.getTaskId());
    }

    @Test
    void imageTaskStatusRequest_equals_sameTaskId() {
        ImageTaskStatusRequest r1 = new ImageTaskStatusRequest();
        r1.setTaskId("task-001");

        ImageTaskStatusRequest r2 = new ImageTaskStatusRequest();
        r2.setTaskId("task-001");

        assertEquals(r1, r2);
    }
}
