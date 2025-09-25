package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class VolumeHelper {
    @ApiModelProperty(value = "主机目录")
    private String hostPath;

    @ApiModelProperty(value = "容器目录")
    private String containerPath;

    @ApiModelProperty(value = "模式")
    private String mode;
}
