package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerHealthStatus {

    @ApiModelProperty(value = "容器ID（短ID）")
    private String containerId;

    @ApiModelProperty(value = "容器名称")
    private String containerName;

    @ApiModelProperty(value = "镜像名称")
    private String image;

    @ApiModelProperty(value = "容器运行状态")
    private String state;

    @ApiModelProperty(value = "健康检查状态（healthy/unhealthy/starting/none）")
    private String health;

    @ApiModelProperty(value = "连续失败次数")
    private Integer failingStreak;

    @ApiModelProperty(value = "最近一次健康检查时间")
    private String lastCheck;
}
