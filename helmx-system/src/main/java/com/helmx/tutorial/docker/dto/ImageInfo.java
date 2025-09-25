package com.helmx.tutorial.docker.dto;

import com.github.dockerjava.api.model.ContainerConfig;
import io.swagger.annotations.ApiModelProperty;
import com.github.dockerjava.api.command.InspectImageResponse;

import com.helmx.tutorial.docker.utils.ByteUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Data
public class ImageInfo {
    @ApiModelProperty(value = "镜像ID")
    private String id;

    @ApiModelProperty(value = "标签")
    private List<String> repoTags;

    @ApiModelProperty(value = "镜像大小")
    private String size;

    @ApiModelProperty(value = "标签")
    private Map<String, String> labels;

    @ApiModelProperty(value = "创建时间")
    private String createdAt;

    @ApiModelProperty(value = "构建环境")
    private String build;

    @ApiModelProperty(value = "镜像配置")
    private Map<String, Object> config;

    private List<String> layers;

    private List<Map<String, String>> history;

    public ImageInfo(InspectImageResponse image) {
        this.id = Objects.requireNonNull(image.getId()).split(":")[1];
        this.repoTags = image.getRepoTags();
        this.size = image.getSize() != null ? ByteUtils.formatBytes(image.getSize()) : "";
        this.createdAt = image.getCreated();

        this.build = image.getArch() + "," + image.getOs();
        ContainerConfig containerConfig = image.getConfig();

        Map<String, Object> details = new HashMap<>();
        if (containerConfig != null) {
            this.labels = containerConfig.getLabels();

            if (containerConfig.getCmd() != null) {
                details.put("CMD", containerConfig.getCmd());
            } else {
                details.put("CMD", "");
            }
            details.put("ENTRYPOINT", containerConfig.getEntrypoint());
            details.put("ENV", containerConfig.getEnv());
            details.put("EXPOSE", containerConfig.getExposedPorts());
            details.put("VOLUMES", containerConfig.getVolumes());
        }
        this.config = details;

        this.layers = Objects.requireNonNull(image.getRootFS()).getLayers();
    }
}
