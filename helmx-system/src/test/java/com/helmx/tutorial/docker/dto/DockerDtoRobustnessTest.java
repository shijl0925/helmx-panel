package com.helmx.tutorial.docker.dto;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockerDtoRobustnessTest {

    @Test
    void imageDto_withoutColonInId_keepsOriginalIdentifier() {
        Image image = mock(Image.class);
        when(image.getId()).thenReturn("image-id");
        when(image.getRepoTags()).thenReturn(new String[]{"repo:latest"});
        when(image.getSize()).thenReturn(1024L);
        when(image.getCreated()).thenReturn(0L);

        ImageDTO dto = assertDoesNotThrow(() -> new ImageDTO(image));

        assertEquals("image-id", dto.getId());
    }

    @Test
    void imageDto_withNullId_usesEmptyIdentifier() {
        Image image = mock(Image.class);
        when(image.getId()).thenReturn(null);
        when(image.getRepoTags()).thenReturn(new String[]{"repo:latest"});
        when(image.getSize()).thenReturn(1024L);
        when(image.getCreated()).thenReturn(0L);

        ImageDTO dto = assertDoesNotThrow(() -> new ImageDTO(image));

        assertEquals("", dto.getId());
    }

    @Test
    void imageInfo_withoutRootFs_orColonInId_usesSafeDefaults() {
        InspectImageResponse image = mock(InspectImageResponse.class);
        when(image.getId()).thenReturn("image-id");
        when(image.getRepoTags()).thenReturn(Collections.singletonList("repo:latest"));
        when(image.getRootFS()).thenReturn(null);

        ImageInfo info = assertDoesNotThrow(() -> new ImageInfo(image));

        assertEquals("image-id", info.getId());
        assertTrue(info.getLayers().isEmpty());
    }

    @Test
    void imageInfo_withNullId_usesEmptyIdentifier() {
        InspectImageResponse image = mock(InspectImageResponse.class);
        when(image.getId()).thenReturn(null);
        when(image.getRootFS()).thenReturn(null);

        ImageInfo info = assertDoesNotThrow(() -> new ImageInfo(image));

        assertEquals("", info.getId());
    }

    @Test
    void containerDto_withMissingOptionalArraysAndNetworks_doesNotThrow() {
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("container-1");
        when(container.getNames()).thenReturn(new String[0]);
        when(container.getState()).thenReturn("running");
        when(container.getImageId()).thenReturn("image-id");
        when(container.getImage()).thenReturn("repo:latest");
        when(container.getCreated()).thenReturn(0L);
        when(container.getStatus()).thenReturn("Up");
        when(container.getNetworkSettings()).thenReturn(null);
        when(container.getPorts()).thenReturn(null);

        ContainerDTO dto = assertDoesNotThrow(() -> new ContainerDTO(container));

        assertEquals("", dto.getName());
        assertEquals("image-id", dto.getImageId());
        assertEquals("", dto.getIpAddress());
        assertEquals("", dto.getPorts());
        assertTrue(dto.getNetworks().isEmpty());
    }

    @Test
    void containerDto_withNullImageId_usesEmptyIdentifier() {
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("container-2");
        when(container.getNames()).thenReturn(new String[]{"/demo"});
        when(container.getState()).thenReturn("running");
        when(container.getImageId()).thenReturn(null);
        when(container.getImage()).thenReturn("repo:latest");
        when(container.getCreated()).thenReturn(0L);
        when(container.getStatus()).thenReturn("Up");
        when(container.getNetworkSettings()).thenReturn(null);
        when(container.getPorts()).thenReturn(null);

        ContainerDTO dto = assertDoesNotThrow(() -> new ContainerDTO(container));

        assertEquals("", dto.getImageId());
    }
}
