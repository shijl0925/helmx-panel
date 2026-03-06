package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemainingDockerDTOsTest {

    // ---- TemplateUpdateRequest ----

    @Test
    void templateUpdateRequest_defaultConstructor_allNull() {
        TemplateUpdateRequest req = new TemplateUpdateRequest();
        assertNull(req.getName());
        assertNull(req.getRemark());
        assertNull(req.getContent());
        assertNull(req.getType());
    }

    @Test
    void templateUpdateRequest_settersAndGetters() {
        TemplateUpdateRequest req = new TemplateUpdateRequest();
        req.setName("updated-template");
        req.setRemark("Updated description");
        req.setContent("FROM alpine:latest");
        req.setType("Dockerfile");

        assertEquals("updated-template", req.getName());
        assertEquals("Updated description", req.getRemark());
        assertEquals("FROM alpine:latest", req.getContent());
        assertEquals("Dockerfile", req.getType());
    }

    @Test
    void templateUpdateRequest_equals_sameFields() {
        TemplateUpdateRequest r1 = new TemplateUpdateRequest();
        r1.setName("t1");
        r1.setType("DockerCompose");

        TemplateUpdateRequest r2 = new TemplateUpdateRequest();
        r2.setName("t1");
        r2.setType("DockerCompose");

        assertEquals(r1, r2);
    }

    // ---- VolumeCreateRequest ----

    @Test
    void volumeCreateRequest_defaultValues() {
        VolumeCreateRequest req = new VolumeCreateRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
        assertNull(req.getDriver());
        assertNull(req.getDriverOpts());
        assertNull(req.getLabels());
    }

    @Test
    void volumeCreateRequest_settersAndGetters() {
        VolumeCreateRequest req = new VolumeCreateRequest();
        req.setHost("tcp://10.0.0.1:2375");
        req.setName("data-volume");
        req.setDriver("local");
        req.setDriverOpts(new String[]{"type=nfs", "o=addr=192.168.1.1"});
        req.setLabels(new String[]{"env=prod"});

        assertEquals("tcp://10.0.0.1:2375", req.getHost());
        assertEquals("data-volume", req.getName());
        assertEquals("local", req.getDriver());
        assertArrayEquals(new String[]{"type=nfs", "o=addr=192.168.1.1"}, req.getDriverOpts());
        assertArrayEquals(new String[]{"env=prod"}, req.getLabels());
    }

    // ---- VolumeInfoRequest ----

    @Test
    void volumeInfoRequest_defaultValues() {
        VolumeInfoRequest req = new VolumeInfoRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
    }

    @Test
    void volumeInfoRequest_settersAndGetters() {
        VolumeInfoRequest req = new VolumeInfoRequest();
        req.setHost("tcp://10.0.0.2:2375");
        req.setName("my-volume");

        assertEquals("tcp://10.0.0.2:2375", req.getHost());
        assertEquals("my-volume", req.getName());
    }

    // ---- VolumeQueryRequest ----

    @Test
    void volumeQueryRequest_defaultValues() {
        VolumeQueryRequest req = new VolumeQueryRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getName());
        assertNull(req.getSortOrder());
        assertNull(req.getPage());
        assertNull(req.getPageSize());
    }

    @Test
    void volumeQueryRequest_settersAndGetters() {
        VolumeQueryRequest req = new VolumeQueryRequest();
        req.setHost("tcp://10.0.0.3:2375");
        req.setName("vol");
        req.setSortOrder("desc");
        req.setPage(1);
        req.setPageSize(25);

        assertEquals("tcp://10.0.0.3:2375", req.getHost());
        assertEquals("vol", req.getName());
        assertEquals("desc", req.getSortOrder());
        assertEquals(1, req.getPage());
        assertEquals(25, req.getPageSize());
    }

    // ---- removeImageRequest ----

    @Test
    void removeImageRequest_defaultValues() {
        removeImageRequest req = new removeImageRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getForce());
        assertNull(req.getImageId());
    }

    @Test
    void removeImageRequest_settersAndGetters() {
        removeImageRequest req = new removeImageRequest();
        req.setHost("tcp://10.0.0.4:2375");
        req.setForce(true);
        req.setImageId("sha256:deadbeef");

        assertEquals("tcp://10.0.0.4:2375", req.getHost());
        assertTrue(req.getForce());
        assertEquals("sha256:deadbeef", req.getImageId());
    }

    @Test
    void removeImageRequest_noForce_removesNormally() {
        removeImageRequest req = new removeImageRequest();
        req.setImageId("sha256:aabbccdd");
        req.setForce(false);

        assertFalse(req.getForce());
        assertEquals("sha256:aabbccdd", req.getImageId());
    }

    // ---- removeNetworkRequest ----

    @Test
    void removeNetworkRequest_defaultValues() {
        removeNetworkRequest req = new removeNetworkRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getNames());
    }

    @Test
    void removeNetworkRequest_settersAndGetters() {
        removeNetworkRequest req = new removeNetworkRequest();
        req.setHost("tcp://10.0.0.5:2375");
        req.setNames(new String[]{"net1", "net2", "net3"});

        assertEquals("tcp://10.0.0.5:2375", req.getHost());
        assertArrayEquals(new String[]{"net1", "net2", "net3"}, req.getNames());
    }

    @Test
    void removeNetworkRequest_emptyNames() {
        removeNetworkRequest req = new removeNetworkRequest();
        req.setNames(new String[]{});
        assertEquals(0, req.getNames().length);
    }

    // ---- removeVolumeRequest ----

    @Test
    void removeVolumeRequest_defaultValues() {
        removeVolumeRequest req = new removeVolumeRequest();
        assertEquals("unix:///var/run/docker.sock", req.getHost());
        assertNull(req.getNames());
    }

    @Test
    void removeVolumeRequest_settersAndGetters() {
        removeVolumeRequest req = new removeVolumeRequest();
        req.setHost("tcp://10.0.0.6:2375");
        req.setNames(new String[]{"vol1", "vol2"});

        assertEquals("tcp://10.0.0.6:2375", req.getHost());
        assertArrayEquals(new String[]{"vol1", "vol2"}, req.getNames());
    }

    @Test
    void removeVolumeRequest_singleVolume() {
        removeVolumeRequest req = new removeVolumeRequest();
        req.setNames(new String[]{"only-vol"});

        assertEquals(1, req.getNames().length);
        assertEquals("only-vol", req.getNames()[0]);
    }
}
