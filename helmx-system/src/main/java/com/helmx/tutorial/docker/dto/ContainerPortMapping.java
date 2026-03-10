package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerPortMapping {

    @ApiModelProperty(value = "容器ID")
    private String containerId;

    @ApiModelProperty(value = "容器名称")
    private String containerName;

    @ApiModelProperty(value = "容器状态")
    private String state;

    @ApiModelProperty(value = "镜像名称")
    private String image;

    @ApiModelProperty(value = "绑定IP（宿主机）")
    private String ip;

    @ApiModelProperty(value = "宿主机端口")
    private Integer publicPort;

    @ApiModelProperty(value = "容器端口")
    private Integer privatePort;

    @ApiModelProperty(value = "协议类型（tcp/udp）")
    private String type;
}
