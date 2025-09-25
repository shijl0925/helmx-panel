package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PortHelper {
    @ApiModelProperty(value = "主机IP")
    private String hostIP;

    @ApiModelProperty(value = "主机端口")
    private String hostPort;

    @ApiModelProperty(value = "容器端口")
    private String containerPort;

    @ApiModelProperty(value = "协议")
    private String protocol;
}
