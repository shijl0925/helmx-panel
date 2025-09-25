package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class VolumeCreateRequest {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "驱动")
    private String driver;

    @ApiModelProperty(value = "驱动参数")
    private String[] driverOpts;

    @ApiModelProperty(value = "标签")
    private String[] labels;
}
