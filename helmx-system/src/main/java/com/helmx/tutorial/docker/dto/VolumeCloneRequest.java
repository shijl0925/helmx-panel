package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VolumeCloneRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "host cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "源卷名称")
    @NotBlank(message = "sourceName cannot be blank")
    private String sourceName;

    @ApiModelProperty(value = "目标卷名称")
    @NotBlank(message = "targetName cannot be blank")
    private String targetName;

    @ApiModelProperty(value = "目标卷驱动（默认 local）")
    private String driver = "local";
}
