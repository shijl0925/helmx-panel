package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ExportImageRequest {
    @ApiModelProperty(value = "Docker主机地址", required = true)
    private String host;

    @ApiModelProperty(value = "镜像名称", required = true)
    private String imageName;

    @ApiModelProperty(value = "文件名称")
    private String filename;
}
