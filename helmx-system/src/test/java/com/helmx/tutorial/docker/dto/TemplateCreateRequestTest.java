package com.helmx.tutorial.docker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCreateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        TemplateCreateRequest req = new TemplateCreateRequest();
        req.setName("nginx-template");
        req.setRemark("Nginx template");
        req.setContent("FROM nginx:latest");
        req.setType("Dockerfile");

        assertEquals("nginx-template", req.getName());
        assertEquals("Nginx template", req.getRemark());
        assertEquals("FROM nginx:latest", req.getContent());
        assertEquals("Dockerfile", req.getType());
    }

    @Test
    void defaultType_isDockerfile() {
        TemplateCreateRequest req = new TemplateCreateRequest();
        assertEquals("Dockerfile", req.getType());
    }

    @Test
    void type_canBeOverridden() {
        TemplateCreateRequest req = new TemplateCreateRequest();
        req.setType("DockerCompose");
        assertEquals("DockerCompose", req.getType());
    }

    @Test
    void remarkAndContent_defaultToNull() {
        TemplateCreateRequest req = new TemplateCreateRequest();
        assertNull(req.getRemark());
        assertNull(req.getContent());
    }
}
