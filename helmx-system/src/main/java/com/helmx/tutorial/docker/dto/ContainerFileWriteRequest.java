package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerFileWriteRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    @NotBlank(message = "Container ID cannot be blank")
    private String containerId;

    @ApiModelProperty(value = "容器内文件路径（含文件名）")
    @NotBlank(message = "File path cannot be blank")
    private String filePath;

    @ApiModelProperty(value = "文件内容")
    @NotBlank(message = "File content cannot be blank")
    private String content;

    @ApiModelProperty(value = "文件编码，默认UTF-8")
    private String encoding = "UTF-8";
}
