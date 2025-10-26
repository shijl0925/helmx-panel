package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VolumeInfoRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "host cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "存储卷名称")
    private String name;
}
