package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerExecRequest {

    @ApiModelProperty(value = "Docker主机地址", required = true)
    private String host;

    @ApiModelProperty(value = "容器ID", required = true)
    private String containerId;

    @ApiModelProperty(value = "要执行的命令", required = true)
    private String[] command;

    @ApiModelProperty(value = "是否附加到标准输入")
    private Boolean attachStdin = true;

    @ApiModelProperty(value = "是否附加到标准输出")
    private Boolean attachStdout = true;

    @ApiModelProperty(value = "是否附加到标准错误")
    private Boolean attachStderr = true;

    @ApiModelProperty(value = "是否使用TTY")
    private Boolean tty = true;
}
