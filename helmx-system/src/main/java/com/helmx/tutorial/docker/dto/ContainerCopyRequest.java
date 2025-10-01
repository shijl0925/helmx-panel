package com.helmx.tutorial.docker.dto;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerCopyRequest {

    @ApiModelProperty(value = "Docker主机地址", required = true)
    private String host;

    @ApiModelProperty(value = "容器ID", required = true)
    private String containerId;

    @ApiModelProperty(value = "容器内路径", required = true)
    private String containerPath;

    @ApiModelProperty(value = "本地文件路径")
    private String localPath;
}
