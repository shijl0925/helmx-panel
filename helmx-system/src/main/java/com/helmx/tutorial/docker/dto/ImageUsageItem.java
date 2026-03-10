package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class ImageUsageItem {

    @ApiModelProperty(value = "镜像ID（短ID）")
    private String id;

    @ApiModelProperty(value = "完整镜像ID")
    private String fullId;

    @ApiModelProperty(value = "镜像标签列表")
    private List<String> repoTags;

    @ApiModelProperty(value = "镜像大小（字节）")
    private long size;

    @ApiModelProperty(value = "镜像大小（可读格式）")
    private String sizeHuman;

    @ApiModelProperty(value = "虚拟大小（含共享层，字节）")
    private long virtualSize;

    @ApiModelProperty(value = "虚拟大小（可读格式）")
    private String virtualSizeHuman;

    @ApiModelProperty(value = "是否正在被容器使用")
    private Boolean isUsed;
}
