package com.helmx.tutorial.docker.dto;

import com.github.dockerjava.api.model.ContainerConfig;
import io.swagger.annotations.ApiModelProperty;
import com.github.dockerjava.api.command.InspectImageResponse;

import com.helmx.tutorial.docker.utils.ByteUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class ImageInfo {
    @ApiModelProperty(value = "镜像ID")
    private String id;

    @ApiModelProperty(value = "镜像作者")
    private String author;

    @ApiModelProperty(value = "镜像注释")
    private String comment;

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

    private List<ImageHistoryItem> history;

    public ImageInfo(InspectImageResponse image) {
        this.id = extractIdentifier(image.getId());
        this.author = image.getAuthor();
        this.comment = image.getComment();
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

        this.layers = image.getRootFS() != null ? image.getRootFS().getLayers() : Collections.emptyList();
    }

    private String extractIdentifier(String rawIdentifier) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            return "";
        }
        int delimiterIndex = rawIdentifier.indexOf(':');
        if (delimiterIndex < 0 || delimiterIndex == rawIdentifier.length() - 1) {
            return rawIdentifier;
        }
        return rawIdentifier.substring(delimiterIndex + 1);
    }
}
