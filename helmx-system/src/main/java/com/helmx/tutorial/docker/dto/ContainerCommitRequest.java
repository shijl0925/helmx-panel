package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerCommitRequest {

    @ApiModelProperty(value = "主机地址", required = true)
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID", required = true)
    @NotBlank(message = "Container ID cannot be blank")
    private String containerId;

    @ApiModelProperty(value = "镜像名称", required = true)
    @NotBlank(message = "Repository cannot be blank")
    private String repository;
}
