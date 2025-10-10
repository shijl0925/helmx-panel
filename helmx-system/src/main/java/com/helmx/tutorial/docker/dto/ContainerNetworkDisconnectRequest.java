package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerNetworkDisconnectRequest {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    private String containerId;

    @ApiModelProperty(value = "网络")
    private String network;
}
