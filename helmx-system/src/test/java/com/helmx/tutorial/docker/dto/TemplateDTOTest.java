package com.helmx.tutorial.docker.dto;

import com.helmx.tutorial.docker.entity.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateDTOTest {

    @Test
    void constructor_mapsAllFields() {
        Template t = new Template();
        t.setId(1L);
        t.setName("nginx");
        t.setRemark("Nginx Dockerfile template");
        t.setContent("FROM nginx:latest\nEXPOSE 80");
        t.setType("Dockerfile");

        TemplateDTO dto = new TemplateDTO(t);

        assertEquals(1L, dto.getId());
        assertEquals("nginx", dto.getName());
        assertEquals("Nginx Dockerfile template", dto.getRemark());
        assertEquals("FROM nginx:latest\nEXPOSE 80", dto.getContent());
        assertEquals("Dockerfile", dto.getType());
    }

    @Test
    void constructor_defaultType_isDockerfile() {
        // Template.type defaults to "Dockerfile" per entity definition
        Template t = new Template();
        t.setId(2L);
        t.setName("base");

        TemplateDTO dto = new TemplateDTO(t);

        assertEquals("Dockerfile", dto.getType());
    }

    @Test
    void constructor_nullRemarkAndContent_mappedAsNull() {
        Template t = new Template();
        t.setId(3L);
        t.setName("empty");

        TemplateDTO dto = new TemplateDTO(t);

        assertNull(dto.getRemark());
        assertNull(dto.getContent());
    }

    @Test
    void constructor_composeType_mappedCorrectly() {
        Template t = new Template();
        t.setId(4L);
        t.setName("stack");
        t.setType("DockerCompose");
        t.setContent("version: '3'\nservices:\n  web:\n    image: nginx");

        TemplateDTO dto = new TemplateDTO(t);

        assertEquals("DockerCompose", dto.getType());
        assertNotNull(dto.getContent());
    }
}
