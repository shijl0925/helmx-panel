package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerOperation {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    private String containerId;

    @ApiModelProperty(value = "操作")
    private String operation; // start stop restart kill pause unpause remove
}
