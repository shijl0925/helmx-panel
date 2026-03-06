package com.helmx.tutorial.docker.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateEntityTest {

    @Test
    void settersAndGetters_workCorrectly() {
        Template template = new Template();
        template.setId(1L);
        template.setName("nginx-dockerfile");
        template.setRemark("Nginx Dockerfile template");
        template.setContent("FROM nginx:latest\nEXPOSE 80");
        template.setType("Dockerfile");

        assertEquals(1L, template.getId());
        assertEquals("nginx-dockerfile", template.getName());
        assertEquals("Nginx Dockerfile template", template.getRemark());
        assertEquals("FROM nginx:latest\nEXPOSE 80", template.getContent());
        assertEquals("Dockerfile", template.getType());
    }

    @Test
    void defaultType_isDockerfile() {
        Template template = new Template();
        // default value set at declaration site
        assertEquals("Dockerfile", template.getType());
    }

    @Test
    void type_canBeChangedToCompose() {
        Template template = new Template();
        template.setType("DockerCompose");
        assertEquals("DockerCompose", template.getType());
    }

    @Test
    void remarkAndContent_canBeNull() {
        Template template = new Template();
        assertNull(template.getRemark());
        assertNull(template.getContent());
    }
}
