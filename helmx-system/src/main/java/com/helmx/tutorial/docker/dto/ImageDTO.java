package com.helmx.tutorial.docker.dto;

import com.helmx.tutorial.docker.utils.ByteUtils;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import com.github.dockerjava.api.model.Image;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class ImageDTO {
    @ApiModelProperty(value = "镜像ID")
    private String id;

    @ApiModelProperty(value = "标签")
    private String[] tags;

    @ApiModelProperty(value = "镜像大小")
    private String size;

    @ApiModelProperty(value = "创建时间")
    private String createdAt;

    @ApiModelProperty(value = "是否正在使用")
    private Boolean isUsed;

    public ImageDTO(Image image) {
        this.id = image.getId().split(":")[1];
        this.tags = image.getRepoTags();
        this.size = ByteUtils.formatBytes(image.getSize());
        this.createdAt = Instant.ofEpochSecond(image.getCreated())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
